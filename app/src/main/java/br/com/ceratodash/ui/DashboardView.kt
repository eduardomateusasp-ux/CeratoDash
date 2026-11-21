package br.com.ceratodash.ui

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import android.graphics.Shader
import android.os.SystemClock
import android.view.View
import br.com.ceratodash.data.AlertLimits
import br.com.ceratodash.obd.LiveHistory
import br.com.ceratodash.obd.Telemetry
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.exp
import kotlin.math.sin

/**
 * Painel desenhado direto no Canvas, em três zonas: conta-giros à esquerda, carro e barra de
 * rotação ao centro, velocímetro à direita, e cartões de métrica embaixo.
 * Máximo de 30 quadros por segundo. Só anima enquanto os ponteiros se movem ou a barra pisca.
 */
class DashboardView(context: Context) : View(context) {

    private companion object {
        const val START = 150f
        const val SWEEP = 240f
        const val SEGMENTS = 24
        const val RPM_MAX = 8000f
    }

    /** Escala de um gauge. Posições dos traços e textos são calculadas uma vez, não a cada quadro. */
    private class Gauge(
        val max: Float, minorStep: Float, labelStep: Float, labelDiv: Float,
        val redFrom: Float, val unit: String
    ) {
        val n = (max / minorStep).toInt() + 1
        val cosT = FloatArray(n)
        val sinT = FloatArray(n)
        val major = BooleanArray(n)
        val labels = arrayOfNulls<String>(n)

        init {
            for (k in 0 until n) {
                val v = k * minorStep
                val ang = Math.toRadians((START + SWEEP * v / max).toDouble())
                cosT[k] = cos(ang).toFloat()
                sinT[k] = sin(ang).toFloat()
                val q = v / labelStep
                major[k] = abs(q - Math.round(q)) < 0.001f
                if (major[k]) labels[k] = (v / labelDiv).toInt().toString()
            }
        }
    }

    private val rpmGauge = Gauge(RPM_MAX, 500f, 1000f, 1000f, 6500f, "rpm")
    private val speedGauge = Gauge(240f, 20f, 40f, 1f, 1000f, "km/h")

    private var t = Telemetry()
    var limits = AlertLimits()
        set(v) {
            field = v
            cardList = cards()
            invalidate()
        }
    private var dRpm = 0f
    private var dSpeed = 0f
    private var lastFrame = 0L

