package br.com.ceratodash.ui

import android.content.Context
import android.widget.LinearLayout
import android.widget.TextView

/** Mapas: atalhos para o Google Maps e o Waze. A leitura do carro continua em segundo plano pelo serviço. */
class MapsScreen(context: Context, private val open: (pkg: String, nome: String) -> Unit) : LinearLayout(context) {
    init {
        orientation = VERTICAL
        setPadding(context.dp(24), context.dp(16), context.dp(24), context.dp(16))
        addView(context.label("Mapas", 24f, Theme.TEXT, true))
        val row = LinearLayout(context).apply { orientation = HORIZONTAL }
        row.addView(context.bigButton("Google Maps") { open("com.google.android.apps.maps", "Google Maps") })
        row.addView(context.bigButton("Waze") { open("com.waze", "Waze") })
        addView(row, LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT).apply { topMargin = context.dp(12) })
        val note: TextView = context.label(
            "O Google Maps funciona sem internet se você baixar as áreas antes. O Waze precisa de internet. " +
                "A leitura do carro continua funcionando enquanto o mapa está aberto.",
            18f, Theme.MUTED
        )
        addView(note, LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT).apply { topMargin = context.dp(16) })
    }
}
