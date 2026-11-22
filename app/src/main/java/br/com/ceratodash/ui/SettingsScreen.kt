package br.com.ceratodash.ui

import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.widget.Button
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import br.com.ceratodash.data.AlertLimits
import br.com.ceratodash.data.AppState
import br.com.ceratodash.data.DrivingStyle
import br.com.ceratodash.data.Prefs
import br.com.ceratodash.obd.Fuel
import br.com.ceratodash.obd.ObdManager
import br.com.ceratodash.service.ObdService

/** Ajustes: adaptador, combustível, limites de alerta e início com o sistema. */
class SettingsScreen(
    private val activity: AppCompatActivity,
    private val onPickAdapter: () -> Unit,
    private val onLimitsChanged: (AlertLimits) -> Unit,
    private val onPickPhoto: () -> Unit,
    /** Estilo de condução, limite de velocidade ou foto mudaram: a tela principal precisa reaplicar. */
    private val onPrefsChanged: () -> Unit,
    private val onTestVoice: () -> Unit,
    private val onToggleOverlay: () -> Unit,
    private val onSearchAdapter: () -> Unit,
    private val onManualAddress: () -> Unit,
    private val onShowLog: () -> Unit,
    private val onShowUsb: () -> Unit,
    private val onShowDevice: () -> Unit,
    private val onTestGps: () -> Unit
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
        val nome = Prefs.adapterName(activity) ?: name ?: "Adaptador"
        return "$nome  ($addr), ${Prefs.transport(activity).label}"
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
                activity.bigButton("Buscar adaptador (BLE)") { onSearchAdapter() },
                activity.bigButton("Digitar endereço") { onManualAddress() },
                activity.bigButton("Escolher entre pareados") { onPickAdapter() }
            )
        )
        adapter.addView(
            row(
                activity.bigButton("Reconectar") { reconnect() },
                activity.bigButton("Desconectar") { ObdService.stop(activity) },
                activity.bigButton("Ver registro da conexão") { onShowLog() }
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

        val va = Prefs.voiceAlerts(activity)
        val vc = Prefs.voiceCoach(activity)
        val voice = card("Voz")
        voice.addView(activity.label("Fala os alertas e as dicas de condução pelo mecanismo de voz do Android. Precisa de uma voz em português instalada na central. Desligada por padrão.", Theme.LABEL, Theme.MUTED))
        voice.addView(
            row(
                activity.bigButton(if (va) "Voz nos alertas: ligada" else "Voz nos alertas: desligada") {
                    Prefs.setVoiceAlerts(activity, !va)
                    refresh()
                }.also { it.stateStyle(activity, va) },
                activity.bigButton(if (vc) "Voz nas dicas: ligada" else "Voz nas dicas: desligada") {
                    Prefs.setVoiceCoach(activity, !vc)
                    refresh()
                }.also { it.stateStyle(activity, vc) }
            )
        )
        voice.addView(row(activity.bigButton("Testar voz") { onTestVoice() }))

        val style = Prefs.coachStyle(activity)
        val coach = card("Condução")
        coach.addView(activity.label("Estilo do treinador de condução. Os limites são valores gerais de direção econômica, não da Kia.", Theme.LABEL, Theme.MUTED))
        val styleButtons = DrivingStyle.values().map { s ->
            activity.bigButton(s.label) {
                Prefs.setCoachStyle(activity, s)
                onPrefsChanged()
                refresh()
            }.also { it.stateStyle(activity, s == style) }
        }
        coach.addView(row(*styleButtons.toTypedArray()))
        coach.addView(stepper("Limite de velocidade", "${Prefs.speedMax(activity)} km/h", { changeSpeed(-5) }, { changeSpeed(5) }))
        coach.addView(activity.label("O app não tem GPS: o limite de velocidade é o que você escolher aqui, não o da via.", Theme.LABEL, Theme.MUTED))

        val photo = card("Foto do carro")
        photo.addView(activity.label(if (CarPhoto.hasUserPhoto(activity)) "Usando a foto que você escolheu." else "Usando a foto padrão do app.", Theme.TEXT_SIZE, Theme.TEXT))
        photo.addView(
            row(
                activity.bigButton("Escolher foto") { onPickPhoto() },
                activity.bigButton("Voltar à foto padrão") {
                    CarPhoto.clear(activity)
                    onPrefsChanged()
                    refresh()
                }
            )
        )
        val left = Prefs.carFacingLeft(activity)
        photo.addView(
            row(
                activity.bigButton(if (left) "Frente do carro na foto: esquerda" else "Frente do carro na foto: direita") {
                    Prefs.setCarFacingLeft(activity, !left)
                    onPrefsChanged()
                    refresh()
                }
            )
        )
        photo.addView(activity.label("Use um PNG ou JPG em vista lateral. Os pontos de motor, bateria e escapamento seguem a posição da frente do carro.", Theme.LABEL, Theme.MUTED))
        photo.addView(activity.label("Foto padrão: Kia Cerato 1.6 SX 2010, de order_242 (Wikimedia Commons), licença CC BY-SA 2.0.", Theme.LABEL, Theme.MUTED))

        val ov = Prefs.overlay(activity)
        val maps = card("Mapas")
        maps.addView(activity.label("Mostra uma faixa pequena com rotação, velocidade e água sobre o Maps ou o Waze. Precisa da permissão Sobrepor outros apps, que esta central pode não ter. Desligada por padrão.", Theme.LABEL, Theme.MUTED))
        maps.addView(
            row(
                activity.bigButton(if (ov) "Faixa sobre o mapa: ligada" else "Faixa sobre o mapa: desligada") { onToggleOverlay() }
                    .also { it.stateStyle(activity, ov) }
            )
        )
        AppState.overlayProblem?.let { maps.addView(activity.label(it, Theme.LABEL, Theme.DANGER)) }

        val usb = card("Diagnóstico da central")
        usb.addView(activity.label("Mostra o que a central tem (Wi-Fi, GPS, Bluetooth LE, USB, Play Store, Maps, apps de espelhamento) e o que isso permite. Ver USB lista o que a porta enxerga com o celular ligado no cabo.", Theme.LABEL, Theme.MUTED))
        usb.addView(row(activity.bigButton("Ver diagnóstico da central") { onShowDevice() }, activity.bigButton("Testar GPS") { onTestGps() }, activity.bigButton("Ver USB") { onShowUsb() }))

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

    private fun changeSpeed(delta: Int) {
        Prefs.setSpeedMax(activity, (Prefs.speedMax(activity) + delta).coerceIn(60, 180))
        onPrefsChanged()
        refresh()
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
