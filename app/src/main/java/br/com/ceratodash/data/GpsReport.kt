package br.com.ceratodash.data

/** Estado do teste de GPS naquele instante. */
data class GpsState(
    val providerPresent: Boolean,
    val locationOn: Boolean,
    val visible: Int,
    val used: Int,
    val accuracyM: Float?,
    val speedKmh: Float?,
    val ttffSec: Int?,
    val elapsedSec: Int
)

/**
 * Texto do teste de GPS. Diz se a central tem provedor de GPS, se a localização está ligada, quantos
 * satélites ela vê e se já obteve posição. Não mostra coordenadas (nada além da precisão). Lógica pura.
 */
object GpsReport {
    fun format(s: GpsState): String {
        if (!s.providerPresent) {
            return "Esta central não tem provedor de GPS. Mapas offline não teriam posição."
        }
        if (!s.locationOn) {
            return "A localização está desligada. Ligue em Configurações, Localização, da central, e toque em Atualizar."
        }
        val sb = StringBuilder()
        sb.append("Tempo de teste: ${s.elapsedSec} s\n")
        sb.append("Satélites vistos: ${s.visible}, usados na posição: ${s.used}\n")
        sb.append("Posição: ")
        sb.append(if (s.accuracyM != null) "obtida, precisão de cerca de ${s.accuracyM.toInt()} m" else "ainda não")
        sb.append('\n')
        if (s.speedKmh != null) sb.append("Velocidade pelo GPS: ${s.speedKmh.toInt()} km/h\n")
        if (s.ttffSec != null) sb.append("Tempo até a primeira posição: ${s.ttffSec} s\n")
        sb.append('\n')
        sb.append(
            when {
                s.accuracyM != null -> "Resultado: o GPS da central está funcionando."
                s.visible > 0 -> "Resultado: a central vê satélites, mas ainda sem posição. Fique em lugar aberto e espere."
                s.elapsedSec >= 60 -> "Resultado: nenhum satélite em 1 minuto. A antena de GPS pode estar desligada, mal posicionada ou ausente. Teste ao ar livre, não em garagem."
                else -> "Procurando satélites. Pode levar de alguns segundos a alguns minutos. Fique ao ar livre."
            }
        )
        return sb.toString()
    }
}
