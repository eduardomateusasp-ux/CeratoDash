package br.com.ceratodash.service

import android.content.Context
import android.graphics.Canvas
import android.graphics.PixelFormat
import android.graphics.Paint
import android.graphics.RectF
import android.os.Build
import android.provider.Settings
import android.view.Gravity
import android.view.View
import android.view.WindowManager
import br.com.ceratodash.data.AppState
import br.com.ceratodash.data.Level
import br.com.ceratodash.obd.Telemetry
import br.com.ceratodash.ui.Theme
import br.com.ceratodash.ui.dp
import br.com.ceratodash.ui.sp

/**
 * Faixa reduzida do painel (rotação, velocidade e água) sobre outros apps, como o Google Maps e o Waze.
 * Não recebe toque (não atrapalha o mapa) e só aparece com a permissão "Sobrepor outros apps".
 * Se a central não permitir, não força nada: guarda o motivo em AppState.overlayProblem.
 * NÃO TESTADO em central real.
 */
class OverlayStrip(private val ctx: Context) {
    private val wm = ctx.getSystemService(Context.WINDOW_SERVICE) as WindowManager
    private var view: StripView? = null

    fun canDraw(): Boolean = Build.VERSION.SDK_INT < 23 || Settings.canDrawOverlays(ctx)

    fun show() {
        if (view != null) return
        val v = StripView(ctx)
        @Suppress("DEPRECATION")
        val type = if (Build.VERSION.SDK_INT >= 26) WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
        else WindowManager.LayoutParams.TYPE_PHONE
        val lp = WindowManager.LayoutParams(
            ctx.dp(380), ctx.dp(64), type,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE,
            PixelFormat.TRANSLUCENT
        )
        lp.gravity = Gravity.TOP or Gravity.START
        lp.x = ctx.dp(16)
        lp.y = ctx.dp(12)
        try {
            wm.addView(v, lp)
            view = v
        } catch (e: Exception) {
            AppState.overlayProblem = "Esta central não permitiu a faixa sobre o mapa."
        }
    }

    fun update(t: Telemetry, level: Level?) {
        view?.set(t, level)
    }

    fun hide() {
        val v = view ?: return
        try {
            wm.removeView(v)
        } catch (_: Exception) {
        }
        view = null
    }
}

private class StripView(ctx: Context) : View(ctx) {
    private var t = Telemetry()
    private var level: Level? = null
    private val bg = Paint(Paint.ANTI_ALIAS_FLAG)
    private val border = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = ctx.dp(2).toFloat()
    }
    private val text = Paint(Paint.ANTI_ALIAS_FLAG).apply { textAlign = Paint.Align.CENTER }
    private val rect = RectF()

    fun set(newT: Telemetry, newLevel: Level?) {
        t = newT
        level = newLevel
        invalidate()
    }

    override fun onDraw(c: Canvas) {
        val w = width.toFloat()
        val h = height.toFloat()
        val r = context.dp(16).toFloat()
        rect.set(1f, 1f, w - 1f, h - 1f)
        bg.color = (Theme.BG and 0x00FFFFFF) or (0xE6 shl 24)
        c.drawRoundRect(rect, r, r, bg)
        border.color = when (level) {
            Level.PERIGO -> Theme.DANGER
            Level.ATENCAO -> Theme.WARN
            null -> Theme.HIGH
        }
        c.drawRoundRect(rect, r, r, border)
        val cols = listOf(
            t.rpm?.toInt()?.toString() to "rpm",
            t.speed?.toInt()?.toString() to "km/h",
            t.coolant?.toInt()?.toString() to "água °C"
        )
        val cw = w / 3f
        for ((i, p) in cols.withIndex()) {
            val cx = cw * i + cw / 2f
            text.color = Theme.TEXT
            text.typeface = Theme.numBold
            text.textSize = context.sp(28f)
            c.drawText(p.first ?: "--", cx, h * 0.55f, text)
            text.color = Theme.MUTED
            text.typeface = Theme.bodyMedium
            text.textSize = context.sp(Theme.MIN_SIZE)
            c.drawText(p.second, cx, h * 0.88f, text)
        }
    }
}
