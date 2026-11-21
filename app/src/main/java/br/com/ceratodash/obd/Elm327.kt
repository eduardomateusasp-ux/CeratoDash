package br.com.ceratodash.obd

import br.com.ceratodash.data.*
import br.com.ceratodash.obd.*
import br.com.ceratodash.service.*
import br.com.ceratodash.ui.*

import java.io.IOException
import java.io.InputStream
import java.io.OutputStream

/** Conversa com o adaptador ELM327 (comandos AT e consultas OBD-II). */
class Elm327(private val input: InputStream, private val output: OutputStream) {

    /** Último problema relatado pelo adaptador/carro numa consulta sem resposta, já em português. */
    @Volatile
    var lastProblem: String? = null
        private set

    companion object {
        private val ruido = Regex("^(SEARCHING\\.\\.\\.|BUSINIT:(\\.\\.\\.)?OK)")

        /**
         * Traduz mensagens de erro do ELM327 (lista do manual do ELM327, "Messages and alerts").
         * Devolve null se a resposta não contém erro conhecido.
         */
        private val quadroMulti = Regex("^[0-9A-F]:")

        private fun limpar(resp: String): List<String> =
            resp.split('\n').map { it.replace(" ", "").replaceFirst(ruido, "") }.filter { it.isNotEmpty() }

        /** Há uma linha de resposta positiva do modo (ex.: "43" no modo 03) ou um quadro múltiplo CAN? */
        fun temRespostaPositiva(resp: String, modo: String): Boolean =
            limpar(resp).any { it.startsWith(modo) || quadroMulti.containsMatchIn(it) }

        /**
         * Lê os códigos DTC de uma resposta do modo 03. Cada código usa 2 bytes: os 2 bits de cima
         * dão a letra (P, C, B, U), os 2 seguintes o primeiro dígito, e o resto 3 dígitos hex
         * (SAE J2012). Em CAN o primeiro byte depois do 43 é a contagem e é descartado.
         * Respostas de várias ECUs são juntadas sem repetir códigos.
         */
        fun parseDtcs(resp: String, can: Boolean): List<String> {
            val linhas = limpar(resp)
            val multi = linhas.filter { quadroMulti.containsMatchIn(it) }
            val fluxos = if (multi.isNotEmpty()) listOf(multi.joinToString("") { it.substring(2) })
            else linhas.filter { it.startsWith("43") }
            val out = LinkedHashSet<String>()
            for (f in fluxos) {
                if (!f.startsWith("43")) continue
                var hex = f.substring(2)
                if (can) hex = hex.drop(2)
                var i = 0
                while (i + 4 <= hex.length) {
                    val v = hex.substring(i, i + 4).toIntOrNull(16)
                    i += 4
                    if (v == null || v == 0) continue
                    out.add("${"PCBU"[(v shr 14) and 3]}${(v shr 12) and 3}${"%03X".format(v and 0xFFF)}")
                }
            }
            return out.toList()
        }

        fun explain(resp: String): String? {
            val u = resp.uppercase()
            return when {
                "LV RESET" in u -> "O adaptador reiniciou por tensão baixa. Verifique a bateria do carro."
                "BUS INIT" in u && "ERROR" in u ->
                    "Falha ao iniciar a comunicação com a ECU (BUS INIT). Ligue o motor e tente de novo."
                "CAN ERROR" in u -> "Erro na rede CAN do carro."
                "BUS BUSY" in u -> "O barramento do carro está ocupado."
                "FB ERROR" in u || "BUS ERROR" in u -> "Erro elétrico no barramento do carro."
                "DATA ERROR" in u -> "Resposta do carro com erro de dados."
                "BUFFER FULL" in u -> "O buffer do adaptador encheu."
                "STOPPED" in u -> "O adaptador interrompeu a consulta."
                "UNABLE TO CONNECT" in u -> "A ECU não respondeu. Ligue a ignição ou o motor."
                "NO DATA" in u -> "O carro não respondeu a este sensor."
                else -> null
            }
        }
    }

    fun send(cmd: String, timeoutMs: Long = 2500): String {
        while (input.available() > 0) input.read()
        output.write((cmd + "\r").toByteArray(Charsets.US_ASCII))
        output.flush()
        val sb = StringBuilder()
        val start = System.currentTimeMillis()
        while (System.currentTimeMillis() - start < timeoutMs) {
            if (input.available() > 0) {
                val c = input.read()
                if (c < 0) throw IOException("Conexão encerrada pelo adaptador")
                if (c.toChar() == '>') return normalize(sb.toString())
                sb.append(c.toChar())
            } else {
                Thread.sleep(4)
            }
        }
        throw IOException("O adaptador não respondeu a $cmd")
    }

