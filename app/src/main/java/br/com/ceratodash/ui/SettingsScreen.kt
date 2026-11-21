package br.com.ceratodash.ui

import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.widget.Button
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import br.com.ceratodash.data.AlertLimits
import br.com.ceratodash.data.Prefs
import br.com.ceratodash.obd.Fuel
import br.com.ceratodash.obd.ObdManager
import br.com.ceratodash.service.ObdService

/** Ajustes: adaptador, combustível, limites de alerta e início com o sistema. */
class SettingsScreen(
    private val activity: AppCompatActivity,
    private val onPickAdapter: () -> Unit,
    private val onLimitsChanged: (AlertLimits) -> Unit
) : ScrollView(activity) {

    private val box = LinearLayout(activity).apply { orientation = LinearLayout.VERTICAL }

    init {
        setPadding(activity.dp(24), activity.dp(16), activity.dp(24), activity.dp(16))
        clipToPadding = false
        addView(box)
        refresh()
    }

    private fun card(title: String): LinearLayout {
        val c = LinearLayout(activity).apply {
            orientation = LinearLayout.VERTICAL
            background = activity.rounded(Theme.SURFACE)
            setPadding(activity.dp(16), activity.dp(12), activity.dp(16), activity.dp(12))
        }
        c.addView(activity.label(title, Theme.TEXT_SIZE, Theme.INFO, true))
        box.addView(c, LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply { bottomMargin = activity.dp(12) })
        return c
    }

    private fun row(vararg views: android.view.View): LinearLayout {
        val r = LinearLayout(activity).apply { orientation = LinearLayout.HORIZONTAL; gravity = android.view.Gravity.CENTER_VERTICAL }
        for (v in views) r.addView(v)
        r.layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply { topMargin = activity.dp(8) }
        return r
    }

    @SuppressLint("MissingPermission")
    private fun adapterName(): String {
        val addr = Prefs.adapter(activity) ?: return "Nenhum adaptador escolhido."
        val name = try {
            BluetoothAdapter.getDefaultAdapter()?.getRemoteDevice(addr)?.name
        } catch (e: Exception) {
            null
        }
        return "${name ?: "Adaptador"}  ($addr)"
    }

    /** Refaz a tela com os valores atuais. */
    fun refresh() {
        box.removeAllViews()
        box.addView(activity.label("Ajustes", 24f, Theme.TEXT, true))
        box.addView(spacer())

        val adapter = card("Adaptador OBDII")
        adapter.addView(activity.label(adapterName(), Theme.TEXT_SIZE, Theme.TEXT))
        adapter.addView(
            row(
                activity.bigButton("Escolher adaptador") { onPickAdapter() },
                activity.bigButton("Reconectar") { reconnect() },
                activity.bigButton("Desconectar") { ObdService.stop(activity) }
            )
        )

        val fuel = card("Combustível")
        fuel.addView(activity.label("Usado para estimar o consumo a partir do fluxo de ar.", Theme.LABEL, Theme.MUTED))
        val cur = ObdManager.fuel
        val buttons = Fuel.values().map { f ->
            activity.bigButton(f.label) {
                ObdManager.fuel = f
                Prefs.setFuel(activity, f)
                refresh()
            }.also { it.stateStyle(activity, f == cur) }
        }
        fuel.addView(row(*buttons.toTypedArray()))

        val lim = Prefs.limits(activity)
        val alerts = card("Alertas")
        alerts.addView(activity.label("Os alertas aparecem numa faixa no topo, com ícone e texto.", Theme.LABEL, Theme.MUTED))
        alerts.addView(
            stepper("Limite de rotação", "${lim.rpmMax} rpm", { change(rpm = -500) }, { change(rpm = 500) })
        )
        alerts.addView(
            stepper(
                "Temperatura crítica da água", "${lim.waterDanger} °C",
                { change(water = -1) }, { change(water = 1) }
            )
        )
        alerts.addView(activity.label("Atenção a partir de ${lim.waterWarn} °C. Bateria: abaixo de 11,5 V ou acima de 15,2 V.", Theme.LABEL, Theme.MUTED))

        val sys = card("Sistema")
        val on = Prefs.autostart(activity)
        sys.addView(activity.label("Iniciar a leitura junto com a central. Só o serviço sobe; abra o app para ver o painel.", Theme.LABEL, Theme.MUTED))
        sys.addView(
            row(
                activity.bigButton(if (on) "Iniciar com o sistema: ligado" else "Iniciar com o sistema: desligado") {
                    Prefs.setAutostart(activity, !on)
                    refresh()
                }.also { it.stateStyle(activity, on) }
            )
        )
    }

    private fun spacer() = android.view.View(activity).apply {
        layoutParams = LinearLayout.LayoutParams(1, activity.dp(8))
    }

    private fun reconnect() {
        if (Prefs.adapter(activity) != null) ObdService.start(activity)
    }

    private fun change(rpm: Int = 0, water: Int = 0) {
        if (rpm != 0) Prefs.setRpmMax(activity, (Prefs.limits(activity).rpmMax + rpm).coerceIn(4000, 8000))
        if (water != 0) Prefs.setWaterDanger(activity, (Prefs.limits(activity).waterDanger + water).coerceIn(100, 120))
        onLimitsChanged(Prefs.limits(activity))
        refresh()
    }

    private fun stepper(title: String, value: String, minus: () -> Unit, plus: () -> Unit): LinearLayout {
        val name: TextView = activity.label(title, Theme.TEXT_SIZE, Theme.TEXT)
        val v: TextView = activity.number(value, 28f, Theme.TEXT).apply { gravity = android.view.Gravity.CENTER; minWidth = activity.dp(120) }
        val minusB: Button = activity.bigButton("-") { minus() }
        val plusB: Button = activity.bigButton("+") { plus() }
        val r = row(name.also { it.layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f) }, minusB, v, plusB)
        return r
    }
}
