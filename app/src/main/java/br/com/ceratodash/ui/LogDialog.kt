package br.com.ceratodash.ui

import android.content.DialogInterface
import android.graphics.Typeface
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import br.com.ceratodash.data.ConnLog

/** Mostra o registro das etapas da conexão (BLE e ELM327), para diagnosticar falhas. */
object LogDialog {
    fun show(activity: AppCompatActivity) {
        val text = TextView(activity).apply {
            setTextColor(Theme.TEXT)
            textSize = Theme.MIN_SIZE
            typeface = Typeface.MONOSPACE
            setTextIsSelectable(true)
        }
        val scroll = ScrollView(activity)
        scroll.addView(text)
        val box = LinearLayout(activity).apply {
            setPadding(activity.dp(16), activity.dp(8), activity.dp(16), activity.dp(8))
            addView(scroll, LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, activity.dp(360)))
        }
        fun refresh() {
            text.text = ConnLog.text()
            scroll.post { scroll.fullScroll(ScrollView.FOCUS_DOWN) }
        }
        val d = AlertDialog.Builder(activity)
            .setTitle("Registro da conexão")
            .setView(box)
            .setPositiveButton("Atualizar", null)
            .setNeutralButton("Limpar", null)
            .setNegativeButton("Fechar", null)
            .create()
        d.show()
        // Atualizar e Limpar não fecham o diálogo.
        d.getButton(DialogInterface.BUTTON_POSITIVE).setOnClickListener { refresh() }
        d.getButton(DialogInterface.BUTTON_NEUTRAL).setOnClickListener {
            ConnLog.clear()
            refresh()
        }
        refresh()
    }
}
