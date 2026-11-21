package br.com.ceratodash

import android.annotation.SuppressLint
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothGattCallback
import android.bluetooth.BluetoothGattCharacteristic
import android.bluetooth.BluetoothGattDescriptor
import android.bluetooth.BluetoothProfile
import android.bluetooth.BluetoothSocket
import android.content.Context
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream
import java.util.UUID
import java.util.concurrent.ConcurrentLinkedQueue
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Semaphore
import java.util.concurrent.TimeUnit

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
 * escrita (o desenho comum dos adaptadores BLE, por exemplo FFF0/FFF1/FFF2 ou FFE0/FFE1).
 * NÃO TESTADO com adaptador real. Conecta pelo endereço, sem varredura; por isso o adaptador
 * precisa estar na lista de pareados da central.
 */
@SuppressLint("MissingPermission")
class BleLink(private val ctx: Context, private val device: BluetoothDevice) : ObdLink {
    private val ccc: UUID = UUID.fromString("00002902-0000-1000-8000-00805f9b34fb")
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
                g.discoverServices()
            } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                fail("O adaptador BLE desconectou")
            }
        }

        override fun onServicesDiscovered(g: BluetoothGatt, status: Int) {
            if (status != BluetoothGatt.GATT_SUCCESS) return fail("Falha ao listar os serviços BLE")
            var rxChar: BluetoothGattCharacteristic? = null
            var txChar: BluetoothGattCharacteristic? = null
            for (svc in g.services) {
                val n = svc.characteristics.firstOrNull {
                    it.properties and (BluetoothGattCharacteristic.PROPERTY_NOTIFY or BluetoothGattCharacteristic.PROPERTY_INDICATE) != 0
                }
                val w = svc.characteristics.firstOrNull {
                    it.properties and (BluetoothGattCharacteristic.PROPERTY_WRITE or BluetoothGattCharacteristic.PROPERTY_WRITE_NO_RESPONSE) != 0
                }
                if (n != null && w != null) {
                    rxChar = n
                    txChar = w
                    break
                }
            }
            if (rxChar == null || txChar == null) return fail("Este adaptador BLE não tem o serviço serial esperado")
            tx = txChar
            g.setCharacteristicNotification(rxChar, true)
            val d = rxChar.getDescriptor(ccc) ?: return fail("Característica BLE sem descritor de notificação")
            val indicate = rxChar.properties and BluetoothGattCharacteristic.PROPERTY_NOTIFY == 0
            d.value = if (indicate) BluetoothGattDescriptor.ENABLE_INDICATION_VALUE
            else BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE
            if (!g.writeDescriptor(d)) fail("Não consegui ativar as notificações BLE")
        }

        override fun onDescriptorWrite(g: BluetoothGatt, d: BluetoothGattDescriptor, status: Int) {
            if (status == BluetoothGatt.GATT_SUCCESS) ready.countDown() else fail("Falha ao ativar as notificações BLE")
        }

        @Suppress("DEPRECATION", "OVERRIDE_DEPRECATION")
        override fun onCharacteristicChanged(g: BluetoothGatt, c: BluetoothGattCharacteristic) {
            c.value?.forEach { rx.add(it) }
        }

        override fun onCharacteristicWrite(g: BluetoothGatt, c: BluetoothGattCharacteristic, status: Int) {
            writeDone.release()
        }
    }

    /** Conecta e prepara a comunicação. Lança IOException se algo falhar. */
    fun connect(timeoutMs: Long = 10000) {
        gatt = device.connectGatt(ctx, false, callback, BluetoothDevice.TRANSPORT_LE)
        if (!ready.await(timeoutMs, TimeUnit.MILLISECONDS)) fail("Tempo esgotado ao conectar por BLE")
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
            var pos = off
            while (pos < off + len) {
                val end = minOf(pos + 20, off + len)
                c.value = b.copyOfRange(pos, end)
                c.writeType = if (props and BluetoothGattCharacteristic.PROPERTY_WRITE == 0)
                    BluetoothGattCharacteristic.WRITE_TYPE_NO_RESPONSE
                else BluetoothGattCharacteristic.WRITE_TYPE_DEFAULT
                writeDone.drainPermits()
                if (!g.writeCharacteristic(c)) throw IOException("Falha ao enviar comando por BLE")
                if (!writeDone.tryAcquire(2, TimeUnit.SECONDS)) throw IOException("Tempo esgotado ao enviar por BLE")
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
