package br.com.ceratodash.ui

import android.content.Context
import android.graphics.drawable.GradientDrawable
import android.view.Gravity
import android.view.View
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.content.ContextCompat
import br.com.ceratodash.R
import br.com.ceratodash.data.Alert
import br.com.ceratodash.data.DriveEvent
import br.com.ceratodash.data.Level
import br.com.ceratodash.obd.ConnState
import br.com.ceratodash.obd.ObdStatus

/**
 * Faixa de alerta no topo. O alerta nunca depende só da cor: tem ícone e texto.
 * Texto escuro sobre amarelo ou vermelho para manter o contraste.
 */
class AlertBanner(context: Context) : LinearLayout(context) {
    private val icon = ImageView(context)
    private val msg = TextView(context).apply {
        textSize = 20f
        typeface = Theme.numBold
        setTextColor(Theme.BG)
    }
    private var shown: String? = null
    private var shownLevel: Level? = null

    init {
        orientation = HORIZONTAL
        gravity = Gravity.CENTER_VERTICAL
        setPadding(context.dp(24), context.dp(8), context.dp(24), context.dp(8))
        icon.setImageDrawable(ContextCompat.getDrawable(context, R.drawable.ic_alert)?.mutate()?.apply { setTint(Theme.BG) })
        addView(icon, LayoutParams(context.dp(32), context.dp(32)).apply { rightMargin = context.dp(12) })
        addView(msg, LayoutParams(0, LayoutParams.WRAP_CONTENT, 1f))
        visibility = GONE
    }

    fun show(alerts: List<Alert>) {
        if (alerts.isEmpty()) {
            if (visibility != GONE) visibility = GONE
            shown = null
            return
        }
        val top = alerts[0]
        val text = if (alerts.size > 1) top.message + "  (+${alerts.size - 1})" else top.message
        if (text != shown || top.level != shownLevel) {
            shown = text
            shownLevel = top.level
            msg.text = text
            setBackgroundColor(if (top.level == Level.PERIGO) Theme.DANGER else Theme.WARN)
        }
        if (visibility != VISIBLE) visibility = VISIBLE
    }
}

/** Indicador de status da conexão: ponto colorido mais texto. */
class StatusIndicator(context: Context) : LinearLayout(context) {
    private val dot = View(context).apply { background = GradientDrawable().apply { shape = GradientDrawable.OVAL; setColor(Theme.MUTED) } }
    private val text = TextView(context).apply {
        textSize = Theme.LABEL
        typeface = Theme.bodyMedium
        setTextColor(Theme.TEXT)
    }

    init {
        orientation = HORIZONTAL
        gravity = Gravity.CENTER_VERTICAL
        addView(dot, LayoutParams(context.dp(14), context.dp(14)).apply { rightMargin = context.dp(10) })
        addView(text, LayoutParams(0, LayoutParams.WRAP_CONTENT, 1f))
    }

    fun render(s: ObdStatus) {
        val color = when (s.state) {
            ConnState.CONECTADO -> Theme.OK
            ConnState.CONECTANDO, ConnState.INICIALIZANDO -> Theme.WARN
            ConnState.ERRO -> Theme.DANGER
            ConnState.DESCONECTADO -> Theme.MUTED
        }
        (dot.background as GradientDrawable).setColor(color)
        text.text = if (s.message.isBlank()) s.state.label else "${s.state.label}: ${s.message}"
    }
}

/** Dica curta de condução, por alguns segundos. Texto grande e direto: não exige atenção prolongada. */
class CoachBanner(context: Context) : LinearLayout(context) {
    private val msg = TextView(context).apply {
        textSize = 20f
        typeface = Theme.numBold
        setTextColor(Theme.TEXT)
    }
    private var shown: DriveEvent? = null

    init {
        orientation = HORIZONTAL
        gravity = Gravity.CENTER_VERTICAL
        setBackgroundColor(Theme.HIGH)
        setPadding(context.dp(24), context.dp(8), context.dp(24), context.dp(8))
        val icon = ImageView(context)
        icon.setImageDrawable(ContextCompat.getDrawable(context, R.drawable.ic_nav_conducao)?.mutate()?.apply { setTint(Theme.INFO) })
        addView(icon, LayoutParams(context.dp(32), context.dp(32)).apply { rightMargin = context.dp(12) })
        addView(msg, LayoutParams(0, LayoutParams.WRAP_CONTENT, 1f))
        visibility = GONE
    }

    fun show(tip: DriveEvent?) {
        if (tip == null) {
            if (visibility != GONE) visibility = GONE
            shown = null
            return
        }
        if (tip != shown) {
            shown = tip
            msg.text = tip.tip
        }
        if (visibility != VISIBLE) visibility = VISIBLE
    }
}
