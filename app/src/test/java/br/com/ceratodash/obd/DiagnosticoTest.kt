package br.com.ceratodash.obd

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test
import java.io.IOException

class DiagnosticoTest {

    @Test
    fun leCodigosEmCan() {
        val r = Elm327.parseDtcs("43 02 01 33 03 00 00 00", can = true)
        assertEquals(listOf("P0133", "P0300"), r)
    }

    @Test
    fun leCodigosForaDeCan() {
        val r = Elm327.parseDtcs("43 01 33 03 00 00 00", can = false)
        assertEquals(listOf("P0133", "P0300"), r)
    }

    @Test
    fun letrasEDigitosDoCodigo() {
        // 0x4123 = C0123, 0x8001 = B0001, 0xC155 = U0155, 0x1133 = P1133
        val r = Elm327.parseDtcs("43 41 23 80 01 C1 55", can = false)
        assertEquals(listOf("C0123", "B0001", "U0155"), r)
        assertEquals(listOf("P1133"), Elm327.parseDtcs("43 11 33 00 00 00", can = false))
    }

    @Test
    fun semCodigos() {
        assertTrue(Elm327.parseDtcs("43 00 00 00 00 00 00", can = true).isEmpty())
    }

    @Test
    fun variasEcusNaoRepetemCodigo() {
        val r = Elm327.parseDtcs("43 01 33 00 00 00\n43 01 33 00 00 00", can = false)
        assertEquals(listOf("P0133"), r)
    }

    @Test
    fun quadroMultiploCan() {
        val resp = "008\n0: 43 04 01 33 03 00\n1: 04 20 00 00 00 00"
        assertEquals(listOf("P0133", "P0300", "P0420"), Elm327.parseDtcs(resp, can = true))
    }

    @Test
    fun readDtcsPeloAdaptadorSimulado() {
        val fake = FakeElm { cmd ->
            when (cmd) {
                "ATDPN" -> "A6"
                "03" -> "43 01 01 33 00 00 00" // em CAN: 43, contagem (1), código 0133, preenchimento
                else -> "OK"
            }
        }
        assertEquals(listOf("P0133"), fake.elm().readDtcs())
    }

    @Test
    fun readDtcsSemRespostaLancaErro() {
        val fake = FakeElm { cmd -> if (cmd == "03") "NO DATA" else "A6" }
        try {
            fake.elm().readDtcs()
            fail("deveria lançar IOException")
        } catch (e: IOException) {
            assertTrue(e.message!!.contains("não respondeu"))
        }
    }

    @Test
    fun readDtcsSemCodigosGuardados() {
        val fake = FakeElm { cmd -> if (cmd == "03") "43 00 00 00 00 00 00" else "A6" }
        assertTrue(fake.elm().readDtcs().isEmpty())
    }

    @Test
    fun luzDeInjecao() {
        val acesa = FakeElm { "41 01 82 07 E5 00" }.elm().milStatus()
        assertEquals(Pair(true, 2), acesa)
        val apagada = FakeElm { "41 01 02 07 E5 00" }.elm().milStatus()
        assertEquals(Pair(false, 2), apagada)
        assertNull(FakeElm { "NO DATA" }.elm().milStatus())
    }

    @Test
    fun limparConfirmadoPeloCarro() {
        val fake = FakeElm { "44" }
        fake.elm().clearDtcs()
        assertEquals(listOf("04"), fake.comandos)
    }

    @Test
    fun limparSemConfirmacaoLancaErro() {
        try {
            FakeElm { "NO DATA" }.elm().clearDtcs()
            fail("deveria lançar IOException")
        } catch (e: IOException) {
            assertNotNull(e.message)
        }
    }
}

class HistoryTest {
    private fun rpm(v: Float?) = Telemetry(rpm = v)

    @Test
    fun registraPicoEMinimo() {
        val h = History(windowSec = 2, periodMs = 500)
        assertFalse(h.hasData("rpm"))
        for ((i, v) in listOf(800f, 3000f, 1500f).withIndex()) h.add(i * 500L, rpm(v))
        assertTrue(h.hasData("rpm"))
        assertEquals(3000f, h.peak("rpm")!!, 0.001f)
        assertEquals(800f, h.min("rpm")!!, 0.001f)
    }

    @Test
    fun sensorSemRespostaNaoTemDado() {
        val h = History(windowSec = 2, periodMs = 500)
        h.add(0, rpm(900f))
        assertFalse(h.hasData("coolant"))
        assertNull(h.peak("coolant"))
    }

    @Test
    fun respeitaOPeriodoDeAmostragem() {
        val h = History(windowSec = 2, periodMs = 500)
        assertTrue(h.add(0, rpm(1f)))
        assertFalse(h.add(100, rpm(2f)))
        assertTrue(h.add(500, rpm(3f)))
    }

    @Test
    fun janelaDeslizaEmOrdemCronologica() {
        val h = History(windowSec = 2, periodMs = 500) // guarda 4 amostras
        for (i in 1..6) h.add(i * 500L, rpm(i.toFloat()))
        val buf = FloatArray(h.size)
        assertEquals(4, h.copyInto("rpm", buf))
        assertEquals(listOf(3f, 4f, 5f, 6f), buf.toList())
        // pico e mínimo valem para a sessão inteira, não só para a janela
        assertEquals(6f, h.peak("rpm")!!, 0.001f)
        assertEquals(1f, h.min("rpm")!!, 0.001f)
    }

    @Test
    fun valorNuloViraBuracoNoGrafico() {
        val h = History(windowSec = 2, periodMs = 500)
        h.add(0, rpm(1000f))
        h.add(500, rpm(null))
        val buf = FloatArray(h.size)
        assertEquals(2, h.copyInto("rpm", buf))
        assertTrue(buf[1].isNaN())
    }

    @Test
    fun resetLimpaTudo() {
        val h = History(windowSec = 2, periodMs = 500)
        h.add(0, rpm(1000f))
        h.reset()
        assertFalse(h.hasData("rpm"))
        assertTrue(h.add(0, rpm(1f)))
    }
}
