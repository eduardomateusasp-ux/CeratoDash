package br.com.ceratodash.data

/** Dados da central coletados pela tela de diagnóstico. Campos nulos = o Android não informou. */
data class DeviceInfo(
    val manufacturer: String,
    val model: String,
    val hardware: String,
    val androidRelease: String,
    val sdk: Int,
    val ramTotalMb: Long,
    val ramFreeMb: Long,
    val widthPx: Int,
    val heightPx: Int,
    val densityDpi: Int,
    val bluetoothOn: Boolean?,
    val bleFeature: Boolean,
    val wifiFeature: Boolean,
    val wifiOn: Boolean?,
    val internet: Boolean?,
    val gpsFeature: Boolean,
    val locationOn: Boolean?,
    val usbHost: Boolean,
    val overlayAllowed: Boolean?,
    /** Apps conhecidos e se estão instalados (nome, instalado). */
    val knownApps: List<Pair<String, Boolean>>,
    /** Apps cujo nome sugere espelhamento ou CarPlay. */
    val similarApps: List<String>
)

/**
 * Texto do diagnóstico da central. Responde de uma vez: o que a central tem (Wi-Fi, GPS, Bluetooth LE, USB
 * anfitrião, Play Store...) e o que cada recurso permite (mapas na central, AirPlay, adaptador de CarPlay).
 * Lógica pura, testada. Nada é inferido além do que o Android informa.
 */
object DeviceReport {
    private fun yn(b: Boolean?): String = when (b) {
        true -> "sim"
        false -> "não"
        null -> "não informado"
    }

    fun format(i: DeviceInfo): String {
        val sb = StringBuilder()
        sb.append("APARELHO\n")
        sb.append("  ${i.manufacturer} ${i.model}, ${i.hardware}\n")
        sb.append("  Android ${i.androidRelease} (API ${i.sdk})\n")
        sb.append("  Memória: ${i.ramFreeMb} MB livres de ${i.ramTotalMb} MB\n")
        val dp = if (i.densityDpi > 0) i.widthPx * 160 / i.densityDpi else 0
        val dph = if (i.densityDpi > 0) i.heightPx * 160 / i.densityDpi else 0
        sb.append("  Tela: ${i.widthPx} x ${i.heightPx} px (${dp} x ${dph} dp, ${i.densityDpi} dpi)\n")
        sb.append("\nRECURSOS\n")
        sb.append("  Bluetooth ligado: ${yn(i.bluetoothOn)}\n")
        sb.append("  Bluetooth LE (BLE): ${yn(i.bleFeature)}\n")
        sb.append("  Wi-Fi no aparelho: ${yn(i.wifiFeature)}, ligado: ${yn(i.wifiOn)}\n")
        sb.append("  Internet agora: ${yn(i.internet)}\n")
        sb.append("  GPS no aparelho: ${yn(i.gpsFeature)}, localização ligada: ${yn(i.locationOn)}\n")
        sb.append("  USB como anfitrião: ${yn(i.usbHost)}\n")
        sb.append("  Sobrepor outros apps (faixa sobre o mapa): ${yn(i.overlayAllowed)}\n")
        sb.append("\nAPPS\n")
        for ((nome, ok) in i.knownApps) sb.append("  $nome: ${if (ok) "instalado" else "não instalado"}\n")
        if (i.similarApps.isEmpty()) {
            sb.append("  Apps de espelhamento ou CarPlay: nenhum encontrado\n")
        } else {
            sb.append("  Possíveis apps de espelhamento ou CarPlay:\n")
            for (a in i.similarApps) sb.append("    $a\n")
        }
        sb.append("\nO QUE ISSO PERMITE\n")
        for (l in conclusions(i)) sb.append("  ").append(l).append('\n')
        return sb.toString().trimEnd()
    }

    private fun installed(i: DeviceInfo, nome: String): Boolean = i.knownApps.any { it.first == nome && it.second }

    /** Frases curtas sobre o que dá para tentar, com base só nos recursos informados. */
    fun conclusions(i: DeviceInfo): List<String> {
        val out = ArrayList<String>()
        out.add(if (i.bleFeature) "Adaptadores OBD só BLE: a central tem Bluetooth LE." else "Adaptadores OBD só BLE não funcionam: a central não tem Bluetooth LE. Use adaptador clássico.")
        out.add(
            if (i.usbHost) "Adaptador de CarPlay e espelhamento por cabo: a porta USB pode ser anfitriã (confira em Ver USB com o celular ligado)."
            else "Adaptador de CarPlay e espelhamento por cabo: não, a central diz que não tem USB anfitrião."
        )
        out.add(
            if (i.wifiFeature) "Espelhamento AirPlay e internet do celular: a central tem Wi-Fi."
            else "Espelhamento AirPlay e internet do celular: não, a central não tem Wi-Fi."
        )
        val maps = installed(i, "Google Maps")
        val store = installed(i, "Play Store")
        out.add(
            when {
                maps -> "Google Maps já está na central."
                store && i.wifiFeature -> "Dá para tentar instalar Maps ou Waze pela Play Store."
                else -> "Maps e Waze na central: sem Play Store ou sem Wi-Fi, só instalando APK."
            }
        )
        if ((maps || store) && !i.gpsFeature) out.add("Atenção: sem GPS na central, Maps e Waze nela teriam posição ruim.")
        return out
    }
}
