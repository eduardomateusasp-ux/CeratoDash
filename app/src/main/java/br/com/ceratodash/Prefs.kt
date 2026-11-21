package br.com.ceratodash

import android.content.Context

/** Preferências guardadas só no aparelho (SharedPreferences). */
object Prefs {
    private fun sp(c: Context) = c.getSharedPreferences("ceratodash", Context.MODE_PRIVATE)

    fun adapter(c: Context): String? = sp(c).getString("addr", null)
    fun setAdapter(c: Context, addr: String) = sp(c).edit().putString("addr", addr).apply()

    fun fuel(c: Context): Fuel = Fuel.values().getOrElse(sp(c).getInt("fuel", 0)) { Fuel.GASOLINA }
    fun setFuel(c: Context, f: Fuel) = sp(c).edit().putInt("fuel", f.ordinal).apply()

    /** Iniciar a leitura junto com o sistema da central. Desligado por padrão. */
    fun autostart(c: Context): Boolean = sp(c).getBoolean("autostart", false)
    fun setAutostart(c: Context, on: Boolean) = sp(c).edit().putBoolean("autostart", on).apply()
}
