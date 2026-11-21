package br.com.ceratodash.ui

import android.widget.Button
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import br.com.ceratodash.data.DtcTable
import br.com.ceratodash.obd.DiagResult
import br.com.ceratodash.obd.ObdManager
import kotlinx.coroutines.launch
import kotlin.coroutines.cancellation.CancellationException

/** Diagnóstico: ler códigos de falha (modo 03), ver a luz de injeção e limpar (modo 04) com confirmação. */
class DiagnosticScreen(private val activity: AppCompatActivity) : LinearLayout(activity) {

    private val readBtn: Button
    private val clearBtn: Button
    private val info = activity.label("Toque em Ler códigos para consultar o carro.", 18f, Theme.MUTED)
    private val mil = activity.label("", 20f, Theme.TEXT, true)
    private val list = LinearLayout(activity).apply { orientation = VERTICAL }
    private var busy = false

    init {
        orientation = VERTICAL
        setPadding(activity.dp(24), activity.dp(16), activity.dp(24), activity.dp(16))
        addView(activity.label("Diagnóstico", 24f, Theme.TEXT, true))

        val row = LinearLayout(activity).apply { orientation = HORIZONTAL }
        readBtn = activity.bigButton("Ler códigos") { read() }
        clearBtn = activity.bigButton("Limpar códigos") { confirmClear() }
        row.addView(readBtn)
        row.addView(clearBtn)
        addView(row, LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT).apply { topMargin = activity.dp(12) })

        addView(mil, LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT).apply { topMargin = activity.dp(16) })
        addView(info, LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT).apply { topMargin = activity.dp(8) })
        val scroll = ScrollView(activity)
        scroll.addView(list)
        addView(scroll, LayoutParams(LayoutParams.MATCH_PARENT, 0, 1f).apply { topMargin = activity.dp(8) })
    }

    private fun setBusy(on: Boolean) {
        busy = on
        readBtn.enable(!on)
        clearBtn.enable(!on)
    }

    private fun read() {
        if (busy) return
        setBusy(true)
        info.setTextColor(Theme.MUTED)
        info.text = "Consultando o carro..."
        activity.lifecycleScope.launch {
            try {
                render(ObdManager.readDiagnostics())
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                showError(e.message ?: "Falha ao ler os códigos.")
            } finally {
                setBusy(false)
            }
        }
    }

    private fun confirmClear() {
        if (busy) return
        AlertDialog.Builder(activity)
            .setTitle("Apagar os códigos de falha?")
            .setMessage(
                "Isso apaga os códigos armazenados e apaga a luz de injeção. " +
                    "Também zera os testes de emissões do carro, que levam alguns ciclos de condução para completar.\n\n" +
                    "Anote os códigos antes. Faça com a ignição ligada e, de preferência, com o motor desligado."
            )
            .setPositiveButton("Apagar códigos") { _, _ -> clear() }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    private fun clear() {
        if (busy) return
        setBusy(true)
        info.setTextColor(Theme.MUTED)
        info.text = "Apagando os códigos..."
        activity.lifecycleScope.launch {
            try {
                ObdManager.clearDiagnostics()
                render(ObdManager.readDiagnostics())
                info.text = "Comando de limpeza aceito pelo carro. Resultado da nova leitura:"
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                showError(e.message ?: "Falha ao apagar os códigos.")
            } finally {
                setBusy(false)
            }
        }
    }

    private fun showError(msg: String) {
        mil.text = ""
        list.removeAllViews()
        info.setTextColor(Theme.DANGER)
        info.text = msg
    }

    private fun render(r: DiagResult) {
        info.setTextColor(Theme.MUTED)
        info.text = ""
        when (r.mil) {
            null -> {
                mil.text = "Luz de injeção: o carro não informou"
                mil.setTextColor(Theme.MUTED)
            }
            true -> {
                mil.text = "Luz de injeção: acesa (${r.milCount ?: 0} códigos)"
                mil.setTextColor(Theme.WARN)
            }
            false -> {
                mil.text = "Luz de injeção: apagada"
                mil.setTextColor(Theme.OK)
            }
        }
        list.removeAllViews()
        if (r.codes.isEmpty()) {
            list.addView(activity.label("Nenhum código de falha armazenado.", 18f, Theme.OK))
            return
        }
        for (code in r.codes) {
            val desc = DtcTable.describe(code) ?: "sem descrição na tabela genérica (pode ser código do fabricante)"
            val card = LinearLayout(activity).apply {
                orientation = VERTICAL
                background = activity.rounded(Theme.SURFACE, 16)
                setPadding(activity.dp(16), activity.dp(12), activity.dp(16), activity.dp(12))
            }
            card.addView(activity.label(code, 24f, Theme.WARN, true))
            card.addView(activity.label(desc, 18f, Theme.TEXT))
            list.addView(card, LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT).apply { bottomMargin = activity.dp(8) })
        }
    }
}
