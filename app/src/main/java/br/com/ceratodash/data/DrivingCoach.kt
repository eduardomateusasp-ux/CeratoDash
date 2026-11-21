package br.com.ceratodash.data

import br.com.ceratodash.obd.Telemetry
import kotlinx.coroutines.flow.MutableStateFlow
import kotlin.math.abs
import kotlin.math.roundToInt

/**
 * Limites de cada estilo de condução.
 *
 * IMPORTANTE: não existe especificação da Kia para "condução ideal". Estes números são valores
 * gerais de direção econômica escolhidos como ponto de partida, e precisam ser calibrados com o uso
 * real. O app só mede o que o OBD informa (sem GPS), então o limite de velocidade é o que o motorista
 * configura, não o da via.
 */
data class Profile(
    /** Aceleração brusca: aumento de velocidade acima disto, em km/h por segundo. */
    val accelHard: Float,
    /** Freada brusca: redução de velocidade acima disto, em km/h por segundo. */
    val brakeHard: Float,
    /** Pedal brusco: abertura da borboleta acima disto, em % por segundo. */
    val throttleRate: Float,
    /** Rotação alta (rpm), por mais de 2 s. */
    val rpmHigh: Int,
    /** Rotação máxima aceitável com o motor ainda frio (água abaixo de 60 °C). */
    val coldRpm: Int,
    /** Tempo parado com o motor ligado antes de avisar (s). */
    val idleSec: Int
)

enum class DrivingStyle(val label: String, val profile: Profile) {
    ECONOMICA("Econômica", Profile(8f, 10f, 40f, 3000, 2500, 60)),
    EQUILIBRADA("Equilibrada", Profile(10f, 12f, 60f, 4000, 3000, 120)),
    ESPORTIVA("Esportiva", Profile(14f, 16f, 90f, 5500, 3500, 180))
}

enum class DriveEvent(val title: String, val tip: String, val penalty: Int, val minMs: Long) {
    ACELERACAO("Aceleração brusca", "Acelerou forte: alivie o pedal", 3, 1000),
    FREADA("Freada brusca", "Freada forte: antecipe e frei mais cedo", 3, 1000),
    PEDAL("Pedal brusco", "Pedal muito rápido: pise mais devagar", 2, 500),
    ROTACAO("Rotação alta", "Rotação alta: alivie o pedal", 2, 2000),
    MOTOR_FRIO("Motor frio forçado", "Motor ainda frio: evite rotação alta", 3, 2000),
    PARADO("Parado com motor ligado", "Parado há muito tempo: considere desligar", 1, 0),
    VELOCIDADE("Velocidade acima do limite", "Acima do seu limite de velocidade", 2, 3000),
    ARRASTANDO("Motor arrastando", "Motor sob esforço em rotação baixa: alivie o pedal", 1, 3000);

    /** O que seria o ideal, com os números do estilo escolhido. */
    fun ideal(p: Profile, speedMax: Int): String = when (this) {
        ACELERACAO -> "Aumentar a velocidade em menos de ${p.accelHard.toInt()} km/h por segundo"
        FREADA -> "Reduzir a velocidade em menos de ${p.brakeHard.toInt()} km/h por segundo"
        PEDAL -> "Abrir o pedal em menos de ${p.throttleRate.toInt()}% por segundo"
        ROTACAO -> "Manter a rotação abaixo de ${p.rpmHigh} rpm"
        MOTOR_FRIO -> "Manter a rotação abaixo de ${p.coldRpm} rpm até a água passar de 60 °C"
        PARADO -> "Ficar parado com o motor ligado por menos de ${p.idleSec} s"
        VELOCIDADE -> "Manter a velocidade abaixo de $speedMax km/h (limite escolhido por você)"
        ARRASTANDO -> "Evitar mais de 75% de carga abaixo de 1500 rpm"
    }
}

class DriveEntry(val timeMs: Long, val event: DriveEvent)

/** Estado do treinador para a interface. É uma data class para a tela só atualizar quando mudar. */
data class CoachSnapshot(
    val running: Boolean = false,
    val score: Int = 100,
    val counts: List<Int> = List(DriveEvent.values().size) { 0 },
    val minutes: Int = 0,
    val recent: List<Pair<Long, DriveEvent>> = emptyList(),
    val tip: DriveEvent? = null
) {
    val events: Int get() = counts.sum()
}

/**
 * Observa a condução a cada amostra (a cada 500 ms) e conta "eventos": cada situação ruim que dura
 * o tempo mínimo conta uma vez por episódio. Só usa dados que o carro informou; valor ausente nunca
 * gera evento. Pontuação: 100 menos os pontos de penalidade por minuto (mínimo de 5 min), vezes 40.
 * É uma escala do próprio app, sem padrão externo.
 */
