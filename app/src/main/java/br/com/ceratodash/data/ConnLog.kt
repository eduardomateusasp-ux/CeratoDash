package br.com.ceratodash.data

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Registro curto das etapas da conexão com o adaptador (BLE, ELM327), para diagnosticar falhas sem
 * precisar do log do Android. Fica só na memória e só tem as últimas linhas. Nada sai do aparelho.
 */
object ConnLog {
    const val MAX_LINES = 120
    private val lines = ArrayList<String>()
    private val fmt = SimpleDateFormat("HH:mm:ss", Locale.US)

    @Synchronized
    fun add(msg: String) {
        lines.add(fmt.format(Date()) + " " + msg)
        while (lines.size > MAX_LINES) lines.removeAt(0)
    }

    @Synchronized
    fun text(): String = if (lines.isEmpty()) "Nenhum registro ainda." else lines.joinToString("\n")

    @Synchronized
    fun clear() = lines.clear()

    @Synchronized
    fun size(): Int = lines.size
}
