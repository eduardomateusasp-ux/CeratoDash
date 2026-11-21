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
    private val prefs by lazy { getSharedPreferences("ceratodash", MODE_PRIVATE) }

    private val match = LinearLayout.LayoutParams.MATCH_PARENT
    private val wrap = LinearLayout.LayoutParams.WRAP_CONTENT

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        ObdManager.fuel = Fuel.values().getOrElse(prefs.getInt("fuel", 0)) { Fuel.GASOLINA }
        setContentView(buildUi())

        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch { ObdManager.telemetry.collect { dash.update(it) } }
                launch { ObdManager.status.collect { renderStatus(it) } }
            }
        }
        if (!ObdManager.isRunning) connectSaved()
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
        val (label, color) = when (s.state) {
            ConnState.CONECTADO -> "Conectado" to DashboardView.OK
            ConnState.CONECTANDO -> "Conectando" to DashboardView.WARN
            ConnState.INICIALIZANDO -> "Iniciando o adaptador" to DashboardView.WARN
            ConnState.ERRO -> "Sem conexão" to DashboardView.DANGER
            ConnState.DESCONECTADO -> "Desconectado" to DashboardView.MUTED
        }
        (statusDot.background as GradientDrawable).setColor(color)
        statusText.text = if (s.message.isBlank()) label else "$label: ${s.message}"
    }

    private fun showSettings() {
        val items = arrayOf(
            "Escolher adaptador OBDII",
            "Combustível: ${ObdManager.fuel.label} (tocar para alternar)",
            "Reconectar",
            "Desconectar"
        )
        AlertDialog.Builder(this)
            .setTitle("Ajustes")
            .setItems(items) { _, i ->
                when (i) {
                    0 -> pickAdapter()
                    1 -> toggleFuel()
                    2 -> connectSaved()
                    3 -> ObdManager.stop()
                }
            }
            .show()
    }

    private fun toggleFuel() {
        val next = if (ObdManager.fuel == Fuel.GASOLINA) Fuel.ETANOL else Fuel.GASOLINA
        ObdManager.fuel = next
        prefs.edit().putInt("fuel", next.ordinal).apply()
        toast("Combustível: ${next.label}")
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
                prefs.edit().putString("addr", devices[i].address).apply()
                ObdManager.start(devices[i])
            }
            .show()
    }

    private fun connectSaved() {
        val addr = prefs.getString("addr", null)
        if (addr == null) {
            ObdManager.status.value = ObdStatus(ConnState.DESCONECTADO, "toque em Ajustes e escolha o adaptador")
            return
        }
        val paired = bonded()
        if (paired == null) {
            ObdManager.status.value = ObdStatus(ConnState.ERRO, "Bluetooth desligado ou indisponível")
            return
        }
        val device = paired.firstOrNull { it.address == addr }
        if (device == null) {
            ObdManager.status.value = ObdStatus(ConnState.ERRO, "o adaptador salvo não está pareado")
            return
        }
        ObdManager.start(device)
    }

    private fun openApp(pkg: String, nome: String) {
        val intent = packageManager.getLaunchIntentForPackage(pkg)
        if (intent == null) toast("$nome não está instalado nesta central.") else startActivity(intent)
    }

    private fun toast(msg: String) = Toast.makeText(this, msg, Toast.LENGTH_LONG).show()
}
