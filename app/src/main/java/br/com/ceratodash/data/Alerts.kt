package br.com.ceratodash.data

import br.com.ceratodash.obd.Telemetry

enum class Level { ATENCAO, PERIGO }

class Alert(val level: Level, val message: String)

/**
 * Limites de alerta. A temperatura de atenção é sempre 8 °C abaixo da crítica (padrão 100 e 108 °C).
 * A tensão da bateria segue a faixa pedida: abaixo de 11,5 V ou acima de 15,2 V.
 */
data class AlertLimits(
    val rpmMax: Int = 6500,
    val waterDanger: Int = 108,
    val voltLow: Float = 11.5f,
    val voltHigh: Float = 15.2f
) {
    val waterWarn: Int get() = waterDanger - 8
}

object Alerts {
    /** Alertas ativos, do mais grave para o menos grave. Valores ausentes (carro não respondeu) não geram alerta. */
    fun evaluate(t: Telemetry, lim: AlertLimits): List<Alert> {
        val out = ArrayList<Alert>(3)
        val water = t.coolant
        if (water != null) {
            if (water >= lim.waterDanger) out.add(Alert(Level.PERIGO, "Temperatura do motor alta: ${water.toInt()} °C"))
            else if (water >= lim.waterWarn) out.add(Alert(Level.ATENCAO, "Temperatura do motor subindo: ${water.toInt()} °C"))
        }
        val v = t.voltage
        if (v != null && (v < lim.voltLow || v > lim.voltHigh)) {
            out.add(Alert(Level.PERIGO, "Tensão da bateria fora do normal: " + String.format("%.1f", v) + " V"))
        }
        val rpm = t.rpm
        if (rpm != null && rpm > lim.rpmMax) out.add(Alert(Level.PERIGO, "Rotação acima do limite: ${rpm.toInt()} rpm"))
        out.sortByDescending { it.level.ordinal }
        return out
    }
}
