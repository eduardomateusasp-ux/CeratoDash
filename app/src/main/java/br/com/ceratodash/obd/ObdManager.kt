package br.com.ceratodash.obd

import br.com.ceratodash.data.*
import br.com.ceratodash.obd.*
import br.com.ceratodash.service.*
import br.com.ceratodash.ui.*

import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.content.Context
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeout
import java.io.IOException
import java.util.UUID
import kotlin.coroutines.cancellation.CancellationException

/**
 * Mantém a conexão com o adaptador (Bluetooth clássico, com BLE como alternativa) e publica
 * os dados do carro. Quem o mantém vivo é o ObdService; a interface só observa os fluxos.
 */
object ObdManager {
    private val SPP: UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB")
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    /**
     * Uma tentativa de uso do adaptador. Cada start() cria uma sessão nova; só a sessão atual
     * pode publicar estado. Assim uma conexão antiga, ainda presa em connect(), não sobrescreve
     * a nova.
     */
    private class Session {
        @Volatile var link: ObdLink? = null
        @Volatile var closed = false

        fun close() {
            closed = true
            link?.close()
        }
    }

    private var job: Job? = null
    private var session: Session? = null

    val telemetry = MutableStateFlow(Telemetry())
    val status = MutableStateFlow(ObdStatus())

    @Volatile
    var fuel: Fuel = Fuel.GASOLINA

    val isRunning: Boolean get() = job?.isActive == true

    // Comandos pedidos pela interface (diagnóstico). Quem fala com o adaptador é só o laço de
    // leitura, que os executa entre um ciclo e outro, para as duas conversas não se misturarem.
    private class Task(val run: (Elm327) -> Any?) {
        val result = CompletableDeferred<Any?>()
        @Volatile var cancelled = false
    }

    private val tasks = Channel<Task>(Channel.UNLIMITED)

    @Volatile
    private var ready = false

    @Suppress("UNCHECKED_CAST")
    private suspend fun <T> withElm(block: (Elm327) -> T): T {
        if (!ready) throw IOException("Sem conexão com o carro. Aguarde conectar.")
        val task = Task(block)
        tasks.send(task)
        try {
            return withTimeout(25000) { task.result.await() } as T
        } catch (e: TimeoutCancellationException) {
            throw IOException("O carro demorou demais para responder.")
        } finally {
            // Se a espera acabou antes da execução, o comando não pode rodar depois (ex.: limpar códigos).
            if (!task.result.isCompleted) task.cancelled = true
        }
    }

    private fun drainTasks(elm: Elm327?, error: String?) {
        while (true) {
            val task = tasks.tryReceive().getOrNull() ?: break
            if (task.cancelled) continue
            if (elm == null) {
                task.result.completeExceptionally(IOException(error ?: "Sem conexão com o carro."))
                continue
            }
            try {
                task.result.complete(task.run(elm))
            } catch (e: IOException) {
                task.result.completeExceptionally(e)
                throw e
            } catch (e: Exception) {
                task.result.completeExceptionally(e)
            }
        }
    }

    /** Lê a luz de injeção e os códigos de falha armazenados. */
    suspend fun readDiagnostics(): DiagResult = withElm { elm ->
        val mil = elm.milStatus()
        DiagResult(mil?.first, mil?.second, elm.readDtcs())
    }

    /** Apaga os códigos de falha (modo 04). Chame só depois da confirmação do usuário. */
    suspend fun clearDiagnostics() {
        withElm { it.clearDtcs() }
    }

    @Synchronized
    fun start(context: Context, address: String) {
        stopSession()
        val s = Session()
        session = s
        val app = context.applicationContext
        job = scope.launch { runLoop(app, address, s) }
    }

