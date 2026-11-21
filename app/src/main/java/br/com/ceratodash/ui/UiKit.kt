package br.com.ceratodash.ui

import android.content.Context
import android.graphics.drawable.GradientDrawable
import android.util.TypedValue
import android.view.Gravity
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView

/** Pequenos auxiliares de interface, para não repetir código nas telas. */
fun Context.dp(v: Int): Int = (v * resources.displayMetrics.density).toInt()

fun Context.sp(v: Float): Float =
    TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_SP, v, resources.displayMetrics)

fun Context.rounded(color: Int, radiusDp: Int = 12, strokeColor: Int = 0, strokeDp: Int = 0) =
    GradientDrawable().apply {
        cornerRadius = dp(radiusDp).toFloat()
        setColor(color)
        if (strokeDp > 0) setStroke(dp(strokeDp), strokeColor)
    }

fun Context.label(text: String, sizeSp: Float = 18f, color: Int = DashboardView.TEXT, bold: Boolean = false) =
    TextView(this).apply {
        this.text = text
        textSize = sizeSp
        setTextColor(color)
        if (bold) setTypeface(typeface, android.graphics.Typeface.BOLD)
    }

/** Botão grande (altura mínima de 56 dp). */
fun Context.bigButton(text: String, onClick: () -> Unit): Button = Button(this).apply {
    this.text = text
    isAllCaps = false
    textSize = 16f
    setTextColor(DashboardView.TEXT)
    background = rounded(DashboardView.HIGH)
    setPadding(dp(24), dp(8), dp(24), dp(8))
    minHeight = dp(56)
    minimumHeight = dp(56)
    gravity = Gravity.CENTER
    setOnClickListener { onClick() }
    layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT)
        .apply { rightMargin = dp(12) }
}

/** Liga ou desliga um botão mostrando o estado desabilitado. */
fun Button.enable(on: Boolean) {
    isEnabled = on
    alpha = if (on) 1f else 0.4f
}
