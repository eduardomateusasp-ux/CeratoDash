package br.com.ceratodash.obd

/** Um aparelho Bluetooth LE encontrado na busca. */
data class FoundDevice(val address: String, val name: String?, val rssi: Int) {
    companion object {
        /** Com nome primeiro (adaptadores quase sempre têm nome), depois por sinal mais forte. */
        fun sorted(list: Collection<FoundDevice>): List<FoundDevice> =
            list.sortedWith(compareByDescending<FoundDevice> { !it.name.isNullOrBlank() }.thenByDescending { it.rssi })
    }
}

object BleAddress {
    private val full = Regex("^([0-9A-F]{2}:){5}[0-9A-F]{2}$")

    /**
     * Normaliza um endereço digitado: aceita maiúsculas ou minúsculas, ":" ou "-" como separador, ou os
     * 12 caracteres colados. Devolve AA:BB:CC:DD:EE:FF, ou nulo se não for um endereço válido.
     */
    fun normalize(input: String): String? {
        val s = input.trim().uppercase().replace('-', ':').replace(" ", "")
        val candidate = if (s.length == 12 && !s.contains(':')) s.chunked(2).joinToString(":") else s
        return if (full.matches(candidate)) candidate else null
    }
}