    @Synchronized
    fun stop() {
        stopSession()
        ready = false
        drainTasks(null, "Desconectado.")
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
    private suspend fun runLoop(app: Context, address: String, s: Session) {
        var preferBle = false
        while (!s.closed) {
            try {
                val adapter = BluetoothAdapter.getDefaultAdapter()
                if (adapter == null) {
                    publish(s, ObdStatus(ConnState.ERRO, "Esta central não tem Bluetooth"))
                } else if (!adapter.isEnabled) {
                    publish(s, ObdStatus(ConnState.ERRO, "Bluetooth desligado. Ligue o Bluetooth da central."))
                } else {
                    val device = adapter.getRemoteDevice(address)
                    val name: String = (try {
                        device.name
                    } catch (e: SecurityException) {
                        null
                    }) ?: address
                    publish(s, ObdStatus(ConnState.CONECTANDO, name))
                    val (link, ble) = openLink(app, device, s, preferBle, Prefs.transport(app))
                    preferBle = ble
                    try {
                        val elm = Elm327(link.input, link.output)
                        publish(s, ObdStatus(ConnState.INICIALIZANDO, name))
                        val supported = elm.initialize()
                        publish(s, ObdStatus(ConnState.CONECTADO, name))
                        ready = true
                        poll(elm, supported, s)
                    } finally {
                        ready = false
                        drainTasks(null, "A conexão com o carro caiu.")
                        link.close()
                        if (s.link === link) s.link = null
                    }
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

    /** Tenta o Bluetooth clássico e, se falhar, o BLE (na ordem inversa se o BLE funcionou antes). */
    @SuppressLint("MissingPermission")
    private fun openLink(
        app: Context, device: BluetoothDevice, s: Session, preferBle: Boolean, transport: Transport
    ): Pair<ObdLink, Boolean> {
        val order = linkOrder(transport, preferBle)
        val type = device.type
        var last: IOException? = null
        for (ble in order) {
            if (s.closed) throw IOException("Conexão cancelada")
            // No modo automático, pula o tipo que o Android já sabe que o aparelho não suporta.
            if (transport == Transport.AUTO) {
                if (ble && type == BluetoothDevice.DEVICE_TYPE_CLASSIC) continue
                if (!ble && type == BluetoothDevice.DEVICE_TYPE_LE) continue
            }
            try {
                return (if (ble) openBle(app, device, s) else openClassic(device, s)) to ble
            } catch (e: IOException) {
                last = e
                if (s.closed) throw IOException("Conexão cancelada")
            }
        }
        // Com um modo só escolhido, mostra o motivo real (por exemplo, o código do erro BLE).
        if (transport != Transport.AUTO && last?.message != null) throw IOException(last.message)
        throw IOException("Não consegui conectar ao adaptador. Ele está ligado na tomada e sem outro aparelho conectado?")
    }

    @SuppressLint("MissingPermission")
    private fun openBle(app: Context, device: BluetoothDevice, s: Session): ObdLink {
        val link = BleLink(app, device)
        s.link = link
        link.connect()
        return link
    }

    @SuppressLint("MissingPermission")
    private fun openClassic(device: BluetoothDevice, s: Session): ObdLink {
        BluetoothAdapter.getDefaultAdapter()?.cancelDiscovery()
        val first = ClassicLink(device.createRfcommSocketToServiceRecord(SPP))
        // Guardado antes do connect(): assim stop() consegue interromper uma conexão em andamento.
        s.link = first
        try {
            if (s.closed) throw IOException("Conexão cancelada")
            first.connect()
            return first
        } catch (e: IOException) {
            first.close()
        }
        if (s.closed) throw IOException("Conexão cancelada")
        // Alternativa comum para adaptadores ELM327 baratos (método oculto do Android).
        try {
            val m = device.javaClass.getMethod("createRfcommSocket", Int::class.javaPrimitiveType)
            val second = ClassicLink(m.invoke(device, 1) as android.bluetooth.BluetoothSocket)
            s.link = second
            if (s.closed) {
                second.close()
                throw IOException("Conexão cancelada")
            }
            second.connect()
            return second
        } catch (e: Exception) {
            throw IOException("Falha no Bluetooth clássico")
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

        var lastCycleMs = 0L
        while (!s.closed) {
            val cycleStart = System.currentTimeMillis()
            drainTasks(elm, null)
            var got = false
            val perCycle = if (lastCycleMs > 450) 1 else 2
            val batch = if (slow.isEmpty()) fast else fast + List(minOf(perCycle, slow.size)) { slow[idx++ % slow.size] }
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
            if (cycleMisses > 20) throw IOException(elm.lastProblem ?: "A ECU parou de responder.")
            if (adapterVoltage && cycle % 25 == 0) {
                t = t.copy(voltage = elm.voltage())
            }
            cycle++
            publish(s, Consumption.apply(t, fuel))
            lastCycleMs = System.currentTimeMillis() - cycleStart
            // Cede a vez: mantém o laço cancelável mesmo se o adaptador responder instantaneamente.
            delay(1)
        }
    }
}
