package br.com.ceratodash

import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.view.WindowManager
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {

    private lateinit var dash: DashboardView
    private lateinit var statusText: TextView
    private lateinit var statusDot: View

    private val match = LinearLayout.LayoutParams.MATCH_PARENT
    private val wrap = LinearLayout.LayoutParams.WRAP_CONTENT

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        ObdManager.fuel = Prefs.fuel(this)
        setContentView(buildUi())

        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch { ObdManager.telemetry.collect { dash.update(it) } }
                launch { ObdManager.status.collect { renderStatus(it) } }
            }
        }
        // O serviço é o dono da conexão. Se ele ainda não está rodando, inicia.
        if (!ObdManager.isRunning) {
            if (Prefs.adapter(this) == null) {
                ObdManager.status.value =
                    ObdStatus(ConnState.DESCONECTADO, "toque em Ajustes e escolha o adaptador")
            } else {
                ObdService.start(this)
            }
        }
    }

    private fun buildUi(): View {
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(DashboardView.BG)
        }
        val bar = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(dp(16), dp(8), dp(16), dp(8))
        }
        statusDot = View(this).apply { background = dot(DashboardView.MUTED) }
        bar.addView(statusDot, LinearLayout.LayoutParams(dp(12), dp(12)).apply { rightMargin = dp(10) })
        statusText = TextView(this).apply {
            setTextColor(DashboardView.TEXT)
            textSize = 16f
        }
        bar.addView(statusText, LinearLayout.LayoutParams(0, wrap, 1f))
        bar.addView(button("Ajustes") { showSettings() })
        bar.addView(button("Google Maps") { openApp("com.google.android.apps.maps", "Google Maps") })
        bar.addView(button("Waze") { openApp("com.waze", "Waze") })
        root.addView(bar, LinearLayout.LayoutParams(match, wrap))

        dash = DashboardView(this)
        root.addView(dash, LinearLayout.LayoutParams(match, 0, 1f))
        return root
    }

    private fun dp(v: Int) = (v * resources.displayMetrics.density).toInt()

    private fun dot(color: Int) = GradientDrawable().apply {
        shape = GradientDrawable.OVAL
        setColor(color)
    }

    private fun button(label: String, onClick: () -> Unit): Button = Button(this).apply {
        text = label
        isAllCaps = false
        setTextColor(DashboardView.TEXT)
        textSize = 14f
        background = GradientDrawable().apply {
            cornerRadius = dp(12).toFloat()
            setColor(DashboardView.HIGH)
        }
        setPadding(dp(18), dp(8), dp(18), dp(8))
        minHeight = 0
        minimumHeight = 0
        setOnClickListener { onClick() }
        layoutParams = LinearLayout.LayoutParams(wrap, dp(44)).apply { leftMargin = dp(8) }
    }

    private fun renderStatus(s: ObdStatus) {
        val color = when (s.state) {
            ConnState.CONECTADO -> DashboardView.OK
            ConnState.CONECTANDO, ConnState.INICIALIZANDO -> DashboardView.WARN
            ConnState.ERRO -> DashboardView.DANGER
            ConnState.DESCONECTADO -> DashboardView.MUTED
        }
        (statusDot.background as GradientDrawable).setColor(color)
        statusText.text = if (s.message.isBlank()) s.state.label else "${s.state.label}: ${s.message}"
    }

    private fun showSettings() {
        val auto = if (Prefs.autostart(this)) "ligado" else "desligado"
        val items = arrayOf(
            "Escolher adaptador OBDII",
            "Combustível: ${ObdManager.fuel.label} (tocar para alternar)",
            "Iniciar com o sistema: $auto (tocar para alternar)",
            "Reconectar",
            "Desconectar"
        )
        AlertDialog.Builder(this)
            .setTitle("Ajustes")
            .setItems(items) { _, i ->
                when (i) {
                    0 -> pickAdapter()
                    1 -> toggleFuel()
                    2 -> toggleAutostart()
                    3 -> reconnect()
                    4 -> ObdService.stop(this)
                }
            }
            .show()
    }

    private fun toggleFuel() {
        val next = if (ObdManager.fuel == Fuel.GASOLINA) Fuel.ETANOL else Fuel.GASOLINA
        ObdManager.fuel = next
        Prefs.setFuel(this, next)
        toast("Combustível: ${next.label}")
    }

    private fun toggleAutostart() {
        val on = !Prefs.autostart(this)
        Prefs.setAutostart(this, on)
        toast(if (on) "A leitura vai iniciar junto com a central." else "Início com o sistema desligado.")
    }

    private fun reconnect() {
        if (Prefs.adapter(this) == null) toast("Escolha o adaptador primeiro.") else ObdService.start(this)
    }

    @SuppressLint("MissingPermission")
    private fun bonded(): List<BluetoothDevice>? {
        val adapter = BluetoothAdapter.getDefaultAdapter()
        if (adapter == null) {
            toast("Esta central não tem Bluetooth.")
            return null
        }
        if (!adapter.isEnabled) {
            toast("Ligue o Bluetooth da central.")
            return null
        }
        return try {
            adapter.bondedDevices.toList()
        } catch (e: SecurityException) {
            toast("Sem permissão de Bluetooth.")
            null
        }
    }

    @SuppressLint("MissingPermission")
    private fun pickAdapter() {
        val devices = bonded() ?: return
        if (devices.isEmpty()) {
            toast("Nenhum aparelho pareado. Pareie o adaptador OBDII nos ajustes de Bluetooth da central.")
            return
        }
        val names = devices.map { "${it.name ?: "Sem nome"}\n${it.address}" }.toTypedArray()
        AlertDialog.Builder(this)
            .setTitle("Escolha o adaptador OBDII")
            .setItems(names) { _, i ->
                Prefs.setAdapter(this, devices[i].address)
                ObdService.start(this)
            }
            .show()
    }

    private fun openApp(pkg: String, nome: String) {
        val intent = packageManager.getLaunchIntentForPackage(pkg)
        if (intent == null) toast("$nome não está instalado nesta central.") else startActivity(intent)
    }

    private fun toast(msg: String) = Toast.makeText(this, msg, Toast.LENGTH_LONG).show()
}
