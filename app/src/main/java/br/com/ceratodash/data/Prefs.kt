package br.com.ceratodash.data

import br.com.ceratodash.data.*
import br.com.ceratodash.obd.*
import br.com.ceratodash.service.*
import br.com.ceratodash.ui.*

import android.content.Context

/** Preferências guardadas só no aparelho (SharedPreferences). */
object Prefs {
    private fun sp(c: Context) = c.getSharedPreferences("ceratodash", Context.MODE_PRIVATE)

    fun adapter(c: Context): String? = sp(c).getString("addr", null)
    fun setAdapter(c: Context, addr: String) = sp(c).edit().putString("addr", addr).apply()

    fun fuel(c: Context): Fuel = Fuel.values().getOrElse(sp(c).getInt("fuel", 0)) { Fuel.GASOLINA }
    fun setFuel(c: Context, f: Fuel) = sp(c).edit().putInt("fuel", f.ordinal).apply()

    /** Sensores escolhidos para os gráficos da tela de Telemetria. */
    fun chartKeys(c: Context): List<String> =
        sp(c).getString("charts", "rpm,speed,coolant")!!.split(',').filter { it.isNotBlank() }

    fun setChartKeys(c: Context, keys: List<String>) =
        sp(c).edit().putString("charts", keys.joinToString(",")).apply()

    /** Limites de alerta (rotação máxima e temperatura crítica da água). */
    fun limits(c: Context): AlertLimits = AlertLimits(
        rpmMax = sp(c).getInt("rpmMax", 6500),
        waterDanger = sp(c).getInt("waterDanger", 108)
    )

    fun setRpmMax(c: Context, v: Int) = sp(c).edit().putInt("rpmMax", v).apply()
    fun setWaterDanger(c: Context, v: Int) = sp(c).edit().putInt("waterDanger", v).apply()

    /** Iniciar a leitura junto com o sistema da central. Desligado por padrão. */
    fun autostart(c: Context): Boolean = sp(c).getBoolean("autostart", false)
    fun setAutostart(c: Context, on: Boolean) = sp(c).edit().putBoolean("autostart", on).apply()
}
