package br.com.ceratodash.ui

import android.content.Context
import android.content.res.ColorStateList
import android.graphics.drawable.GradientDrawable
import android.graphics.drawable.StateListDrawable
import android.util.TypedValue
import android.view.Gravity
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView

/** Auxiliares de interface compartilhados pelas telas. Cores e fontes vêm sempre do Theme. */
fun Context.dp(v: Int): Int = (v * resources.displayMetrics.density).toInt()

fun Context.sp(v: Float): Float =
    TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_SP, v, resources.displayMetrics)

fun Context.rounded(color: Int, radiusDp: Int = 16, strokeColor: Int = 0, strokeDp: Int = 0) =
    GradientDrawable().apply {
        cornerRadius = dp(radiusDp).toFloat()
        setColor(color)
        if (strokeDp > 0) setStroke(dp(strokeDp), strokeColor)
    }

/** Texto geral (Inter). Com bold = true vira título (Rajdhani Bold). */
fun Context.label(
    text: String,
    sizeSp: Float = Theme.TEXT_SIZE,
    color: Int = Theme.TEXT,
    bold: Boolean = false
) = TextView(this).apply {
    this.text = text
    textSize = maxOf(sizeSp, Theme.MIN_SIZE)
    setTextColor(color)
    typeface = if (bold) Theme.numBold else Theme.body
}

/** Número ou valor de destaque (Rajdhani SemiBold). */
fun Context.number(text: String, sizeSp: Float = Theme.VALUE_SECONDARY, color: Int = Theme.TEXT) =
    TextView(this).apply {
        this.text = text
        textSize = sizeSp
        setTextColor(color)
        typeface = Theme.numSemi
    }

/** Botão grande (mínimo de 56 dp) com os estados normal, pressionado e desabilitado. */
fun Context.bigButton(text: String, onClick: () -> Unit): Button = Button(this).apply {
    this.text = text
    isAllCaps = false
    textSize = Theme.LABEL
    typeface = Theme.bodyMedium
    stateStyle(this@bigButton, selected = false)
    setPadding(dp(24), dp(8), dp(24), dp(8))
    minHeight = dp(56)
    minimumHeight = dp(56)
    minWidth = dp(56)
    minimumWidth = dp(56)
    gravity = Gravity.CENTER
    setOnClickListener { onClick() }
    layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT)
        .apply { rightMargin = dp(12) }
}

/** Aplica o visual de botão. selected = opção escolhida (borda azul). */
fun Button.stateStyle(c: Context, selected: Boolean) {
    val states = StateListDrawable()
    states.addState(intArrayOf(-android.R.attr.state_enabled), c.rounded(Theme.SURFACE))
    states.addState(intArrayOf(android.R.attr.state_pressed), c.rounded(Theme.PRESSED, 16, Theme.INFO, 2))
    states.addState(
        intArrayOf(),
        if (selected) c.rounded(Theme.HIGH, 16, Theme.INFO, 2) else c.rounded(Theme.HIGH)
    )
    background = states
    setTextColor(
        ColorStateList(
            arrayOf(intArrayOf(-android.R.attr.state_enabled), intArrayOf()),
            intArrayOf(Theme.MUTED, if (selected) Theme.INFO else Theme.TEXT)
        )
    )
}

/** Liga ou desliga um botão (o visual do estado desabilitado vem do stateStyle). */
fun Button.enable(on: Boolean) {
    isEnabled = on
}
