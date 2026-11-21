package br.com.ceratodash.ui

import android.content.Context
import android.content.DialogInterface
import android.location.LocationManager
import android.text.InputType
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import br.com.ceratodash.obd.BleAddress
import br.com.ceratodash.obd.BleScan
import br.com.ceratodash.obd.FoundDevice
import br.com.ceratodash.obd.Transport

/**
 * Busca de adaptador Bluetooth LE dentro do app, mais a opção de digitar o endereço.
 * Adaptadores só BLE não aparecem na lista de pareados do Android, por isso precisam disto.
 */
class AdapterSearch(
    private val activity: AppCompatActivity,
    private val onChosen: (address: String, name: String?, transport: Transport) -> Unit
) {
    private var scan: BleScan? = null
    private var dialog: AlertDialog? = null
    private val found = LinkedHashMap<String, FoundDevice>()
    private lateinit var status: TextView
    private lateinit var list: LinearLayout
    private var lastRender = 0L

    /** Abre a busca. A permissão de localização e o Bluetooth já devem ter sido conferidos. */
    fun open() {
        stop()
        found.clear()
        val ctx = activity
        val content = LinearLayout(ctx).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(ctx.dp(24), ctx.dp(8), ctx.dp(24), ctx.dp(8))
        }
        content.addView(
            ctx.label(
                "Ligue o adaptador no carro, com a ignição ligada, e desligue o Bluetooth do celular (o adaptador só aceita um aparelho). Toque no seu adaptador na lista.",
                Theme.LABEL, Theme.MUTED
            )
        )
        status = ctx.label("Buscando...", Theme.TEXT_SIZE, Theme.INFO)
        content.addView(status, LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply { topMargin = ctx.dp(8) })
        list = LinearLayout(ctx).apply { orientation = LinearLayout.VERTICAL }
        val scroll = ScrollView(ctx)
        scroll.addView(list)
        content.addView(scroll, LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, ctx.dp(260)).apply { topMargin = ctx.dp(8) })

        val d = AlertDialog.Builder(ctx)
            .setTitle("Buscar adaptador (Bluetooth LE)")
            .setView(content)
            .setPositiveButton("Buscar de novo", null)
            .setNeutralButton("Digitar endereço") { _, _ ->
                stop()
                openManual()
            }
            .setNegativeButton("Fechar") { _, _ -> stop() }
            .create()
        d.setOnDismissListener { stop() }
        d.show()
        // Botão que não fecha o diálogo.
        d.getButton(DialogInterface.BUTTON_POSITIVE).setOnClickListener { startScan() }
        dialog = d
        startScan()
    }

    private fun startScan() {
        stop()
        found.clear()
        render()
        status.text = "Buscando..."
        val s = BleScan(
            onFound = { dev ->
                activity.runOnUiThread {
                    found[dev.address] = dev
                    val now = System.currentTimeMillis()
                    if (now - lastRender > 400) {
                        lastRender = now
                        render()
                    }
                }
            },
            onEnd = { problem ->
                activity.runOnUiThread {
                    render()
                    status.text = when {
                        problem != null -> problem
                        found.isEmpty() -> "Nenhum dispositivo encontrado. Confira: adaptador ligado, celular desconectado dele e a localização da central ligada." + (if (!locationOn()) " A localização parece desligada." else "")
                        else -> "Busca terminada: ${found.size} dispositivos. Toque no seu adaptador."
                    }
                }
            }
        )
        scan = s
        s.start()
    }

    private fun locationOn(): Boolean {
        val lm = activity.getSystemService(Context.LOCATION_SERVICE) as? LocationManager ?: return true
        return try {
            lm.isProviderEnabled(LocationManager.GPS_PROVIDER) || lm.isProviderEnabled(LocationManager.NETWORK_PROVIDER)
        } catch (e: Exception) {
            true
        }
    }

    private fun render() {
        if (!::list.isInitialized) return
        list.removeAllViews()
        for (d in FoundDevice.sorted(found.values).take(15)) {
            val title = if (d.name.isNullOrBlank()) "Sem nome" else d.name
            val row = activity.label("$title\n${d.address}   sinal ${d.rssi}", Theme.TEXT_SIZE, if (d.name.isNullOrBlank()) Theme.MUTED else Theme.TEXT).apply {
                background = activity.rounded(Theme.HIGH)
                setPadding(activity.dp(16), activity.dp(8), activity.dp(16), activity.dp(8))
                minHeight = activity.dp(64)
                setOnClickListener {
                    stop()
                    dialog?.dismiss()
                    onChosen(d.address, d.name, Transport.BLE)
                }
            }
            list.addView(row, LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply { bottomMargin = activity.dp(6) })
        }
    }

    fun stop() {
        scan?.stop()
        scan = null
    }

    /** Pede o endereço (AA:BB:CC:DD:EE:FF) e depois o tipo de Bluetooth do adaptador. */
    fun openManual() {
        val input = EditText(activity).apply {
            hint = "AA:BB:CC:DD:EE:FF"
            inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_FLAG_CAP_CHARACTERS
            setSingleLine()
            setTextColor(Theme.TEXT)
            setHintTextColor(Theme.MUTED)
            textSize = Theme.TEXT_SIZE
        }
        val box = LinearLayout(activity).apply {
            setPadding(activity.dp(24), activity.dp(8), activity.dp(24), 0)
            addView(input, LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT))
        }
        AlertDialog.Builder(activity)
            .setTitle("Endereço do adaptador")
            .setMessage("Escreva o endereço do adaptador: seis pares de letras e números separados por dois pontos. Você encontra nas informações do aparelho Bluetooth, no celular onde ele já funciona.")
            .setView(box)
            .setPositiveButton("Continuar") { _, _ ->
                val a = BleAddress.normalize(input.text.toString())
                if (a == null) {
                    Toast.makeText(activity, "Endereço inválido. Exemplo: AA:BB:CC:DD:EE:FF", Toast.LENGTH_LONG).show()
                } else {
                    chooseTransport(a)
                }
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    private fun chooseTransport(address: String) {
        AlertDialog.Builder(activity)
            .setTitle("Tipo de Bluetooth do adaptador")
            .setItems(arrayOf("Bluetooth LE (adaptadores mais novos)", "Bluetooth clássico")) { _, i ->
                onChosen(address, null, if (i == 0) Transport.BLE else Transport.CLASSIC)
            }
            .show()
    }
}
