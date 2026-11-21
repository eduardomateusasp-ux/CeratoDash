package br.com.ceratodash.data

/** Estado simples compartilhado entre a tela e o serviço. Nada disso sai do aparelho. */
object AppState {
    /** A tela do CeratoDash está visível (a faixa sobre o mapa só aparece quando não está). */
    @Volatile
    var activityVisible = false

    /** Motivo de a faixa sobre o mapa não ter funcionado, em português. */
    @Volatile
    var overlayProblem: String? = null
}

object OverlayRules {
    /** A faixa só aparece se ligada, se a central permitir sobreposição e se outro app está na frente. */
    fun shouldShow(enabled: Boolean, canDraw: Boolean, appVisible: Boolean): Boolean =
        enabled && canDraw && !appVisible
}
