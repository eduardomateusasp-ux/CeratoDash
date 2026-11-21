package br.com.ceratodash

import java.io.IOException
import java.io.InputStream
import java.io.OutputStream

/** Conversa com o adaptador ELM327 (comandos AT e consultas OBD-II). */
class Elm327(private val input: InputStream, private val output: OutputStream) {

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
            val resp = send("01$code", 15000)
            val data = findData(resp, "41$code", 4)
            if (data == null) {
                if (base == 0) throw IOException("A ECU não respondeu. Ligue a ignição ou o motor.")
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

    /** Lê um sensor. Devolve null se o carro não respondeu. */
    fun query(p: Pid): Float? {
        val resp = send("01" + p.code)
        val data = findData(resp, "41" + p.code, p.bytes) ?: return null
        return p.decode(data)
    }

    /** Tensão medida pelo próprio adaptador (ATRV). */
    fun voltage(): Float? {
        val r = send("ATRV")
        return r.lines().firstNotNullOfOrNull { it.replace("V", "").trim().toFloatOrNull() }
    }

    private fun findData(resp: String, prefix: String, bytes: Int): IntArray? {
        for (line in resp.split('\n')) {
            val l = line.replace(" ", "")
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
