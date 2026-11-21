package br.com.ceratodash.ui

import android.content.Context
import android.graphics.Typeface
import androidx.core.content.ContextCompat
import androidx.core.content.res.ResourcesCompat
import br.com.ceratodash.R

/**
 * Tema do app: cores lidas de colors.xml e fontes embutidas no APK (res/font).
 * Rajdhani (SemiBold e Bold) para números e rótulos do painel; Inter (Regular e Medium) para texto.
 * Iniciado uma vez em CeratoApp.onCreate, antes de qualquer tela ou serviço.
 */
object Theme {
    var BG = 0; private set
    var SURFACE = 0; private set
    var HIGH = 0; private set
    var PRESSED = 0; private set
    var ACCENT = 0; private set
    var INFO = 0; private set
    var OK = 0; private set
    var WARN = 0; private set
    var DANGER = 0; private set
    var TEXT = 0; private set
    var MUTED = 0; private set

    var numSemi: Typeface = Typeface.DEFAULT_BOLD; private set
    var numBold: Typeface = Typeface.DEFAULT_BOLD; private set
    var body: Typeface = Typeface.DEFAULT; private set
    var bodyMedium: Typeface = Typeface.DEFAULT; private set

    // Escala de texto em sp (nada abaixo de 14 sp).
    const val VALUE_MAIN = 96f
    const val VALUE_SECONDARY = 40f
    const val LABEL = 16f
    const val TEXT_SIZE = 18f
    const val MIN_SIZE = 14f

    fun init(c: Context) {
        BG = ContextCompat.getColor(c, R.color.bg)
        SURFACE = ContextCompat.getColor(c, R.color.surface)
        HIGH = ContextCompat.getColor(c, R.color.surface_high)
        PRESSED = ContextCompat.getColor(c, R.color.surface_pressed)
        ACCENT = ContextCompat.getColor(c, R.color.accent)
        INFO = ContextCompat.getColor(c, R.color.info)
        OK = ContextCompat.getColor(c, R.color.ok)
        WARN = ContextCompat.getColor(c, R.color.warn)
        DANGER = ContextCompat.getColor(c, R.color.danger)
        TEXT = ContextCompat.getColor(c, R.color.text)
        MUTED = ContextCompat.getColor(c, R.color.text_muted)
        numSemi = font(c, R.font.rajdhani_semibold, Typeface.DEFAULT_BOLD)
        numBold = font(c, R.font.rajdhani_bold, Typeface.DEFAULT_BOLD)
        body = font(c, R.font.inter_regular, Typeface.DEFAULT)
        bodyMedium = font(c, R.font.inter_medium, Typeface.DEFAULT)
    }

    private fun font(c: Context, id: Int, fallback: Typeface): Typeface =
        try {
            ResourcesCompat.getFont(c, id) ?: fallback
        } catch (e: Exception) {
            fallback
        }
}
