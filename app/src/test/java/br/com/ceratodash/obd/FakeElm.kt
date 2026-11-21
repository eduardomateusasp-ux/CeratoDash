package br.com.ceratodash.obd

import java.io.InputStream
import java.io.OutputStream
import java.util.concurrent.ConcurrentLinkedQueue

/**
 * ELM327 simulado para testes sem o carro. Cada comando terminado em CR recebe a resposta
 * devolvida por `responder`, seguida do prompt ">" como no adaptador real.
 */
class FakeElm(private val responder: (String) -> String) {
    val comandos = mutableListOf<String>()
    private val fila = ConcurrentLinkedQueue<Int>()
    private val linha = StringBuilder()

    val output: OutputStream = object : OutputStream() {
        override fun write(b: Int) {
            if (b == '\r'.code) {
                val cmd = linha.toString()
                linha.setLength(0)
                comandos.add(cmd)
                (responder(cmd) + "\r\r>").forEach { fila.add(it.code) }
            } else {
                linha.append(b.toChar())
            }
        }
    }

    val input: InputStream = object : InputStream() {
        override fun available(): Int = fila.size
        override fun read(): Int = fila.poll() ?: -1
    }

    fun elm() = Elm327(input, output)
}