class DrivingCoach(
    @Volatile var profile: Profile = DrivingStyle.EQUILIBRADA.profile,
    @Volatile var speedMax: Int = 110
) {
    private class Sample(val t: Long, val speed: Float?, val throttle: Float?)

    private val n = DriveEvent.values().size
    private val ring = ArrayDeque<Sample>()
    private val since = LongArray(n) { -1L }
    private val falseSince = LongArray(n) { -1L }
    private val active = BooleanArray(n)
    private val counts = IntArray(n)
    private val log = ArrayList<DriveEntry>()
    private var penalty = 0
    private var runMs = 0L
    private var lastSample = -1L
    private var tipEvent: DriveEvent? = null
    private var tipUntil = 0L
    private var isRunning = false

    companion object {
        const val RPM_ON = 400f
        const val TIP_MS = 5000L
        private const val MAX_LOG = 12
    }

    @Synchronized
    fun reset() {
        ring.clear()
        since.fill(-1L)
        falseSince.fill(-1L)
        active.fill(false)
        counts.fill(0)
        log.clear()
        penalty = 0
        runMs = 0L
        lastSample = -1L
        tipEvent = null
        tipUntil = 0L
    }

    /** Processa uma amostra. Devolve o evento que acabou de disparar, se algum. */
    @Synchronized
    fun onSample(now: Long, t: Telemetry): DriveEvent? {
        val p = profile
        val rpm = t.rpm
        isRunning = rpm != null && rpm >= RPM_ON
        if (!isRunning) {
            ring.clear()
            since.fill(-1L)
            falseSince.fill(-1L)
            active.fill(false)
            lastSample = -1L
            return null
        }
        if (lastSample >= 0) runMs += minOf(now - lastSample, 2000L)
        lastSample = now

        ring.addLast(Sample(now, t.speed, t.throttle))
        while (ring.isNotEmpty() && ring.first().t < now - 4000L) ring.removeFirst()

        val accel = rate(now, 1000L, 1500L, t.speed) { it.speed }
        val thr = rate(now, 700L, 1000L, t.throttle) { it.throttle }
        val speed = t.speed
        val coolant = t.coolant
        val load = t.load

        val cond = BooleanArray(n)
        cond[DriveEvent.ACELERACAO.ordinal] = accel != null && accel > p.accelHard
        cond[DriveEvent.FREADA.ordinal] = accel != null && accel < -p.brakeHard
        cond[DriveEvent.PEDAL.ordinal] = thr != null && thr > p.throttleRate
        cond[DriveEvent.ROTACAO.ordinal] = rpm!! > p.rpmHigh
        cond[DriveEvent.MOTOR_FRIO.ordinal] = coolant != null && coolant < 60f && rpm > p.coldRpm
        cond[DriveEvent.PARADO.ordinal] = speed != null && speed == 0f
        cond[DriveEvent.VELOCIDADE.ordinal] = speed != null && speed > speedMax
        cond[DriveEvent.ARRASTANDO.ordinal] = load != null && speed != null && speed > 5f && load > 75f && rpm < 1500f

        var fired: DriveEvent? = null
        for (ev in DriveEvent.values()) {
            val i = ev.ordinal
            if (cond[i]) {
                falseSince[i] = -1L
                if (since[i] < 0) since[i] = now
                val min = if (ev == DriveEvent.PARADO) p.idleSec * 1000L else ev.minMs
                if (!active[i] && now - since[i] >= min) {
                    active[i] = true
                    counts[i]++
                    penalty += ev.penalty
                    log.add(DriveEntry(now, ev))
                    if (log.size > MAX_LOG) log.removeAt(0)
                    tipEvent = ev
                    tipUntil = now + TIP_MS
                    fired = ev
                }
            } else if (since[i] >= 0) {
                // Histerese de 1 s: o episódio só acaba depois de 1 s sem a condição.
                if (falseSince[i] < 0) falseSince[i] = now
                if (now - falseSince[i] >= 1000L) {
                    since[i] = -1L
                    falseSince[i] = -1L
                    active[i] = false
                }
            }
        }
        return fired
    }

    /** Variação por segundo entre o valor atual e uma amostra de cerca de `target` ms atrás. */
    private fun rate(now: Long, minAge: Long, target: Long, current: Float?, pick: (Sample) -> Float?): Float? {
        if (current == null) return null
        var best: Sample? = null
        for (s in ring) {
            val age = now - s.t
            if (age < minAge || pick(s) == null) continue
            if (best == null || abs(age - target) < abs(now - best.t - target)) best = s
        }
        val b = best ?: return null
        val old = pick(b) ?: return null
        return (current - old) / ((now - b.t) / 1000f)
    }

    private fun scoreNow(): Int {
        val minutes = maxOf(5.0, runMs / 60000.0)
        return maxOf(0, (100 - penalty / minutes * 40).roundToInt())
    }

    @Synchronized
    fun snapshot(now: Long): CoachSnapshot = CoachSnapshot(
        running = isRunning,
        score = scoreNow(),
        counts = counts.toList(),
        minutes = (runMs / 60000L).toInt(),
        recent = log.map { it.timeMs to it.event }.reversed(),
        tip = if (now < tipUntil) tipEvent else null
    )
}

/** Treinador único do app, alimentado pelo serviço. */
object LiveCoach {
    val coach = DrivingCoach()
    val snapshot = MutableStateFlow(CoachSnapshot())

    /** Aplica o estilo e o limite de velocidade guardados nas preferências. */
    fun applySettings(style: DrivingStyle, speedMax: Int) {
        coach.profile = style.profile
        coach.speedMax = speedMax
    }
}
