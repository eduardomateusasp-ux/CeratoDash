package br.com.ceratodash.ui

import android.widget.LinearLayout
import android.widget.ScrollView
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import br.com.ceratodash.data.Trip
import br.com.ceratodash.data.TripDb
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/** Viagens: lista do histórico (SQLite local), viagem em andamento e apagar. */
class TripsScreen(private val activity: AppCompatActivity) : LinearLayout(activity) {

    private val list = LinearLayout(activity).apply { orientation = VERTICAL }
    private val liveBox = LinearLayout(activity).apply { orientation = VERTICAL }
    private val clearAll = activity.bigButton("Apagar todas") { confirmDeleteAll() }
    private val dateFmt = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale("pt", "BR"))
    private var history: List<Trip> = emptyList()

    init {
        orientation = VERTICAL
        setPadding(activity.dp(24), activity.dp(16), activity.dp(24), activity.dp(16))
        val head = LinearLayout(activity).apply { orientation = HORIZONTAL }
        head.addView(activity.label("Viagens", 24f, Theme.TEXT, true), LayoutParams(0, LayoutParams.WRAP_CONTENT, 1f))
        clearAll.layoutParams = LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT)
        head.addView(clearAll)
        addView(head)
        addView(liveBox, LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT).apply { topMargin = activity.dp(8) })
        val scroll = ScrollView(activity)
        scroll.addView(list)
        addView(scroll, LayoutParams(LayoutParams.MATCH_PARENT, 0, 1f).apply { topMargin = activity.dp(8) })
    }

    /** Recarrega o histórico do banco. */
    fun reload() {
        activity.lifecycleScope.launch {
            history = try {
                withContext(Dispatchers.IO) { TripDb.get(activity).list() }
            } catch (e: Exception) {
                emptyList()
            }
            render()
        }
    }

    /** Atualiza o cartão da viagem em andamento. */
    fun setLive(trip: Trip?) {
        liveBox.removeAllViews()
        if (trip != null) liveBox.addView(card(trip, "Viagem em andamento", null))
    }

    private fun render() {
        list.removeAllViews()
        clearAll.enable(history.isNotEmpty())
        if (history.isEmpty()) {
            list.addView(
                activity.label(
                    "Nenhuma viagem gravada. A viagem começa sozinha quando o motor liga e termina 20 segundos depois de desligar.",
                    18f, Theme.MUTED
                )
            )
            return
        }
        for (t in history) {
            list.addView(
                card(t, dateFmt.format(Date(t.startMs))) { confirmDelete(t) },
                LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT).apply { bottomMargin = activity.dp(8) }
            )
        }
    }

    private fun card(t: Trip, title: String, onDelete: (() -> Unit)?): LinearLayout {
        val c = LinearLayout(activity).apply {
            orientation = VERTICAL
            background = activity.rounded(Theme.SURFACE, 16)
            setPadding(activity.dp(16), activity.dp(12), activity.dp(16), activity.dp(12))
        }
        val top = LinearLayout(activity).apply { orientation = HORIZONTAL }
        top.addView(activity.label(title, 20f, Theme.INFO, true), LayoutParams(0, LayoutParams.WRAP_CONTENT, 1f))
        if (onDelete != null) top.addView(activity.bigButton("Apagar") { onDelete() })
        c.addView(top)
        c.addView(activity.label("Duração: ${duration(t.durationMs)}    Distância: ${String.format("%.1f", t.distanceKm)} km", 18f))
        val avg = t.avgSpeed?.let { String.format("%.0f km/h", it) } ?: "--"
        val kml = t.kmPerLiter?.let { String.format("%.1f km/L", it) } ?: "--"
        c.addView(activity.label("Média: $avg    Máxima: ${String.format("%.0f", t.maxSpeed)} km/h    Consumo: $kml", 18f))
        t.score?.let { sc ->
            c.addView(
                activity.label("Condução: nota $sc    ${t.events} eventos", 18f, if (sc >= 80) Theme.OK else if (sc >= 60) Theme.WARN else Theme.DANGER)
            )
        }
        return c
    }

    private fun duration(ms: Long): String {
        val s = ms / 1000
        val h = s / 3600
        val m = (s % 3600) / 60
        return if (h > 0) String.format("%d h %02d min", h, m) else String.format("%d min %02d s", m, s % 60)
    }

    private fun confirmDelete(t: Trip) {
        AlertDialog.Builder(activity)
            .setTitle("Apagar esta viagem?")
            .setMessage("A viagem de ${dateFmt.format(Date(t.startMs))} será apagada do aparelho.")
            .setPositiveButton("Apagar") { _, _ -> delete { TripDb.get(activity).delete(t.id) } }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    private fun confirmDeleteAll() {
        AlertDialog.Builder(activity)
            .setTitle("Apagar todas as viagens?")
            .setMessage("Todo o histórico de viagens será apagado do aparelho. Não dá para desfazer.")
            .setPositiveButton("Apagar tudo") { _, _ -> delete { TripDb.get(activity).deleteAll() } }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    private fun delete(action: () -> Unit) {
        activity.lifecycleScope.launch {
            withContext(Dispatchers.IO) { action() }
            reload()
        }
    }
}
