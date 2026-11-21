package br.com.ceratodash

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import android.graphics.Shader
import android.graphics.Typeface
import android.view.View
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.sin

/** Painel desenhado direto no Canvas: conta-giros, velocímetro, carro e indicadores. */
class DashboardView(context: Context) : View(context) {

    companion object {
        val BG = 0xFF0B0D10.toInt()
        val SURFACE = 0xFF14181D.toInt()
        val HIGH = 0xFF1C2229.toInt()
        val ACCENT = 0xFFFF3B30.toInt()
        val INFO = 0xFF00D1FF.toInt()
        val OK = 0xFF2ED573.toInt()
        val WARN = 0xFFFFB020.toInt()
        val DANGER = 0xFFFF453A.toInt()
        val TEXT = 0xFFF2F5F7.toInt()
        val MUTED = 0xFF8A94A0.toInt()
        private const val START = 150f
        private const val SWEEP = 240f
    }

    private var t = Telemetry()
    private var dRpm = 0f
    private var dSpeed = 0f

    private val arcPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeCap = Paint.Cap.ROUND
    }
    private val fill = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.FILL }
    private val line = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeCap = Paint.Cap.ROUND
    }
    private val text = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        textAlign = Paint.Align.CENTER
        typeface = Typeface.create("sans-serif-condensed", Typeface.BOLD)
    }

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

    private val tmp = RectF()
    private val arcRect = RectF()
    private var cardList: List<Card> = cards()
    private var alertMsg: String? = null

    fun update(newT: Telemetry) {
        t = newT
        // Textos e cartões só mudam quando chega dado novo (poucas vezes por segundo), não a cada quadro.
        cardList = cards()
        alertMsg = alertText()
        invalidate()
    }

    override fun onDraw(c: Canvas) {
        val w = width.toFloat()
        val h = height.toFloat()
        c.drawColor(BG)

        val targetRpm = t.rpm ?: 0f
        val targetSpeed = t.speed ?: 0f
        dRpm += (targetRpm - dRpm) * 0.25f
        dSpeed += (targetSpeed - dSpeed) * 0.25f

        val r = minOf(w * 0.19f, h * 0.36f)
        val cy = h * 0.42f
        drawGauge(
            c, w * 0.19f, cy, r, dRpm, 8000f, 500f, 1000f, 1000f, 6500f,
            if (t.rpm != null) dRpm.toInt().toString() else "--", "rpm"
        )
        drawGauge(
            c, w * 0.81f, cy, r, dSpeed, 240f, 20f, 40f, 1f, 9999f,
            if (t.speed != null) dSpeed.toInt().toString() else "--", "km/h"
        )
        drawCar(c, w * 0.5f, cy, w * 0.31f, h)
        drawCards(c, w, h)
        drawAlert(c, w, h)

        if (abs(targetRpm - dRpm) > 1f || abs(targetSpeed - dSpeed) > 0.3f) {
            // Máximo de 30 quadros por segundo (o padrão seria a taxa da tela, 60 ou mais).
            postInvalidateDelayed(33)
        }
    }

    private fun withAlpha(color: Int, a: Int) = (color and 0x00FFFFFF) or (a shl 24)

    private fun drawGauge(
        c: Canvas, cx: Float, cy: Float, r: Float, value: Float, max: Float,
        minorStep: Float, labelStep: Float, labelDiv: Float, redFrom: Float,
        valueText: String, unit: String
    ) {
        val sw = r * 0.11f
        val rect = arcRect
        rect.set(cx - r + sw / 2, cy - r + sw / 2, cx + r - sw / 2, cy + r - sw / 2)
        arcPaint.strokeWidth = sw
        arcPaint.color = HIGH
        c.drawArc(rect, START, SWEEP, false, arcPaint)

        val frac = (value / max).coerceIn(0f, 1f)
        val redFrac = (redFrom / max).coerceIn(0f, 1f)
        if (redFrac < 1f) {
            arcPaint.color = withAlpha(ACCENT, 80)
            c.drawArc(rect, START + SWEEP * redFrac, SWEEP * (1f - redFrac), false, arcPaint)
        }
        if (frac > 0.002f) {
            arcPaint.color = INFO
            c.drawArc(rect, START, SWEEP * minOf(frac, redFrac), false, arcPaint)
            if (frac > redFrac) {
                arcPaint.color = ACCENT
                c.drawArc(rect, START + SWEEP * redFrac, SWEEP * (frac - redFrac), false, arcPaint)
            }
        }

        line.color = MUTED
        text.color = MUTED
        text.textSize = r * 0.11f
        var v = 0f
        while (v <= max + 0.01f) {
            val ang = Math.toRadians((START + SWEEP * v / max).toDouble())
            val rem = v % labelStep
            val major = rem < 0.01f || labelStep - rem < 0.01f
            val r1 = r * (if (major) 0.70f else 0.77f)
            val r2 = r * 0.83f
            line.strokeWidth = if (major) r * 0.022f else r * 0.012f
            val ca = cos(ang).toFloat()
            val sa = sin(ang).toFloat()
            c.drawLine(cx + ca * r1, cy + sa * r1, cx + ca * r2, cy + sa * r2, line)
            if (major) {
                c.drawText(
                    (v / labelDiv).toInt().toString(),
                    cx + ca * r * 0.58f, cy + sa * r * 0.58f + r * 0.04f, text
                )
            }
            v += minorStep
        }

        if (frac > 0.002f) {
            val ang = Math.toRadians((START + SWEEP * frac).toDouble())
            line.color = TEXT
            line.strokeWidth = r * 0.03f
            val ca = cos(ang).toFloat()
            val sa = sin(ang).toFloat()
            c.drawLine(cx + ca * r * 0.80f, cy + sa * r * 0.80f, cx + ca * r * 0.97f, cy + sa * r * 0.97f, line)
        }

        text.color = TEXT
        text.textSize = r * 0.36f
        c.drawText(valueText, cx, cy + r * 0.13f, text)
        text.color = MUTED
        text.textSize = r * 0.13f
        c.drawText(unit, cx, cy + r * 0.42f, text)
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

            fill.color = withAlpha(ACCENT, 70)
            tmp.set(8f, 30.5f, 92f, 32f)
            c.drawRoundRect(tmp, 0.8f, 0.8f, fill)

            fill.shader = bodyShader
            c.drawPath(carPath, fill)
            fill.shader = null
            line.color = withAlpha(TEXT, 90)
            line.strokeWidth = 0.35f
            c.drawPath(carPath, line)

            fill.color = 0xFF05080B.toInt()
            c.drawPath(windowPath, fill)

            fill.color = 0xFFFFE9A8.toInt()
            tmp.set(93f, 19.5f, 97.5f, 21.5f)
            c.drawRoundRect(tmp, 0.8f, 0.8f, fill)
            fill.color = ACCENT
            tmp.set(3.4f, 19f, 6f, 21f)
            c.drawRoundRect(tmp, 0.8f, 0.8f, fill)

            for (wx in floatArrayOf(22f, 78f)) {
                fill.color = BG
                c.drawCircle(wx, 27.5f, 8f, fill)
                fill.color = 0xFF05070A.toInt()
                c.drawCircle(wx, 27.5f, 6.6f, fill)
                fill.color = 0xFF3A424B.toInt()
                c.drawCircle(wx, 27.5f, 4.2f, fill)
                fill.color = ACCENT
                c.drawCircle(wx, 27.5f, 1.2f, fill)
            }
            c.restore()
        }
        text.color = MUTED
        text.textSize = h * 0.035f
        c.drawText("Kia Cerato", cx, top + carH + h * 0.07f, text)
    }

    private class Card(val label: String, val value: String, val color: Int)

    private fun cards(): List<Card> {
        val tt = t
        val coolant = tt.coolant
        val volt = tt.voltage
        val kml = tt.kmPerLiter
        val lph = tt.litersPerHour
        return listOf(
            Card(
                "Água (°C)", coolant?.let { it.toInt().toString() } ?: "--",
                when {
                    coolant == null -> MUTED
                    coolant >= 108f -> DANGER
                    coolant >= 100f -> WARN
                    coolant < 60f -> INFO
                    else -> OK
                }
            ),
            Card("Admissão (°C)", tt.intake?.let { it.toInt().toString() } ?: "--", TEXT),
            Card(
                "Bateria (V)", volt?.let { String.format("%.1f", it) } ?: "--",
                when {
                    volt == null -> MUTED
                    volt < 11.8f || volt > 15.0f -> DANGER
                    volt < 12.2f -> WARN
                    else -> OK
                }
            ),
            Card("Carga do motor (%)", tt.load?.let { it.toInt().toString() } ?: "--", TEXT),
            Card("Borboleta (%)", tt.throttle?.let { it.toInt().toString() } ?: "--", TEXT),
            when {
                kml != null -> Card("Consumo (km/L)", String.format("%.1f", kml), TEXT)
                lph != null -> Card("Consumo (L/h)", String.format("%.1f", lph), TEXT)
                else -> Card("Consumo", "--", MUTED)
            }
        )
    }

    private fun drawCards(c: Canvas, w: Float, h: Float) {
        val list = cardList
        val margin = w * 0.02f
        val gap = w * 0.012f
        val n = list.size
        val cw = (w - margin * 2 - gap * (n - 1)) / n
        val ch = h * 0.17f
        val top = h - ch - h * 0.035f
        for ((i, card) in list.withIndex()) {
            val left = margin + i * (cw + gap)
            fill.color = SURFACE
            tmp.set(left, top, left + cw, top + ch)
            c.drawRoundRect(tmp, ch * 0.16f, ch * 0.16f, fill)
            fill.color = card.color
            tmp.set(left + cw * 0.08f, top + ch * 0.86f, left + cw * 0.08f + cw * 0.22f, top + ch * 0.92f)
            c.drawRoundRect(tmp, 4f, 4f, fill)
            text.color = MUTED
            text.textSize = ch * 0.15f
            c.drawText(card.label, left + cw / 2, top + ch * 0.27f, text)
            text.color = card.color
            text.textSize = ch * 0.40f
            c.drawText(card.value, left + cw / 2, top + ch * 0.70f, text)
        }
    }

    private fun alertText(): String? {
        val tt = t
        val volt = tt.voltage
        return when {
            (tt.coolant ?: 0f) >= 108f -> "Temperatura do motor alta"
            volt != null && (volt < 11.5f || volt > 15.2f) -> "Tensão da bateria fora do normal"
            (tt.rpm ?: 0f) > 7000f -> "Rotação muito alta"
            else -> null
        }
    }

    private fun drawAlert(c: Canvas, w: Float, h: Float) {
        val msg = alertMsg ?: return
        text.textSize = h * 0.045f
        val tw = text.measureText(msg)
        val padX = h * 0.04f
        val bh = h * 0.08f
        fill.color = DANGER
        tmp.set(w / 2 - tw / 2 - padX, h * 0.02f, w / 2 + tw / 2 + padX, h * 0.02f + bh)
        c.drawRoundRect(tmp, bh * 0.3f, bh * 0.3f, fill)
        text.color = 0xFFFFFFFF.toInt()
        c.drawText(msg, w / 2, h * 0.02f + bh * 0.68f, text)
    }
}
