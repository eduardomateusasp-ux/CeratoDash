package br.com.ceratodash.obd

import android.annotation.SuppressLint
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothGattCallback
import android.bluetooth.BluetoothGattCharacteristic
import android.bluetooth.BluetoothGattDescriptor
import android.bluetooth.BluetoothProfile
import android.bluetooth.BluetoothSocket
import android.content.Context
import android.os.Handler
import android.os.Looper
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream
import java.util.UUID
import java.util.concurrent.ConcurrentLinkedQueue
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Semaphore
import java.util.concurrent.TimeUnit

/** Como falar com o adaptador: automático (clássico e depois BLE), só BLE ou só clássico. */
enum class Transport(val label: String) {
    AUTO("automático"),
    BLE("Bluetooth LE"),
    CLASSIC("Bluetooth clássico")
}

/**
 * Ordem das tentativas de conexão (true = BLE, false = clássico).
 * No modo automático tenta o clássico primeiro, ou o BLE primeiro se ele funcionou da última vez.
 */
fun linkOrder(t: Transport, preferBle: Boolean): List<Boolean> = when (t) {
    Transport.BLE -> listOf(true)
    Transport.CLASSIC -> listOf(false)
    Transport.AUTO -> if (preferBle) listOf(true, false) else listOf(false, true)
}

/**
 * Serviços BLE usados por adaptadores ELM327 para a porta serial. Quanto menor o número, mais
 * provável é que seja o serviço certo. Os UUIDs abaixo são os desenhos comuns de adaptadores BLE
 * baratos (FFF0/FFF1/FFF2, FFE0/FFE1, 18F0/2AF0/2AF1) e o Nordic UART (6E400001).
 * Não foi conferido com o seu adaptador.
 */
object SerialServices {
    private val known = listOf("fff0", "ffe0", "18f0", "6e400001")
    private val ignored = listOf("1800", "1801", "180a")
    private val base = Regex("^0000([0-9a-f]{4})-0000-1000-8000-00805f9b34fb$")

    /** Forma curta do UUID: "fff0" para os de 16 bits, os 8 primeiros caracteres para os demais. */
    fun short(uuid: String): String {
        val u = uuid.lowercase()
        return base.find(u)?.groupValues?.get(1) ?: u.take(8)
    }

    fun isIgnored(uuid: String): Boolean = short(uuid) in ignored

    fun rank(uuid: String): Int {
        val i = known.indexOf(short(uuid))
        return if (i >= 0) i else 100
    }
}

/** Canal de bytes até o adaptador, seja Bluetooth clássico (SPP) ou BLE. */
interface ObdLink {
    val input: InputStream
    val output: OutputStream
    fun close()
}

class ClassicLink(private val socket: BluetoothSocket) : ObdLink {
    override val input: InputStream get() = socket.inputStream
    override val output: OutputStream get() = socket.outputStream

    @SuppressLint("MissingPermission")
    fun connect() = socket.connect()

    override fun close() {
        try {
            socket.close()
        } catch (_: Exception) {
        }
    }
}

/**
 * Adaptador ELM327 BLE. Procura um serviço com uma característica de notificação e outra de
 * escrita (o desenho comum dos adaptadores BLE), dando preferência aos serviços conhecidos.
 * Conecta pelo endereço, sem exigir pareamento. Se falhar, a mensagem traz o código do erro ou a
 * lista de serviços do adaptador, para facilitar o ajuste. NÃO TESTADO com adaptador real.
 */
@SuppressLint("MissingPermission")
class BleLink(private val ctx: Context, private val device: BluetoothDevice) : ObdLink {
    private val ccc: UUID = UUID.fromString("00002902-0000-1000-8000-00805f9b34fb")
    private val main = Handler(Looper.getMainLooper())
    private val rx = ConcurrentLinkedQueue<Byte>()
    private val ready = CountDownLatch(1)
    private val writeDone = Semaphore(0)

    @Volatile private var gatt: BluetoothGatt? = null
    @Volatile private var tx: BluetoothGattCharacteristic? = null
    @Volatile private var failure: String? = null
    @Volatile private var gone = false

    private fun fail(msg: String) {
        if (failure == null) failure = msg
        gone = true
        ready.countDown()
    }

    private val callback = object : BluetoothGattCallback() {
        override fun onConnectionStateChange(g: BluetoothGatt, status: Int, newState: Int) {
            if (newState == BluetoothProfile.STATE_CONNECTED && status == BluetoothGatt.GATT_SUCCESS) {
                // Uma pequena espera antes de listar os serviços evita falhas em vários aparelhos.
                main.postDelayed({ if (!gone) g.discoverServices() }, 400)
            } else if (status != BluetoothGatt.GATT_SUCCESS) {
                fail("Falha ao conectar por BLE (código $status). O adaptador pode estar conectado a outro aparelho: desligue o Bluetooth do celular.")
            } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                fail("O adaptador BLE desconectou")
            }
        }

