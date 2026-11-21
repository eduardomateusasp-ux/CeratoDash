package br.com.ceratodash.ui

import android.content.Context
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import br.com.ceratodash.data.CoachSnapshot
import br.com.ceratodash.data.DriveEvent
import br.com.ceratodash.data.LiveCoach
import br.com.ceratodash.data.Prefs

/**
 * Condução: nota e eventos da viagem atual, o que seria o ideal em cada item e os últimos eventos.
 * Os limites são valores gerais de direção econômica (estilo escolhido em Ajustes), não uma
 * especificação da Kia.
 */
class CoachScreen(context: Context) : ScrollView(context) {
    private val box = LinearLayout(context).apply { orientation = LinearLayout.VERTICAL }
    private val score = context.number("--", Theme.VALUE_MAIN, Theme.TEXT)
    private val scoreInfo = context.label("", Theme.TEXT_SIZE, Theme.MUTED)
    private val now = context.label("", 20f, Theme.TEXT, true)
    private val eventsBox = LinearLayout(context).apply { orientation = LinearLayout.VERTICAL }
    private val recentBox = LinearLayout(context).apply { orientation = LinearLayout.VERTICAL }
    private val counts = ArrayList<TextView>()
    private var built = false

    init {
        setPadding(context.dp(24), context.dp(16), context.dp(24), context.dp(16))
        clipToPadding = false
        addView(box)
        box.addView(context.label("Condução", 24f, Theme.TEXT, true))

        val top = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = android.view.Gravity.CENTER_VERTICAL
            background = context.rounded(Theme.SURFACE)
            setPadding(context.dp(24), context.dp(12), context.dp(24), context.dp(12))
        }
        top.addView(score, LinearLayout.LayoutParams(context.dp(180), LinearLayout.LayoutParams.WRAP_CONTENT))
        val info = LinearLayout(context).apply { orientation = LinearLayout.VERTICAL }
        info.addView(context.label("Nota da viagem (0 a 100)", Theme.TEXT_SIZE, Theme.TEXT, true))
        info.addView(scoreInfo)
        info.addView(now)
        top.addView(info, LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f))
        box.addView(top, LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply { topMargin = context.dp(8) })

        box.addView(context.label("O que o app confere", Theme.TEXT_SIZE, Theme.INFO, true), LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply { topMargin = context.dp(16) })
        box.addView(eventsBox)
        box.addView(context.label("Últimos eventos", Theme.TEXT_SIZE, Theme.INFO, true), LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply { topMargin = context.dp(16) })
        box.addView(recentBox)
        box.addView(
            context.label(
                "Os limites são valores gerais de direção econômica, ajustáveis pelo estilo em Ajustes. Não são especificação da Kia. " +
                    "O app só mede o que o OBD informa, sem GPS: o limite de velocidade é o que você configurou, não o da via. " +
                    "Não mexa na tela enquanto dirige.",
                Theme.LABEL, Theme.MUTED
            ),
            LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply { topMargin = context.dp(16) }
        )
        render(CoachSnapshot())
    }

    private fun build() {
        val p = Prefs.coachStyle(context)
        val speed = Prefs.speedMax(context)
        eventsBox.removeAllViews()
        counts.clear()
        for (ev in DriveEvent.values()) {
            val row = LinearLayout(context).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = android.view.Gravity.CENTER_VERTICAL
                background = context.rounded(Theme.SURFACE)
                setPadding(context.dp(16), context.dp(8), context.dp(16), context.dp(8))
            }
            val text = LinearLayout(context).apply { orientation = LinearLayout.VERTICAL }
            text.addView(context.label(ev.title, Theme.TEXT_SIZE, Theme.TEXT, true))
            text.addView(context.label("Ideal: " + ev.ideal(p.profile, speed).replaceFirstChar { it.lowercase() }, Theme.LABEL, Theme.MUTED))
            row.addView(text, LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f))
            val c = context.number("0", 32f, Theme.TEXT)
            counts.add(c)
            row.addView(c)
            eventsBox.addView(row, LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply { topMargin = context.dp(6) })
        }
        built = true
    }

    /** Refaz os textos de "ideal" quando o estilo ou o limite de velocidade mudam. */
    fun rebuild() {
        built = false
        render(LiveCoach.snapshot.value)
    }

    fun render(s: CoachSnapshot) {
        if (!built) build()
        if (!s.running && s.events == 0) {
            score.text = "--"
            score.setTextColor(Theme.MUTED)
            scoreInfo.text = "Estilo: ${Prefs.coachStyle(context).label}"
            now.text = "Aguardando o motor ligar."
            now.setTextColor(Theme.MUTED)
        } else {
            score.text = s.score.toString()
            score.setTextColor(
                when {
                    s.score >= 80 -> Theme.OK
                    s.score >= 60 -> Theme.WARN
                    else -> Theme.DANGER
                }
            )
            scoreInfo.text = "Estilo: ${Prefs.coachStyle(context).label}    ${s.events} " + (if (s.events == 1) "evento" else "eventos") + " em ${s.minutes} min"
            val tip = s.tip
            now.text = tip?.tip ?: if (s.running) "Condução dentro do ideal agora." else "Motor desligado."
            now.setTextColor(if (tip != null) Theme.WARN else Theme.OK)
        }
        for ((i, c) in counts.withIndex()) {
            val n = s.counts.getOrElse(i) { 0 }
            c.text = n.toString()
            c.setTextColor(if (n == 0) Theme.OK else Theme.WARN)
        }
        recentBox.removeAllViews()
        if (s.recent.isEmpty()) {
            recentBox.addView(context.label("Nenhum evento nesta viagem.", Theme.TEXT_SIZE, Theme.MUTED))
        } else {
            val t = System.currentTimeMillis()
            for ((at, ev) in s.recent.take(8)) {
                val ago = ((t - at) / 1000L).coerceAtLeast(0)
                val whenText = if (ago < 60) "há $ago s" else "há ${ago / 60} min"
                recentBox.addView(context.label("${ev.title}    $whenText", Theme.TEXT_SIZE, Theme.TEXT))
            }
        }
    }
}
