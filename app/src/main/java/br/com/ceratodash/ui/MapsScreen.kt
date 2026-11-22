package br.com.ceratodash.ui

import android.content.Context
import android.widget.LinearLayout
import android.widget.TextView

/** Mapas: atalhos para o Google Maps e o Waze. A leitura do carro continua em segundo plano pelo serviço. */
class MapsScreen(context: Context, private val open: (pkgs: List<String>, nome: String) -> Unit) : LinearLayout(context) {
    init {
        orientation = VERTICAL
        setPadding(context.dp(24), context.dp(16), context.dp(24), context.dp(16))
        addView(context.label("Mapas", 24f, Theme.TEXT, true))
        val row = LinearLayout(context).apply { orientation = HORIZONTAL }
        row.addView(context.bigButton("Organic Maps") { open(listOf("app.organicmaps"), "Organic Maps") })
        // OsmAnd: o pacote da versão gratuita e o da versão Plus (nomes não conferidos nesta central).
        row.addView(context.bigButton("OsmAnd") { open(listOf("net.osmand", "net.osmand.plus"), "OsmAnd") })
        row.addView(context.bigButton("Google Maps") { open(listOf("com.google.android.apps.maps"), "Google Maps") })
        row.addView(context.bigButton("Waze") { open(listOf("com.waze"), "Waze") })
        addView(row, LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT).apply { topMargin = context.dp(12) })
        val note: TextView = context.label(
            "Organic Maps e OsmAnd navegam 100% offline com mapas baixados antes e precisam de GPS na central. O Google Maps funciona sem internet se você baixar as áreas antes. O Waze precisa de internet. " +
                "A leitura do carro continua funcionando enquanto o mapa está aberto. Com a faixa ligada nos Ajustes, rotação, velocidade e água aparecem sobre o mapa, se a central permitir.",
            18f, Theme.MUTED
        )
        addView(note, LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT).apply { topMargin = context.dp(16) })
    }
}