        override fun onServicesDiscovered(g: BluetoothGatt, status: Int) {
            if (status != BluetoothGatt.GATT_SUCCESS) return fail("Falha ao listar os serviços BLE (código $status)")
            var best: Triple<Int, BluetoothGattCharacteristic, BluetoothGattCharacteristic>? = null
            for (svc in g.services) {
                val id = svc.uuid.toString()
                if (SerialServices.isIgnored(id)) continue
                val n = svc.characteristics.firstOrNull {
                    it.properties and (BluetoothGattCharacteristic.PROPERTY_NOTIFY or BluetoothGattCharacteristic.PROPERTY_INDICATE) != 0
                }
                val w = svc.characteristics.firstOrNull {
                    it.properties and (BluetoothGattCharacteristic.PROPERTY_WRITE or BluetoothGattCharacteristic.PROPERTY_WRITE_NO_RESPONSE) != 0
                }
                if (n != null && w != null) {
                    val rank = SerialServices.rank(id)
                    if (best == null || rank < best.first) best = Triple(rank, n, w)
                }
            }
            val found = best
            if (found == null) {
                val lista = g.services.joinToString(", ") { SerialServices.short(it.uuid.toString()) }
                return fail("Este adaptador BLE não tem o serviço serial esperado (serviços: $lista)")
            }
            val rxChar = found.second
            tx = found.third
            g.setCharacteristicNotification(rxChar, true)
            val d = rxChar.getDescriptor(ccc) ?: return fail("Característica BLE sem descritor de notificação")
            val indicate = rxChar.properties and BluetoothGattCharacteristic.PROPERTY_NOTIFY == 0
            d.value = if (indicate) BluetoothGattDescriptor.ENABLE_INDICATION_VALUE
            else BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE
            if (!g.writeDescriptor(d)) fail("Não consegui ativar as notificações BLE")
        }

        override fun onDescriptorWrite(g: BluetoothGatt, d: BluetoothGattDescriptor, status: Int) {
            if (status == BluetoothGatt.GATT_SUCCESS) ready.countDown() else fail("Falha ao ativar as notificações BLE (código $status)")
        }

        @Suppress("DEPRECATION", "OVERRIDE_DEPRECATION")
        override fun onCharacteristicChanged(g: BluetoothGatt, characteristic: BluetoothGattCharacteristic) {
            characteristic.value?.forEach { rx.add(it) }
        }

        override fun onCharacteristicWrite(g: BluetoothGatt, c: BluetoothGattCharacteristic, status: Int) {
            writeDone.release()
        }
    }

    /** Conecta e prepara a comunicação. Lança IOException se algo falhar. */
    fun connect(timeoutMs: Long = 12000) {
        gatt = device.connectGatt(ctx, false, callback, BluetoothDevice.TRANSPORT_LE)
        if (!ready.await(timeoutMs, TimeUnit.MILLISECONDS)) fail("Tempo esgotado ao conectar por BLE. O adaptador está ligado e longe de outro aparelho?")
        failure?.let {
            close()
            throw IOException(it)
        }
    }

    override val input: InputStream = object : InputStream() {
        override fun available(): Int {
            if (rx.isEmpty() && gone) throw IOException(failure ?: "Conexão BLE encerrada")
            return rx.size
        }

        override fun read(): Int = rx.poll()?.toInt()?.and(0xFF) ?: -1
    }

    override val output: OutputStream = object : OutputStream() {
        override fun write(b: Int) = write(byteArrayOf(b.toByte()), 0, 1)

        @Suppress("DEPRECATION")
        override fun write(b: ByteArray, off: Int, len: Int) {
            val g = gatt
            val c = tx
            if (g == null || c == null || gone) throw IOException(failure ?: "Conexão BLE encerrada")
            val props = c.properties
            val semResposta = props and BluetoothGattCharacteristic.PROPERTY_WRITE == 0
            var pos = off
            while (pos < off + len) {
                val end = minOf(pos + 20, off + len)
                c.value = b.copyOfRange(pos, end)
                c.writeType = if (semResposta) BluetoothGattCharacteristic.WRITE_TYPE_NO_RESPONSE
                else BluetoothGattCharacteristic.WRITE_TYPE_DEFAULT
                writeDone.drainPermits()
                if (!g.writeCharacteristic(c)) throw IOException("Falha ao enviar comando por BLE")
                // Escrita sem resposta: alguns aparelhos nem avisam o fim, então não exige o aviso.
                val ok = writeDone.tryAcquire(if (semResposta) 300L else 2000L, TimeUnit.MILLISECONDS)
                if (!ok && !semResposta) throw IOException("Tempo esgotado ao enviar por BLE")
                pos = end
            }
        }
    }

    override fun close() {
        fail("Conexão cancelada")
        try {
            gatt?.disconnect()
            gatt?.close()
        } catch (_: Exception) {
        }
    }
}
