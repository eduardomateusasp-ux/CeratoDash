package br.com.ceratodash.ui

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import android.view.View
import android.widget.HorizontalScrollView
import android.widget.LinearLayout
import android.widget.TextView
import br.com.ceratodash.data.Prefs
import br.com.ceratodash.obd.LiveHistory
import br.com.ceratodash.obd.Sensor
import br.com.ceratodash.obd.Sensors

/** Telemetria: gráficos ao vivo dos últimos 60 s dos sensores escolhidos, com pico e mínimo da sessão. */
class TelemetryScreen(context: Context) : LinearLayout(context) {

    private companion object {
        const val MAX_CHARTS = 6
    }

    private val chips = LinearLayout(context).apply { orientation = HORIZONTAL }
    private val charts = ChartsView(context)
    private val empty = context.label("Aguardando dados do carro. Só aparecem os sensores que o carro responder.", 18f, Theme.MUTED)
    private var shownKeys: List<String> = emptyList()
    private val selected = Prefs.chartKeys(context).toMutableList()

    init {
        orientation = VERTICAL
        setPadding(context.dp(24), context.dp(16), context.dp(24), context.dp(16))
        addView(context.label("Telemetria", 24f, Theme.TEXT, true))
        val scroll = HorizontalScrollView(context).apply { isHorizontalScrollBarEnabled = false }
        scroll.addView(chips)
        addView(scroll, LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT).apply { topMargin = context.dp(8) })
        addView(empty, LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT).apply { topMargin = context.dp(16) })
        addView(charts, LayoutParams(LayoutParams.MATCH_PARENT, 0, 1f).apply { topMargin = context.dp(8) })
    }

    /** Chamado quando chega dado novo e a tela está visível. */
    fun refresh() {
        val h = LiveHistory.history
        val avail = Sensors.all.filter { h.hasData(it.key) }.map { it.key }
        if (avail != shownKeys) {
            shownKeys = avail
            rebuildChips()
        }
        empty.visibility = if (avail.isEmpty()) VISIBLE else GONE
        var chosen = selected.filter { it in avail }
        if (chosen.isEmpty()) chosen = avail.take(3)
        charts.setData(chosen.mapNotNull { Sensors.byKey(it) })
    }

    private fun rebuildChips() {
        chips.removeAllViews()
        for (key in shownKeys) {
            val s = Sensors.byKey(key) ?: continue
            val chip = TextView(context).apply {
                text = if (s.unit == "L/h" || s.unit == "km/L") "${s.label} (${s.unit})" else s.label
                textSize = Theme.LABEL
                typeface = Theme.bodyMedium
                gravity = android.view.Gravity.CENTER
                minHeight = context.dp(48)
                setPadding(context.dp(16), 0, context.dp(16), 0)
                setOnClickListener { toggle(key) }
            }
            styleChip(chip, key in selected)
            chips.addView(chip, LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT).apply { rightMargin = context.dp(8) })
        }
    }

    private fun styleChip(chip: TextView, on: Boolean) {
        chip.setTextColor(if (on) Theme.INFO else Theme.MUTED)
        chip.background = context.rounded(
            if (on) Theme.HIGH else Theme.SURFACE, 12,
            if (on) Theme.INFO else Theme.MUTED, 1
        )
    }

    private fun toggle(key: String) {
        if (key in selected) selected.remove(key) else {
            selected.add(key)
            while (selected.size > MAX_CHARTS) selected.removeAt(0)
        }
        Prefs.setChartKeys(context, selected)
        shownKeys = emptyList() // força refazer os chips com o novo estado
        refresh()
    }
}

/** Desenha uma grade de gráficos de linha. Sem animação contínua: só redesenha quando chega dado. */
class ChartsView(context: Context) : View(context) {
    private var sensors: List<Sensor> = emptyList()
    private val buf = FloatArray(LiveHistory.history.size)

