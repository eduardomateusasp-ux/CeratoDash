package br.com.ceratodash.data

import br.com.ceratodash.obd.Telemetry
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class AlertasTest {
    private val lim = AlertLimits()

    @Test
    fun semDadoNaoGeraAlerta() {
        assertTrue(Alerts.evaluate(Telemetry(), lim).isEmpty())
    }

    @Test
    fun aguaNormalNaoAlerta() {
        assertTrue(Alerts.evaluate(Telemetry(coolant = 90f), lim).isEmpty())
    }

    @Test
    fun aguaAtencaoAPartirDe100() {
        val a = Alerts.evaluate(Telemetry(coolant = 100f), lim)
        assertEquals(1, a.size)
        assertEquals(Level.ATENCAO, a[0].level)
        assertTrue(Alerts.evaluate(Telemetry(coolant = 99f), lim).isEmpty())
    }

    @Test
    fun aguaPerigoAPartirDe108() {
        val a = Alerts.evaluate(Telemetry(coolant = 108f), lim)
        assertEquals(Level.PERIGO, a[0].level)
        assertTrue(a[0].message.contains("108"))
    }

    @Test
    fun tensaoForaDaFaixa() {
        assertEquals(Level.PERIGO, Alerts.evaluate(Telemetry(voltage = 11.4f), lim)[0].level)
        assertEquals(Level.PERIGO, Alerts.evaluate(Telemetry(voltage = 15.3f), lim)[0].level)
        assertTrue(Alerts.evaluate(Telemetry(voltage = 11.5f), lim).isEmpty())
        assertTrue(Alerts.evaluate(Telemetry(voltage = 14.2f), lim).isEmpty())
        assertTrue(Alerts.evaluate(Telemetry(voltage = 15.2f), lim).isEmpty())
    }

    @Test
    fun rotacaoAcimaDoLimiteConfigurado() {
        assertTrue(Alerts.evaluate(Telemetry(rpm = 6500f), lim).isEmpty())
        assertEquals(1, Alerts.evaluate(Telemetry(rpm = 6600f), lim).size)
        assertTrue(Alerts.evaluate(Telemetry(rpm = 6600f), AlertLimits(rpmMax = 7000)).isEmpty())
    }

    @Test
    fun limiteDeAguaConfiguravelMudaAAtencao() {
        val l = AlertLimits(waterDanger = 112)
        assertEquals(104, l.waterWarn)
        assertTrue(Alerts.evaluate(Telemetry(coolant = 103f), l).isEmpty())
        assertEquals(Level.ATENCAO, Alerts.evaluate(Telemetry(coolant = 104f), l)[0].level)
    }

    @Test
    fun maisGraveVemPrimeiro() {
        val a = Alerts.evaluate(Telemetry(coolant = 101f, voltage = 10f), lim)
        assertEquals(2, a.size)
        assertEquals(Level.PERIGO, a[0].level)
        assertEquals(Level.ATENCAO, a[1].level)
    }
}
