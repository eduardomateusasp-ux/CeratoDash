package br.com.ceratodash.data

import br.com.ceratodash.obd.Telemetry
import kotlinx.coroutines.flow.MutableStateFlow

/** Uma viagem: do motor ligar ao motor desligar. Tudo calculado só com dados do OBD, sem GPS. */
data class Trip(
    val id: Long = 0,
    val startMs: Long,
    val endMs: Long,
    val distanceKm: Double = 0.0,
    val maxSpeed: Float = 0f,
    /** Combustível gasto (L) e distância (km) medidos só enquanto o carro informava o fluxo de ar. */
    val fuelL: Double = 0.0,
    val fuelDistKm: Double = 0.0
) {
    val durationMs: Long get() = endMs - startMs

    /** Velocidade média sobre o tempo total da viagem (inclui paradas). Nula se não houve deslocamento. */
    val avgSpeed: Float?
        get() = if (durationMs >= 1000 && distanceKm > 0.0) (distanceKm / (durationMs / 3_600_000.0)).toFloat() else null

    /** Consumo médio. Nulo sem fluxo de ar (MAF) ou com menos de 0,5 km medidos. */
    val kmPerLiter: Float?
        get() = if (fuelL > 0.0 && fuelDistKm >= 0.5) (fuelDistKm / fuelL).toFloat() else null
}

interface TripStore {
    fun insert(startMs: Long): Long
    fun update(trip: Trip, ended: Boolean)
}

/**
 * Detecta início e fim de viagem pelas amostras de telemetria.
 * Início: rotação >= 400 rpm por 3 s seguidos. Fim: 20 s sem motor ligado (ou sem resposta do carro);
 * o fim é gravado no último instante com o motor ligado. Distância = velocidade integrada no tempo.
 * Chame onSample em intervalos curtos e regulares (o serviço chama a cada 500 ms).
 */
class TripRecorder(
    private val store: TripStore,
    private val onChange: (Trip?) -> Unit = {}
) {
    companion object {
        const val RPM_ON = 400f
        const val START_AFTER_MS = 3_000L
        const val END_AFTER_MS = 20_000L
        const val MAX_DT_MS = 2_000L
        const val SAVE_EVERY_MS = 30_000L
    }

    private var current: Trip? = null
    private var candidateSince = 0L
    private var lastRunning = 0L
    private var lastSample = 0L
    private var lastSave = 0L

    @Synchronized
    fun onSample(now: Long, t: Telemetry) {
        val running = (t.rpm ?: 0f) >= RPM_ON
        val c = current
        if (c == null) {
            if (!running) {
                candidateSince = 0L
            } else {
                if (candidateSince == 0L) candidateSince = now
                if (now - candidateSince >= START_AFTER_MS) begin(candidateSince, now)
            }
            lastSample = now
            return
        }
        if (running) {
            lastRunning = now
            val dt = minOf(now - lastSample, MAX_DT_MS)
            val speed = t.speed ?: 0f
            val km = speed * dt / 3_600_000.0
            val lph = t.litersPerHour
            current = c.copy(
                endMs = now,
                distanceKm = c.distanceKm + km,
                maxSpeed = maxOf(c.maxSpeed, speed),
                fuelL = if (lph != null) c.fuelL + lph * dt / 3_600_000.0 else c.fuelL,
                fuelDistKm = if (lph != null) c.fuelDistKm + km else c.fuelDistKm
            )
            if (now - lastSave >= SAVE_EVERY_MS) save(false, now)
            onChange(current)
        } else if (now - lastRunning >= END_AFTER_MS) {
            end(now)
        }
        lastSample = now
    }

    /** Fecha a viagem em andamento (serviço sendo encerrado). */
    @Synchronized
    fun finish(now: Long) {
        if (current != null) end(now)
    }

    private fun begin(start: Long, now: Long) {
        val id = store.insert(start)
        current = Trip(id = id, startMs = start, endMs = now)
        candidateSince = 0L
        lastRunning = now
        lastSave = now
        onChange(current)
    }

    private fun end(now: Long) {
        val c = current ?: return
        current = c.copy(endMs = maxOf(lastRunning, c.startMs))
        save(true, now)
        current = null
        onChange(null)
    }

    private fun save(ended: Boolean, now: Long) {
        current?.let { store.update(it, ended) }
        lastSave = now
    }
}

/** Estado ao vivo da viagem para a tela. Só existe no aparelho. */
object TripLive {
    val current = MutableStateFlow<Trip?>(null)

    /** Aumenta cada vez que uma viagem termina, para a lista recarregar. */
    val version = MutableStateFlow(0)
}