    private fun normalize(raw: String): String =
        raw.replace('\r', '\n')
            .split('\n')
            .map { it.trim().uppercase() }
            .filter { it.isNotEmpty() }
            .joinToString("\n")

    /** Configura o adaptador e devolve os códigos de sensores que o carro diz suportar. */
    fun initialize(): Set<String> {
        send("ATZ", 6000)
        send("ATE0")
        send("ATL0")
        send("ATS0")
        send("ATH0")
        send("ATSP0")
        val supported = mutableSetOf<String>()
        var base = 0
        while (base <= 0x60) {
            val code = "%02X".format(base)
            var resp = send("01$code", 15000)
            var data = findData(resp, "41$code", 4)
            if (data == null && base == 0 && retentavel(resp)) {
                // A primeira busca de protocolo às vezes falha na hora em que a ignição liga.
                Thread.sleep(500)
                resp = send("01$code", 15000)
                data = findData(resp, "41$code", 4)
            }
            if (data == null) {
                if (base == 0) throw IOException(explain(resp) ?: "A ECU não respondeu. Ligue a ignição ou o motor.")
                break
            }
            val mask = data.fold(0L) { acc, b -> (acc shl 8) or b.toLong() }
            for (i in 0 until 32) {
                if (((mask shr (31 - i)) and 1L) == 1L) supported.add("%02X".format(base + i + 1))
            }
            if ((mask and 1L) == 0L) break
            base += 0x20
        }
        return supported
    }

    private fun retentavel(resp: String): Boolean {
        val u = resp.uppercase()
        return "STOPPED" in u || "BUS BUSY" in u || "CAN ERROR" in u || "BUS INIT" in u
    }

    /**
     * Lê um sensor. Devolve null se o carro não respondeu (o motivo fica em lastProblem).
     * Lança IOException se o adaptador reiniciou e perdeu a configuração.
     */
    fun query(p: Pid): Float? {
        val resp = send("01" + p.code)
        if ("LV RESET" in resp || "ELM327" in resp) {
            throw IOException(explain(resp) ?: "O adaptador reiniciou.")
        }
        val data = findData(resp, "41" + p.code, p.bytes)
        if (data == null) {
            lastProblem = explain(resp)
            return null
        }
        lastProblem = null
        return p.decode(data)
    }

    /** Verdadeiro se o protocolo em uso é CAN (6 a 9 no ATDPN). Nesse caso a resposta do modo 03 traz byte de contagem. */
    fun isCan(): Boolean {
        val p = send("ATDPN").lines().firstOrNull()?.removePrefix("A") ?: return false
        return p.firstOrNull() in '6'..'9'
    }

    /** Luz de injeção (MIL) e quantidade de códigos, do PID 0101. Nulo se o carro não respondeu. */
    fun milStatus(): Pair<Boolean, Int>? {
        val d = findData(send("0101"), "4101", 4) ?: return null
        return Pair(d[0] and 0x80 != 0, d[0] and 0x7F)
    }

    /** Códigos de falha armazenados (modo 03). Lança IOException se o carro não respondeu. */
    fun readDtcs(): List<String> {
        val can = isCan()
        val resp = send("03", 8000)
        if (!temRespostaPositiva(resp, "43")) {
            throw IOException(motivo(resp, "O carro não respondeu à leitura de códigos."))
        }
        return parseDtcs(resp, can)
    }

    /** Limpa os códigos e apaga a luz de injeção (modo 04). Lança IOException se o carro não confirmar. */
    fun clearDtcs() {
        val resp = send("04", 8000)
        if (!temRespostaPositiva(resp, "44")) {
            throw IOException(motivo(resp, "O carro não confirmou a limpeza dos códigos."))
        }
    }

    /** Mensagem de erro de um comando de diagnóstico; "NO DATA" aqui não é sobre um sensor. */
    private fun motivo(resp: String, padrao: String): String =
        if ("NO DATA" in resp.uppercase()) padrao else explain(resp) ?: padrao

    /** Tensão medida pelo próprio adaptador (ATRV). */
    fun voltage(): Float? {
        val r = send("ATRV")
        return r.lines().firstNotNullOfOrNull { it.replace("V", "").trim().toFloatOrNull() }
    }

    private fun findData(resp: String, prefix: String, bytes: Int): IntArray? {
        for (line in resp.split('\n')) {
            val l = line.replace(" ", "").replaceFirst(ruido, "")
            if (l.startsWith(prefix) && l.length >= prefix.length + bytes * 2) {
                return try {
                    IntArray(bytes) { i ->
                        l.substring(prefix.length + i * 2, prefix.length + i * 2 + 2).toInt(16)
                    }
                } catch (e: NumberFormatException) {
                    null
                }
            }
        }
        return null
    }
}