    private val arcPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeCap = Paint.Cap.ROUND
    }
    private val fill = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.FILL }
    private val line = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeCap = Paint.Cap.ROUND
        strokeJoin = Paint.Join.ROUND
    }
    private val text = Paint(Paint.ANTI_ALIAS_FLAG).apply { textAlign = Paint.Align.CENTER }
    private val tmp = RectF()
    private val arcRect = RectF()
    private val spark = Path()
    private val buf = FloatArray(LiveHistory.history.size)

    private val carPath = Path().apply {
        moveTo(5f, 27f)
        lineTo(3.5f, 20f)
        cubicTo(3.2f, 17.5f, 6f, 16.5f, 12f, 16f)
        lineTo(26f, 14.5f)
        cubicTo(33f, 8f, 41f, 5f, 52f, 5f)
        cubicTo(62f, 5f, 70f, 8.5f, 76f, 14f)
        lineTo(91f, 17f)
        cubicTo(96f, 18f, 98.5f, 21f, 98f, 25f)
        lineTo(96.5f, 27f)
        close()
    }
    private val windowPath = Path().apply {
        moveTo(29f, 14f)
        lineTo(37f, 8.2f)
        cubicTo(40f, 7f, 46f, 6.3f, 51f, 6.3f)
        lineTo(51f, 14f)
        close()
        moveTo(54f, 14f)
        lineTo(54f, 6.3f)
        cubicTo(62f, 6.5f, 67f, 9f, 72f, 14f)
        close()
    }
    private val bodyShader = LinearGradient(
        0f, 5f, 0f, 27f, 0xFF3A444F.toInt(), 0xFF0E1216.toInt(), Shader.TileMode.CLAMP
    )
    private val carBitmap: Bitmap? = loadCarPhoto()

    private class Card(val label: String, val value: String, val color: Int, val key: String?)

    private var cardList: List<Card> = cards()

    private fun loadCarPhoto(): Bitmap? {
        val id = resources.getIdentifier("car_photo", "drawable", context.packageName)
        if (id == 0) return null
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeResource(resources, id, bounds)
        var sample = 1
        while (bounds.outWidth / sample > 1200) sample *= 2
        val opts = BitmapFactory.Options().apply { inSampleSize = sample }
        return BitmapFactory.decodeResource(resources, id, opts)
    }

    fun update(newT: Telemetry) {
        t = newT
        // Os cartões só mudam quando chega dado novo (poucas vezes por segundo), não a cada quadro.
        cardList = cards()
        invalidate()
    }

    private fun withAlpha(color: Int, a: Int) = (color and 0x00FFFFFF) or (a shl 24)

    override fun onDraw(c: Canvas) {
        val w = width.toFloat()
        val h = height.toFloat()
        c.drawColor(Theme.BG)

        val targetRpm = t.rpm ?: 0f
        val targetSpeed = t.speed ?: 0f
        // Ponteiro suave: cerca de 95% do caminho em 150 ms, independente da taxa de quadros.
        val now = SystemClock.uptimeMillis()
        val dt = if (lastFrame == 0L) 33L else (now - lastFrame).coerceIn(1L, 100L)
        lastFrame = now
        val k = 1f - exp(-dt / 50f)
        dRpm += (targetRpm - dRpm) * k
        dSpeed += (targetSpeed - dSpeed) * k

        val r = minOf(w * 0.19f, h * 0.36f)
        val cy = h * 0.42f
        drawGauge(
            c, rpmGauge, w * 0.19f, cy, r, dRpm,
            if (t.rpm != null) dRpm.toInt().toString() else "--"
        )
        drawGauge(
            c, speedGauge, w * 0.81f, cy, r, dSpeed,
            if (t.speed != null) dSpeed.toInt().toString() else "--"
        )
        val blinking = (t.rpm ?: 0f) >= limits.rpmMax - 300f
        drawRpmBar(c, w, h, now, blinking)
        drawCar(c, w * 0.5f, cy, w * 0.31f, h)
        drawCards(c, w, h)

        if (blinking || abs(targetRpm - dRpm) > 1f || abs(targetSpeed - dSpeed) > 0.3f) {
            // Máximo de 30 quadros por segundo (o padrão seria a taxa da tela, 60 ou mais).
            postInvalidateDelayed(33)
        } else {
            lastFrame = 0L
        }
    }

    private fun drawGauge(c: Canvas, g: Gauge, cx: Float, cy: Float, r: Float, value: Float, valueText: String) {
        val sw = r * 0.11f
        val rect = arcRect
        rect.set(cx - r + sw / 2, cy - r + sw / 2, cx + r - sw / 2, cy + r - sw / 2)
        arcPaint.strokeWidth = sw
        arcPaint.color = Theme.HIGH
        c.drawArc(rect, START, SWEEP, false, arcPaint)

        val frac = (value / g.max).coerceIn(0f, 1f)
        val redFrac = (g.redFrom / g.max).coerceIn(0f, 1f)
        if (redFrac < 1f) {
            arcPaint.color = withAlpha(Theme.ACCENT, 80)
            c.drawArc(rect, START + SWEEP * redFrac, SWEEP * (1f - redFrac), false, arcPaint)
        }
        if (frac > 0.002f) {
            arcPaint.color = Theme.INFO
            c.drawArc(rect, START, SWEEP * minOf(frac, redFrac), false, arcPaint)
            if (frac > redFrac) {
                arcPaint.color = Theme.ACCENT
                c.drawArc(rect, START + SWEEP * redFrac, SWEEP * (frac - redFrac), false, arcPaint)
            }
        }

        line.color = Theme.MUTED
        text.color = Theme.MUTED
        text.typeface = Theme.numSemi
        text.textAlign = Paint.Align.CENTER
        text.textSize = maxOf(context.sp(Theme.MIN_SIZE), r * 0.11f)
        for (i in 0 until g.n) {
            val major = g.major[i]
            val r1 = r * (if (major) 0.70f else 0.77f)
            val r2 = r * 0.83f
            line.strokeWidth = if (major) r * 0.022f else r * 0.012f
            val ca = g.cosT[i]
            val sa = g.sinT[i]
            c.drawLine(cx + ca * r1, cy + sa * r1, cx + ca * r2, cy + sa * r2, line)
            val lbl = g.labels[i]
            if (lbl != null) c.drawText(lbl, cx + ca * r * 0.58f, cy + sa * r * 0.58f + r * 0.04f, text)
        }

        if (frac > 0.002f) {
            val ang = Math.toRadians((START + SWEEP * frac).toDouble())
            line.color = Theme.TEXT
            line.strokeWidth = r * 0.03f
            val ca = cos(ang).toFloat()
            val sa = sin(ang).toFloat()
            c.drawLine(cx + ca * r * 0.80f, cy + sa * r * 0.80f, cx + ca * r * 0.97f, cy + sa * r * 0.97f, line)
        }

        // Valor principal: 96 sp, reduzido só se não couber dentro do arco.
        text.color = Theme.TEXT
        text.typeface = Theme.numBold
        text.textSize = minOf(context.sp(Theme.VALUE_MAIN), r * 0.5f)
        c.drawText(valueText, cx, cy + r * 0.16f, text)
        text.color = Theme.MUTED
        text.typeface = Theme.bodyMedium
        text.textSize = maxOf(context.sp(Theme.LABEL), r * 0.13f)
        c.drawText(g.unit, cx, cy + r * 0.45f, text)
    }

    /** Barra de rotação em segmentos. Pisca perto do limite de rotação configurado. */
    private fun drawRpmBar(c: Canvas, w: Float, h: Float, now: Long, blinking: Boolean) {
        val barW = w * 0.30f
        val barH = h * 0.05f
        val left = w * 0.5f - barW / 2
        val top = h * 0.07f
        val gap = context.dp(3).toFloat()
        val sw = (barW - gap * (SEGMENTS - 1)) / SEGMENTS
        val lit = Math.ceil((dRpm / RPM_MAX * SEGMENTS).toDouble()).toInt().coerceIn(0, SEGMENTS)
        val off = blinking && (now / 120L) % 2L == 0L
        for (i in 0 until SEGMENTS) {
            val rpmAt = (i + 0.5f) / SEGMENTS * RPM_MAX
            fill.color = when {
                i >= lit || off -> Theme.HIGH
                rpmAt >= 6500f -> Theme.ACCENT
                rpmAt >= 5500f -> Theme.WARN
                else -> Theme.INFO
            }
            val x = left + i * (sw + gap)
            tmp.set(x, top, x + sw, top + barH)
            c.drawRoundRect(tmp, sw * 0.25f, sw * 0.25f, fill)
        }
    }

    private fun drawCar(c: Canvas, cx: Float, cy: Float, carW: Float, h: Float) {
        val s = carW / 100f
        val carH = 36f * s
        val top = cy - carH / 2f - h * 0.02f
        val bmp = carBitmap
        if (bmp != null) {
            val dw = carW * 1.15f
            val dh = dw * bmp.height / bmp.width
            tmp.set(cx - dw / 2, cy - dh / 2, cx + dw / 2, cy + dh / 2)
            c.drawBitmap(bmp, null, tmp, null)
        } else {
            c.save()
            c.translate(cx - carW / 2f, top)
            c.scale(s, s)

            fill.color = withAlpha(Theme.ACCENT, 70)
            tmp.set(8f, 30.5f, 92f, 32f)
            c.drawRoundRect(tmp, 0.8f, 0.8f, fill)

            fill.shader = bodyShader
            c.drawPath(carPath, fill)
            fill.shader = null
            line.color = withAlpha(Theme.TEXT, 90)
            line.strokeWidth = 0.35f
            c.drawPath(carPath, line)

            fill.color = 0xFF05080B.toInt()
            c.drawPath(windowPath, fill)

            fill.color = 0xFFFFE9A8.toInt()
            tmp.set(93f, 19.5f, 97.5f, 21.5f)
            c.drawRoundRect(tmp, 0.8f, 0.8f, fill)
            fill.color = Theme.ACCENT
            tmp.set(3.4f, 19f, 6f, 21f)
            c.drawRoundRect(tmp, 0.8f, 0.8f, fill)

            for (wx in WHEELS) {
                fill.color = Theme.BG
                c.drawCircle(wx, 27.5f, 8f, fill)
                fill.color = 0xFF05070A.toInt()
                c.drawCircle(wx, 27.5f, 6.6f, fill)
                fill.color = 0xFF3A424B.toInt()
                c.drawCircle(wx, 27.5f, 4.2f, fill)
                fill.color = Theme.ACCENT
                c.drawCircle(wx, 27.5f, 1.2f, fill)
            }
            c.restore()
        }
        text.color = Theme.MUTED
        text.typeface = Theme.bodyMedium
        text.textAlign = Paint.Align.CENTER
        text.textSize = maxOf(context.sp(Theme.LABEL), h * 0.035f)
        c.drawText("Kia Cerato", cx, top + carH + h * 0.07f, text)
    }

    private val WHEELS = floatArrayOf(22f, 78f)

    private fun cards(): List<Card> {
        val tt = t
        val lim = limits
        val coolant = tt.coolant
        val volt = tt.voltage
        val kml = tt.kmPerLiter
        val lph = tt.litersPerHour
        return listOf(
            Card(
                "Água (°C)", coolant?.let { it.toInt().toString() } ?: "--",
                when {
                    coolant == null -> Theme.MUTED
                    coolant >= lim.waterDanger -> Theme.DANGER
                    coolant >= lim.waterWarn -> Theme.WARN
                    coolant < 60f -> Theme.INFO
                    else -> Theme.OK
                }, "coolant"
            ),
            Card("Admissão (°C)", tt.intake?.let { it.toInt().toString() } ?: "--", Theme.TEXT, "intake"),
            Card(
                "Bateria (V)", volt?.let { String.format("%.1f", it) } ?: "--",
                when {
                    volt == null -> Theme.MUTED
                    volt < lim.voltLow || volt > lim.voltHigh -> Theme.DANGER
                    volt < 12.2f -> Theme.WARN
                    else -> Theme.OK
                }, "voltage"
            ),
            Card("Carga do motor (%)", tt.load?.let { it.toInt().toString() } ?: "--", Theme.TEXT, "load"),
            Card("Borboleta (%)", tt.throttle?.let { it.toInt().toString() } ?: "--", Theme.TEXT, "throttle"),
            when {
                kml != null -> Card("Consumo (km/L)", String.format("%.1f", kml), Theme.TEXT, "kml")
                lph != null -> Card("Consumo (L/h)", String.format("%.1f", lph), Theme.TEXT, "lph")
                else -> Card("Consumo", "--", Theme.MUTED, null)
            }
        )
    }

    private fun drawCards(c: Canvas, w: Float, h: Float) {
        val list = cardList
        val margin = context.dp(24).toFloat()
        val gap = context.dp(8).toFloat()
        val n = list.size
        val cw = (w - margin * 2 - gap * (n - 1)) / n
        val ch = maxOf(h * 0.2f, context.dp(88).toFloat())
        val top = h - ch - context.dp(16).toFloat()
        val pad = context.dp(12).toFloat()
        val radius = context.dp(16).toFloat()
        for ((i, card) in list.withIndex()) {
            val left = margin + i * (cw + gap)
            fill.color = Theme.SURFACE
            tmp.set(left, top, left + cw, top + ch)
            c.drawRoundRect(tmp, radius, radius, fill)

            text.textAlign = Paint.Align.LEFT
            text.color = Theme.MUTED
            text.typeface = Theme.bodyMedium
            text.textSize = context.sp(Theme.MIN_SIZE)
            c.drawText(card.label, left + pad, top + pad + text.textSize * 0.85f, text)

            text.color = card.color
            text.typeface = Theme.numSemi
            text.textSize = minOf(context.sp(Theme.VALUE_SECONDARY), ch * 0.45f)
            c.drawText(card.value, left + pad, top + ch - pad, text)

            val key = card.key
            if (key != null) drawSpark(c, key, left + cw * 0.55f, top + ch * 0.45f, left + cw - pad, top + ch - pad)
        }
    }

    /** Mini gráfico dos últimos 60 s dentro do cartão. */
    private fun drawSpark(c: Canvas, key: String, l: Float, t: Float, r: Float, b: Float) {
        val count = LiveHistory.history.copyInto(key, buf)
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
        spark.reset()
        var pen = false
        val total = buf.size - 1
        for (k in 0 until count) {
            val v = buf[k]
            if (v.isNaN()) {
                pen = false
                continue
            }
            val x = l + (r - l) * (k + (buf.size - count)) / total
            val y = b - (b - t) * (v - lo) / (hi - lo)
            if (pen) spark.lineTo(x, y) else spark.moveTo(x, y)
            pen = true
        }
        line.color = withAlpha(Theme.INFO, 200)
        line.strokeWidth = context.dp(2).toFloat()
        c.drawPath(spark, line)
    }
}
