package br.com.ceratodash

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
