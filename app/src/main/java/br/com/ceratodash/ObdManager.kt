package br.com.ceratodash

import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothSocket
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import java.io.IOException
import java.util.UUID
import kotlin.coroutines.cancellation.CancellationException

/** Mantém a conexão Bluetooth com o adaptador e publica os dados do carro. */
object ObdManager {
    private val SPP: UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB")
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    /**
     * Uma tentativa de uso do adaptador. Cada start() cria uma sessão nova; só a sessão atual
     * pode publicar estado. Assim uma conexão antiga, ainda presa em connect(), não sobrescreve
     * a nova nem o socket dela.
     */
    private class Session {
        @Volatile var socket: BluetoothSocket? = null
        @Volatile var closed = false

        fun close() {
            closed = true
            try {
                socket?.close()
            } catch (_: Exception) {
            }
        }
    }

    private var job: Job? = null
    private var session: Session? = null

    val telemetry = MutableStateFlow(Telemetry())
    val status = MutableStateFlow(ObdStatus())

    @Volatile
    var fuel: Fuel = Fuel.GASOLINA

    val isRunning: Boolean get() = job?.isActive == true

    @Synchronized
    fun start(device: BluetoothDevice) {
        stopSession()
        val s = Session()
        session = s
        job = scope.launch { runLoop(device, s) }
    }

    @Synchronized
    fun stop() {
        stopSession()
        telemetry.value = Telemetry()
        status.value = ObdStatus(ConnState.DESCONECTADO, "")
    }

    private fun stopSession() {
        session?.close()
        session = null
        job?.cancel()
        job = null
    }

    private fun publish(s: Session, st: ObdStatus) {
        if (session === s && !s.closed) status.value = st
    }

    private fun publish(s: Session, t: Telemetry) {
        if (session === s && !s.closed) telemetry.value = t
    }

    @SuppressLint("MissingPermission")
    private suspend fun runLoop(device: BluetoothDevice, s: Session) {
        val name: String = (try {
            device.name
        } catch (e: SecurityException) {
            null
        }) ?: device.address

        while (!s.closed) {
            try {
                publish(s, ObdStatus(ConnState.CONECTANDO, name))
                val sock = openSocket(device, s)
                try {
                    val elm = Elm327(sock.inputStream, sock.outputStream)
                    publish(s, ObdStatus(ConnState.INICIALIZANDO, name))
                    val supported = elm.initialize()
                    publish(s, ObdStatus(ConnState.CONECTADO, name))
                    poll(elm, supported, s)
                } finally {
                    try {
                        sock.close()
                    } catch (_: Exception) {
                    }
                    if (s.socket === sock) s.socket = null
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                if (s.closed) return
                publish(s, ObdStatus(ConnState.ERRO, e.message ?: "Falha desconhecida"))
                publish(s, Telemetry())
            }
            if (s.closed) return
            delay(3000)
        }
    }

    @SuppressLint("MissingPermission")
    private fun openSocket(device: BluetoothDevice, s: Session): BluetoothSocket {
        BluetoothAdapter.getDefaultAdapter()?.cancelDiscovery()
        val first = device.createRfcommSocketToServiceRecord(SPP)
        // Guardado antes do connect(): assim stop() consegue interromper uma conexão em andamento.
        s.socket = first
        if (s.closed) {
            first.close()
            throw IOException("Conexão cancelada")
        }
        try {
            first.connect()
            return first
        } catch (e: IOException) {
            try {
                first.close()
            } catch (_: Exception) {
            }
        }
        if (s.closed) throw IOException("Conexão cancelada")
        // Alternativa comum para adaptadores ELM327 baratos (método oculto do Android).
        try {
            val m = device.javaClass.getMethod("createRfcommSocket", Int::class.javaPrimitiveType)
            val second = m.invoke(device, 1) as BluetoothSocket
            s.socket = second
            if (s.closed) {
                second.close()
                throw IOException("Conexão cancelada")
            }
            second.connect()
            return second
        } catch (e: Exception) {
            if (s.closed) throw IOException("Conexão cancelada")
            throw IOException("Não consegui conectar ao adaptador. Ele está ligado na tomada e sem outro aparelho conectado?")
        }
    }

    private suspend fun poll(elm: Elm327, supported: Set<String>, s: Session) {
        val active = Pids.all.filter { supported.isEmpty() || it.code in supported }
        val fast = active.filter { it.code == "0C" || it.code == "0D" }
        val slow = active.filter { it.code != "0C" && it.code != "0D" }
        var t = Telemetry()
        var idx = 0
        var cycle = 0
        var cycleMisses = 0
        val pidMisses = HashMap<String, Int>()
        val adapterVoltage = "42" !in supported

        while (!s.closed) {
            var got = false
            val batch = if (slow.isEmpty()) fast else fast + slow[idx++ % slow.size]
            for (p in batch) {
                val v = elm.query(p)
                if (v != null) {
                    t = p.apply(t, v)
                    pidMisses[p.code] = 0
                    got = true
                } else {
                    // Três falhas seguidas: o valor antigo deixa de valer e o painel mostra "--".
                    val n = (pidMisses[p.code] ?: 0) + 1
                    pidMisses[p.code] = n
                    if (n >= 3) t = p.apply(t, null)
                }
            }
            cycleMisses = if (got) 0 else cycleMisses + 1
            if (cycleMisses > 20) throw IOException("A ECU parou de responder.")
            if (adapterVoltage && cycle % 25 == 0) {
                t = t.copy(voltage = elm.voltage())
            }
            cycle++
            publish(s, Consumption.apply(t, fuel))
            // Cede a vez: mantém o laço cancelável mesmo se o adaptador responder instantaneamente.
            delay(1)
        }
    }
}
