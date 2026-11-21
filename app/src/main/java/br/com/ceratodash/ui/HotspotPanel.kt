package br.com.ceratodash.ui

import android.content.Context
import android.view.Gravity
import android.widget.LinearLayout
import android.widget.TextView
import br.com.ceratodash.data.AlertLimits
import br.com.ceratodash.data.Hotspot
import br.com.ceratodash.data.Hotspots
import br.com.ceratodash.obd.Telemetry

/** Painel que abre ao tocar num ponto do carro (motor, bateria, escapamento) e mostra os dados ao vivo. */
class HotspotPanel(context: Context) : LinearLayout(context) {
    private val title = context.label("", 24f, Theme.INFO, true)
    private val rows = LinearLayout(context).apply { orientation = VERTICAL }
    private var current: Hotspot? = null
    private val values = ArrayList<TextView>()

    init {
        orientation = VERTICAL
        background = context.rounded(Theme.SURFACE, 16, Theme.INFO, 2)
        setPadding(context.dp(24), context.dp(16), context.dp(24), context.dp(16))
        addView(title)
        addView(rows, LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT).apply { topMargin = context.dp(8) })
        val close = context.bigButton("Fechar") { hide() }
        close.layoutParams = LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT).apply {
            topMargin = context.dp(12)
            gravity = Gravity.END
        }
        addView(close)
        visibility = GONE
    }

    fun show(h: Hotspot, t: Telemetry, lim: AlertLimits) {
        current = h
        title.text = h.title
        rows.removeAllViews()
        values.clear()
        for ((label, _) in Hotspots.lines(h, t, lim)) {
            val row = LinearLayout(context).apply { orientation = HORIZONTAL }
            row.addView(context.label(label, Theme.TEXT_SIZE, Theme.MUTED), LayoutParams(0, LayoutParams.WRAP_CONTENT, 1f))
            val v = context.number("--", 28f, Theme.TEXT)
            values.add(v)
            row.addView(v)
            rows.addView(row)
        }
        visibility = VISIBLE
        update(t, lim)
    }

    /** Atualiza os valores quando chega dado novo e o painel está aberto. */
    fun update(t: Telemetry, lim: AlertLimits) {
        val h = current ?: return
        if (visibility != VISIBLE) return
        val lines = Hotspots.lines(h, t, lim)
        for ((i, line) in lines.withIndex()) if (i < values.size) values[i].text = line.second
    }

    fun hide() {
        visibility = GONE
        current = null
    }

    val isOpen: Boolean get() = visibility == VISIBLE
}
