package br.com.ceratodash.data

/**
 * Decide o que falar e quando. Lógica pura (sem Android), para poder ser testada.
 *
 * Regras:
 * - Um alerta é falado uma vez quando aparece (ou quando muda de nível). Alerta de atenção não repete.
 * - Alerta de perigo repete a cada 30 s enquanto continuar.
 * - Um alerta que some e volta em menos de 10 s não é falado de novo (evita tagarelar perto do limite).
 * - Dica de condução: no máximo uma a cada 8 s, e nunca logo depois de um alerta (5 s).
 * - Voz desligada nas Ajustes: não fala nada.
 */
class VoiceAnnouncer {
    companion object {
        const val REPEAT_DANGER_MS = 30_000L
        const val MIN_GAP_MS = 10_000L
        const val COACH_GAP_MS = 8_000L
        const val COACH_AFTER_ALERT_MS = 5_000L
        private const val NEVER = Long.MIN_VALUE / 2
    }

    private val active = HashSet<String>()
    private val lastSpoken = HashMap<String, Long>()
    private var lastAlert = NEVER
    private var lastCoach = NEVER

    /** Devolve as frases a falar agora (em ordem de prioridade). */
    fun onTick(now: Long, alerts: List<Alert>, coachEvent: DriveEvent?, alertsOn: Boolean, coachOn: Boolean): List<String> {
        val out = ArrayList<String>(2)
        if (alertsOn) {
            for (a in alerts) {
                val k = a.key + ":" + a.level.name
                val first = k !in active
                val last = lastSpoken[k] ?: NEVER
                val speak = if (first) now - last >= MIN_GAP_MS
                else a.level == Level.PERIGO && now - last >= REPEAT_DANGER_MS
                if (speak) {
                    out.add(a.spoken)
                    lastSpoken[k] = now
                    lastAlert = now
                }
            }
            active.clear()
            for (a in alerts) active.add(a.key + ":" + a.level.name)
        } else {
            active.clear()
        }
        if (coachOn && coachEvent != null && now - lastCoach >= COACH_GAP_MS && now - lastAlert >= COACH_AFTER_ALERT_MS) {
            out.add(coachEvent.tip)
            lastCoach = now
        }
        return out
    }
}
