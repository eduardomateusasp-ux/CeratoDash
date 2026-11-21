package br.com.ceratodash.data

/** Uma interface USB de um aparelho (classe, subclasse e protocolo). */
data class UsbIface(val cls: Int, val sub: Int, val proto: Int) {
    /** Interface de depuração Android (ADB): classe 255, subclasse 66, protocolo 1. */
    val isAdb: Boolean get() = cls == 255 && sub == 66 && proto == 1
}

data class UsbDev(val vendor: Int, val product: Int, val name: String?, val ifaces: List<UsbIface>)

/**
 * Texto do diagnóstico de USB, para saber se a porta da central enxerga o celular (modo anfitrião) e se o
 * celular oferece a interface de depuração, que o espelhamento por cabo precisaria. Lógica pura.
 */
object UsbReport {
    private fun hex(n: Int) = String.format("%04X", n)

    private fun className(c: Int): String = when (c) {
        1 -> "áudio"
        3 -> "teclado ou mouse"
        6 -> "imagem (câmera)"
        8 -> "armazenamento"
        9 -> "hub"
        255 -> "específica do fabricante"
        else -> "classe $c"
    }

    fun format(devices: List<UsbDev>, hostSupported: Boolean): String {
        if (!hostSupported) {
            return "Este sistema diz que não suporta USB como anfitrião. A central não consegue controlar um celular ligado por cabo."
        }
        if (devices.isEmpty()) {
            return "Nenhum aparelho USB visto. Ligue o celular no cabo e toque em Atualizar. Se continuar vazio, a porta pode ser só de carga ou de armazenamento."
        }
        val sb = StringBuilder()
        for ((i, d) in devices.withIndex()) {
            sb.append("Aparelho ").append(i + 1).append(": ").append(d.name ?: "sem nome")
                .append("  (").append(hex(d.vendor)).append(":").append(hex(d.product)).append(")\n")
            for (f in d.ifaces) {
                sb.append("  interface ").append(className(f.cls))
                    .append(" (").append(f.cls).append("/").append(f.sub).append("/").append(f.proto).append(")")
                if (f.isAdb) sb.append("  <- depuração ADB")
                sb.append('\n')
            }
        }
        val adb = devices.any { d -> d.ifaces.any { it.isAdb } }
        sb.append('\n')
        sb.append(
            if (adb) "Resultado: a central enxerga um aparelho com depuração ADB. O espelhamento por cabo parece possível."
            else "Resultado: a central enxerga o aparelho, mas sem a interface de depuração. Ligue a Depuração USB no celular (Opções do desenvolvedor) e atualize."
        )
        return sb.toString()
    }
}
