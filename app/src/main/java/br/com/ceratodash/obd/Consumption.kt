package br.com.ceratodash.obd

import br.com.ceratodash.data.*
import br.com.ceratodash.obd.*
import br.com.ceratodash.service.*
import br.com.ceratodash.ui.*

/**
 * Consumo instantâneo estimado a partir do fluxo de ar (MAF, g/s).
 * L/h = MAF * 3600 / (relação ar-combustível * densidade em g/L).
 * Sem MAF, não há estimativa: os dois valores ficam nulos (o painel mostra "--").
 */
object Consumption {
    fun apply(t: Telemetry, fuel: Fuel): Telemetry {
        val maf = t.maf ?: return t.copy(litersPerHour = null, kmPerLiter = null)
        val lph = maf * 3600f / (fuel.afr * fuel.density)
        val speed = t.speed ?: 0f
        val kml = if (speed > 3f && lph > 0.01f) speed / lph else null
        return t.copy(litersPerHour = lph, kmPerLiter = kml)
    }
}
