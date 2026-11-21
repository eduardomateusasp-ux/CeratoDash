package br.com.ceratodash.data

import br.com.ceratodash.obd.Telemetry
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class VozTest {
    private val lim = AlertLimits()
    private fun alerts(coolant: Float? = null, voltage: Float? = null, rpm: Float? = null) =
        Alerts.evaluate(Telemetry(coolant = coolant, voltage = voltage, rpm = rpm), lim)

    @Test
    fun cadaAlertaTemChaveEFrase() {
        for (a in alerts(coolant = 110f, voltage = 10f, rpm = 7000f)) {
            assertTrue(a.key.isNotBlank())
            assertTrue(a.spoken.isNotBlank())
        }
    }

    @Test
    fun alertaNovoEFaladoUmaVez() {
        val v = VoiceAnnouncer()
        val a = alerts(coolant = 110f)
        assertEquals(1, v.onTick(0, a, null, true, false).size)
        assertTrue(v.onTick(500, a, null, true, false).isEmpty())
        assertTrue(v.onTick(5000, a, null, true, false).isEmpty())
    }

    @Test
    fun alertaDePerigoRepeteACada30Segundos() {
        val v = VoiceAnnouncer()
        val a = alerts(coolant = 110f)
        assertEquals(1, v.onTick(0, a, null, true, false).size)
        assertTrue(v.onTick(29_000, a, null, true, false).isEmpty())
        assertEquals(1, v.onTick(30_000, a, null, true, false).size)
    }

    @Test
    fun alertaDeAtencaoNaoRepete() {
        val v = VoiceAnnouncer()
        val a = alerts(coolant = 102f) // atenção
        assertEquals(1, v.onTick(0, a, null, true, false).size)
        assertTrue(v.onTick(60_000, a, null, true, false).isEmpty())
    }

    @Test
    fun subirDeAtencaoParaPerigoFalaDeNovo() {
        val v = VoiceAnnouncer()
        assertEquals(1, v.onTick(0, alerts(coolant = 102f), null, true, false).size)
        val fala = v.onTick(1000, alerts(coolant = 109f), null, true, false)
        assertEquals(1, fala.size)
        assertTrue(fala[0].startsWith("Perigo"))
    }

    @Test
    fun alertaQueOscilaPertoDoLimiteNaoTagarela() {
        val v = VoiceAnnouncer()
        assertEquals(1, v.onTick(0, alerts(coolant = 109f), null, true, false).size)
        assertTrue(v.onTick(1000, emptyList(), null, true, false).isEmpty()) // sumiu
        assertTrue(v.onTick(2000, alerts(coolant = 109f), null, true, false).isEmpty()) // voltou em 2 s
    }

    @Test
    fun alertaQueVoltaDepoisDeUmTempoFalaDeNovo() {
        val v = VoiceAnnouncer()
        assertEquals(1, v.onTick(0, alerts(coolant = 109f), null, true, false).size)
        v.onTick(1000, emptyList(), null, true, false)
        assertEquals(1, v.onTick(20_000, alerts(coolant = 109f), null, true, false).size)
    }

    @Test
    fun vozDesligadaNaoFalaNada() {
        val v = VoiceAnnouncer()
        assertTrue(v.onTick(0, alerts(coolant = 110f), DriveEvent.ACELERACAO, false, false).isEmpty())
    }

    @Test
    fun alertasLigadosSemDicasNaoFalaDica() {
        val v = VoiceAnnouncer()
        assertTrue(v.onTick(0, emptyList(), DriveEvent.PEDAL, true, false).isEmpty())
    }

    @Test
    fun dicaDeConducaoEFaladaQuandoLigada() {
        val v = VoiceAnnouncer()
        val fala = v.onTick(0, emptyList(), DriveEvent.ACELERACAO, false, true)
        assertEquals(listOf(DriveEvent.ACELERACAO.tip), fala)
    }

    @Test
    fun dicasTemIntervaloMinimo() {
        val v = VoiceAnnouncer()
        assertEquals(1, v.onTick(0, emptyList(), DriveEvent.ACELERACAO, false, true).size)
        assertTrue(v.onTick(3000, emptyList(), DriveEvent.FREADA, false, true).isEmpty())
        assertEquals(1, v.onTick(9000, emptyList(), DriveEvent.FREADA, false, true).size)
    }

    @Test
    fun alertaTemPrioridadeSobreDica() {
        val v = VoiceAnnouncer()
        val fala = v.onTick(0, alerts(coolant = 110f), DriveEvent.ACELERACAO, true, true)
        assertEquals(1, fala.size)
        assertTrue(fala[0].startsWith("Perigo"))
        // a dica não vem logo depois do alerta
        assertTrue(v.onTick(2000, alerts(coolant = 110f), DriveEvent.PEDAL, true, true).isEmpty())
    }
}
