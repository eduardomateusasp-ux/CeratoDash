package br.com.ceratodash

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test
import java.io.IOException

class Elm327Test {

    private fun carroSimulado(cmd: String): String = when (cmd) {
        "ATZ" -> "ELM327 v1.5"
        "ATE0", "ATL0", "ATS0", "ATH0", "ATSP0" -> "OK"
        // Suporta 01,03,04,05,06,07, 0B a 0F, 11,13,14,15, 1C e 20 (bit 20 liga a faixa seguinte).
        "0100" -> "SEARCHING...\r41 00 BE 3E B8 11"
        "0120" -> "41 20 00 00 00 00"
        "010C" -> "41 0C 1A F8"
        "010D" -> "41 0D 3C"
        "0105" -> "NO DATA"
        "ATRV" -> "12.6V"
        else -> "?"
    }

    @Test
    fun inicializaEDescobreOsSensoresSuportados() {
        val fake = FakeElm(::carroSimulado)
        val suportados = fake.elm().initialize()
        for (p in listOf("01", "03", "04", "05", "06", "07", "0B", "0C", "0D", "0E", "0F", "11", "13", "14", "15", "1C", "20")) {
            assertTrue("faltou $p", p in suportados)
        }
        assertFalse("10" in suportados)
        assertFalse("21" in suportados)
        assertEquals(listOf("ATZ", "ATE0", "ATL0", "ATS0", "ATH0", "ATSP0", "0100", "0120"), fake.comandos)
    }

    @Test
    fun falhaComMensagemQuandoAEcuNaoResponde() {
        val fake = FakeElm { cmd -> if (cmd == "0100") "UNABLE TO CONNECT" else "OK" }
        try {
            fake.elm().initialize()
            fail("deveria lançar IOException")
        } catch (e: IOException) {
            assertTrue(e.message!!.contains("ECU"))
        }
    }

    @Test
    fun leRotacaoEVelocidade() {
        val elm = FakeElm(::carroSimulado).elm()
        // (0x1A * 256 + 0xF8) / 4 = 1726
        assertEquals(1726f, elm.query(Pids.byCode("0C")!!)!!, 0.001f)
        assertEquals(60f, elm.query(Pids.byCode("0D")!!)!!, 0.001f)
    }

    @Test
    fun semDadoDevolveNulo() {
        val elm = FakeElm(::carroSimulado).elm()
        assertNull(elm.query(Pids.byCode("05")!!))
    }

    @Test
    fun aceitaRespostaDeVariasEcus() {
        val elm = FakeElm { "41 0D 3C\r41 0D 3C" }.elm()
        assertEquals(60f, elm.query(Pids.byCode("0D")!!)!!, 0.001f)
    }

    @Test
    fun ignoraRespostaDeOutroSensor() {
        val elm = FakeElm { "41 0D 3C" }.elm()
        assertNull(elm.query(Pids.byCode("0C")!!))
    }

    @Test
    fun leTensaoDoAdaptador() {
        val elm = FakeElm(::carroSimulado).elm()
        assertEquals(12.6f, elm.voltage()!!, 0.001f)
    }

    @Test
    fun tensaoInvalidaDevolveNulo() {
        val elm = FakeElm { "?" }.elm()
        assertNull(elm.voltage())
    }

    @Test
    fun adaptadorMudoGeraErroDeTempoEsgotado() {
        val mudo = FakeElm { "" }
        // Resposta vazia ainda traz o prompt; para simular silêncio, usamos streams sem dados.
        val elm = Elm327(
            object : java.io.InputStream() {
                override fun available() = 0
                override fun read() = -1
            },
            mudo.output
        )
        try {
            elm.send("ATZ", 100)
            fail("deveria lançar IOException")
        } catch (e: IOException) {
            assertNotNull(e.message)
        }
    }
}
