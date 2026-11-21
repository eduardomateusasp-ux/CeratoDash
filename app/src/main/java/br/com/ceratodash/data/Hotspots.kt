package br.com.ceratodash.data

import br.com.ceratodash.obd.Telemetry

/**
 * Pontos interativos sobre a foto do carro. As posições (fração da largura e da altura da imagem,
 * com a frente do carro para a direita) são aproximadas e valem para uma foto lateral. Se a sua
 * foto tiver outro enquadramento, ajuste os números aqui.
 */
enum class Hotspot(val title: String, val x: Float, val y: Float) {
    MOTOR("Motor", 0.84f, 0.42f),
    BATERIA("Bateria", 0.70f, 0.40f),
    ESCAPAMENTO("Escapamento", 0.04f, 0.76f)
}

object Hotspots {
    private fun fmt(v: Float?, decimals: Int, unit: String): String =
        if (v == null) "--" else String.format("%.${decimals}f", v) + " " + unit

    /** Linhas "rótulo e valor" de um ponto. Sensor que o carro não respondeu aparece como "--". */
    fun lines(h: Hotspot, t: Telemetry, lim: AlertLimits): List<Pair<String, String>> = when (h) {
        Hotspot.MOTOR -> listOf(
            "Rotação" to fmt(t.rpm, 0, "rpm"),
            "Carga do motor" to fmt(t.load, 0, "%"),
            "Temperatura da água" to fmt(t.coolant, 0, "°C"),
            "Temperatura do óleo" to fmt(t.oilTemp, 0, "°C"),
            "Avanço de ignição" to fmt(t.timing, 1, "°"),
            "Pressão do coletor" to fmt(t.map, 0, "kPa"),
            "Fluxo de ar" to fmt(t.maf, 1, "g/s")
        )
        Hotspot.BATERIA -> {
            val v = t.voltage
            val estado = when {
                v == null -> "--"
                v < lim.voltLow -> "Baixa"
                v > lim.voltHigh -> "Alta"
                else -> "Normal"
            }
            listOf("Tensão" to fmt(v, 1, "V"), "Estado" to estado)
        }
        Hotspot.ESCAPAMENTO -> listOf(
            "Oxigênio antes do catalisador" to fmt(t.o2b1s1, 2, "V"),
            "Oxigênio depois do catalisador" to fmt(t.o2b1s2, 2, "V"),
            "Temperatura do catalisador" to fmt(t.catTemp, 0, "°C"),
            "Correção de combustível curta" to fmt(t.stft, 1, "%"),
            "Correção de combustível longa" to fmt(t.ltft, 1, "%")
        )
    }
}