    private val card = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Theme.SURFACE }
    private val line = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Theme.INFO
        style = Paint.Style.STROKE
        strokeWidth = context.dp(2).toFloat()
        strokeJoin = Paint.Join.ROUND
    }
    private val grid = Paint().apply {
        color = Theme.HIGH
        strokeWidth = 1f
    }
    private val txt = Paint(Paint.ANTI_ALIAS_FLAG)
    private val path = Path()
    private val rect = RectF()

    fun setData(list: List<Sensor>) {
        sensors = list
        invalidate()
    }

    override fun onDraw(c: Canvas) {
        val n = sensors.size
        if (n == 0) return
        val cols = if (n == 1) 1 else 2
        val rows = (n + cols - 1) / cols
        val gap = context.dp(12).toFloat()
        val cw = (width - gap * (cols - 1)) / cols
        val ch = (height - gap * (rows - 1)) / rows
        val h = LiveHistory.history
        val pad = context.dp(12).toFloat()
        for ((i, s) in sensors.withIndex()) {
            val left = (i % cols) * (cw + gap)
            val top = (i / cols) * (ch + gap)
            rect.set(left, top, left + cw, top + ch)
            c.drawRoundRect(rect, context.dp(16).toFloat(), context.dp(16).toFloat(), card)

            val count = h.copyInto(s.key, buf)
            val last = if (count > 0) buf[count - 1] else Float.NaN

            txt.textAlign = Paint.Align.LEFT
            txt.typeface = Theme.bodyMedium
            txt.color = Theme.MUTED
            txt.textSize = context.sp(14f)
            val unit = if (s.unit.isNotEmpty()) " (${s.unit})" else ""
            c.drawText(s.label + unit, left + pad, top + pad + txt.textSize, txt)

            txt.textAlign = Paint.Align.RIGHT
            txt.typeface = Theme.numSemi
            txt.color = Theme.TEXT
            txt.textSize = context.sp(28f)
            val value = if (last.isNaN()) "--" else String.format("%.${s.decimals}f", last)
            c.drawText(value, left + cw - pad, top + pad + txt.textSize * 0.9f, txt)

            val chartTop = top + pad * 2 + context.sp(28f)
            val chartBottom = top + ch - pad * 2 - context.sp(14f)
            if (chartBottom > chartTop + 20) drawLine(c, count, left + pad, chartTop, left + cw - pad, chartBottom)

            txt.textAlign = Paint.Align.LEFT
            txt.typeface = Theme.bodyMedium
            txt.color = Theme.MUTED
            txt.textSize = context.sp(14f)
            val peak = h.peak(s.key)
            val min = h.min(s.key)
            val fmt = "%.${s.decimals}f"
            val foot = "Pico " + (peak?.let { String.format(fmt, it) } ?: "--") +
                "   Mínimo " + (min?.let { String.format(fmt, it) } ?: "--")
            c.drawText(foot, left + pad, top + ch - pad, txt)
        }
    }

    private fun drawLine(c: Canvas, count: Int, l: Float, t: Float, r: Float, b: Float) {
        c.drawLine(l, b, r, b, grid)
        c.drawLine(l, t, r, t, grid)
        if (count < 2) return
        var lo = Float.POSITIVE_INFINITY
        var hi = Float.NEGATIVE_INFINITY
        for (k in 0 until count) {
            val v = buf[k]
            if (v.isNaN()) continue
            if (v < lo) lo = v
            if (v > hi) hi = v
        }
        if (lo > hi) return
        if (hi - lo < 1f) {
            lo -= 0.5f
            hi += 0.5f
        }
        val margin = (hi - lo) * 0.1f
        lo -= margin
        hi += margin
        val total = buf.size - 1
        path.reset()
        var pen = false
        for (k in 0 until count) {
            val v = buf[k]
            if (v.isNaN()) {
                pen = false
                continue
            }
            val x = l + (r - l) * (k + (buf.size - count)) / total
            val y = b - (b - t) * (v - lo) / (hi - lo)
            if (pen) path.lineTo(x, y) else path.moveTo(x, y)
            pen = true
        }
        c.drawPath(path, line)
    }
}
