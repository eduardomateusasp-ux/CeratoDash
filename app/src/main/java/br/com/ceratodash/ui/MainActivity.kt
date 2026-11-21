package br.com.ceratodash.ui

import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.view.WindowManager
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import br.com.ceratodash.R
import br.com.ceratodash.data.AlertLimits
import br.com.ceratodash.data.Alerts
import br.com.ceratodash.data.Prefs
import br.com.ceratodash.data.TripLive
import br.com.ceratodash.obd.ConnState
import br.com.ceratodash.obd.ObdManager
import br.com.ceratodash.obd.ObdStatus
import br.com.ceratodash.service.ObdService
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {

    private companion object {
        const val PAINEL = 0
        const val TELEMETRIA = 1
        const val DIAGNOSTICO = 2
        const val VIAGENS = 3
        const val MAPAS = 4
        const val AJUSTES = 5
        const val FADE_MS = 150L
    }

    private class NavItem(val name: String, val icon: Int)

    private val nav = listOf(
        NavItem("Painel", R.drawable.ic_nav_painel),
        NavItem("Telemetria", R.drawable.ic_nav_telemetria),
        NavItem("Diagnóstico", R.drawable.ic_nav_diagnostico),
        NavItem("Viagens", R.drawable.ic_nav_viagens),
        NavItem("Mapas", R.drawable.ic_nav_mapas),
        NavItem("Ajustes", R.drawable.ic_nav_ajustes)
    )

    private lateinit var dash: DashboardView
    private lateinit var telemetry: TelemetryScreen
    private lateinit var trips: TripsScreen
    private lateinit var settings: SettingsScreen
    private lateinit var banner: AlertBanner
    private lateinit var status: StatusIndicator
    private val screens = ArrayList<View>()
    private val railItems = ArrayList<TextView>()
    private var current = PAINEL
    private var limits = AlertLimits()

    private val match = LinearLayout.LayoutParams.MATCH_PARENT
    private val wrap = LinearLayout.LayoutParams.WRAP_CONTENT

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        ObdManager.fuel = Prefs.fuel(this)
        limits = Prefs.limits(this)
        setContentView(buildUi())
        dash.limits = limits
        show(PAINEL, animate = false)

        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    ObdManager.telemetry.collect {
                        dash.update(it)
                        banner.show(Alerts.evaluate(it, limits))
                        if (current == TELEMETRIA) telemetry.refresh()
                    }
                }
                launch { ObdManager.status.collect { status.render(it) } }
                launch { TripLive.current.collect { trips.setLive(it) } }
                launch { TripLive.version.collect { trips.reload() } }
            }
        }
        // O serviço é o dono da conexão. Se ele ainda não está rodando, inicia.
        if (!ObdManager.isRunning) {
            if (Prefs.adapter(this) == null) {
                ObdManager.status.value =
                    ObdStatus(ConnState.DESCONECTADO, "abra Ajustes e escolha o adaptador")
            } else {
                ObdService.start(this)
            }
        }
    }

    private fun buildUi(): View {
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            setBackgroundColor(Theme.BG)
        }

        // Navegação lateral: ícone grande com rótulo, alvo mínimo de 64 dp.
        val rail = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(Theme.SURFACE)
        }
        for ((i, n) in nav.withIndex()) {
            val item = TextView(this).apply {
                text = n.name
                textSize = Theme.MIN_SIZE
                typeface = Theme.bodyMedium
                gravity = Gravity.CENTER
                minHeight = dp(64)
                compoundDrawablePadding = dp(2)
                setPadding(dp(4), dp(8), dp(4), dp(8))
                setOnClickListener { show(i) }
            }
            railItems.add(item)
            rail.addView(item, LinearLayout.LayoutParams(dp(104), 0, 1f))
        }
        root.addView(rail, LinearLayout.LayoutParams(wrap, match))

        // Conteúdo: status e faixa de alerta em cima, a tela escolhida embaixo.
        val content = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        status = StatusIndicator(this).apply { setPadding(dp(24), dp(10), dp(24), dp(10)) }
        content.addView(status, LinearLayout.LayoutParams(match, wrap))
        banner = AlertBanner(this)
        content.addView(banner, LinearLayout.LayoutParams(match, wrap))

        val frame = FrameLayout(this)
        dash = DashboardView(this)
        telemetry = TelemetryScreen(this)
        trips = TripsScreen(this)
        settings = SettingsScreen(this, { pickAdapter() }, { l ->
            limits = l
            dash.limits = l
        })
        screens.add(dash)
        screens.add(telemetry)
        screens.add(DiagnosticScreen(this))
        screens.add(trips)
        screens.add(MapsScreen(this) { pkg, nome -> openApp(pkg, nome) })
        screens.add(settings)
        for (s in screens) frame.addView(s, FrameLayout.LayoutParams(match, match))
        content.addView(frame, LinearLayout.LayoutParams(match, 0, 1f))
        root.addView(content, LinearLayout.LayoutParams(0, match, 1f))
        return root
    }

    private fun show(index: Int, animate: Boolean = true) {
        val changed = index != current
        current = index
        for ((i, s) in screens.withIndex()) {
            if (i == index) {
                s.visibility = View.VISIBLE
                if (animate && changed) {
                    // Transição curta (150 ms), a única animação fora dos ponteiros.
                    s.alpha = 0f
                    s.animate().alpha(1f).setDuration(FADE_MS).start()
                } else {
                    s.alpha = 1f
                }
            } else {
                s.animate().cancel()
                s.visibility = View.GONE
            }
        }
        for ((i, item) in railItems.withIndex()) {
            val on = i == index
            val color = if (on) Theme.INFO else Theme.MUTED
            item.setTextColor(color)
            item.setBackgroundColor(if (on) Theme.HIGH else Theme.SURFACE)
            val icon = ContextCompat.getDrawable(this, nav[i].icon)?.mutate()
            icon?.setTint(color)
            item.setCompoundDrawablesWithIntrinsicBounds(null, icon, null, null)
        }
        if (index == TELEMETRIA) telemetry.refresh()
        if (index == VIAGENS) trips.reload()
        if (index == AJUSTES) settings.refresh()
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
                settings.refresh()
            }
            .show()
    }

    private fun openApp(pkg: String, nome: String) {
        val intent = packageManager.getLaunchIntentForPackage(pkg)
        if (intent == null) toast("$nome não está instalado nesta central.") else startActivity(intent)
    }

    private fun toast(msg: String) = Toast.makeText(this, msg, Toast.LENGTH_LONG).show()
}
