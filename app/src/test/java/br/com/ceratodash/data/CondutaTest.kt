package br.com.ceratodash.data

import br.com.ceratodash.obd.Pids
import br.com.ceratodash.obd.Telemetry
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class CondutaTest {

    private fun coach(style: DrivingStyle = DrivingStyle.EQUILIBRADA, speedMax: Int = 110) =
        DrivingCoach(style.profile, speedMax)

    private fun tel(speed: Float? = 40f, rpm: Float? = 1500f, throttle: Float? = 15f, coolant: Float? = 90f, load: Float? = 30f) =
        Telemetry(speed = speed, rpm = rpm, throttle = throttle, coolant = coolant, load = load)

    /** Alimenta o treinador a cada 500 ms. */
    private fun run(c: DrivingCoach, from: Long, to: Long, f: (Long) -> Telemetry) {
        var t = from
        while (t <= to) {
            c.onSample(t, f(t))
            t += 500
        }
    }

    private fun count(c: DrivingCoach, e: DriveEvent) = c.snapshot(0).counts[e.ordinal]

    @Test
    fun aceleracaoSuaveNaoGeraEvento() {
        val c = coach()
        // 4 km/h por segundo
        run(c, 0, 10000) { tel(speed = 20f + it / 250f) }
        assertEquals(0, c.snapshot(0).events)
    }

    @Test
    fun aceleracaoBruscaContaUmaVezPorEpisodio() {
        val c = coach()
        // 12 km/h por segundo, acima dos 10 do estilo equilibrado
        run(c, 0, 6000) { tel(speed = 20f + it * 12f / 1000f) }
        assertEquals(1, count(c, DriveEvent.ACELERACAO))
    }

    @Test
    fun estiloEsportivoToleraAMesmaAceleracao() {
        val c = coach(DrivingStyle.ESPORTIVA)
        run(c, 0, 6000) { tel(speed = 20f + it * 12f / 1000f) }
        assertEquals(0, count(c, DriveEvent.ACELERACAO))
    }

    @Test
    fun estiloEconomicoApontaAceleracaoMenor() {
        val c = coach(DrivingStyle.ECONOMICA)
        run(c, 0, 6000) { tel(speed = 20f + it * 9f / 1000f) } // 9 km/h por segundo > 8
        assertEquals(1, count(c, DriveEvent.ACELERACAO))
    }

    @Test
    fun freadaBrusca() {
        val c = coach()
        run(c, 0, 6000) { tel(speed = 100f - it * 14f / 1000f) }
        assertEquals(1, count(c, DriveEvent.FREADA))
        assertEquals(0, count(c, DriveEvent.ACELERACAO))
    }

    @Test
    fun pedalBrusco() {
        val c = coach()
        run(c, 0, 1500) { tel(throttle = 10f) }
        run(c, 2000, 5000) { tel(throttle = 80f) }
        assertEquals(1, count(c, DriveEvent.PEDAL))
    }

    @Test
    fun rotacaoAltaPrecisaDuraDoisSegundos() {
        val c = coach()
        run(c, 0, 1500) { tel(rpm = 4500f) }
        assertEquals(0, count(c, DriveEvent.ROTACAO))
        run(c, 2000, 4000) { tel(rpm = 4500f) }
        assertEquals(1, count(c, DriveEvent.ROTACAO))
    }

    @Test
    fun rotacaoNormalNaoGeraEvento() {
        val c = coach()
        run(c, 0, 20000) { tel(rpm = 2500f) }
        assertEquals(0, c.snapshot(0).events)
    }

    @Test
    fun motorFrioForcado() {
        val c = coach()
        run(c, 0, 4000) { tel(coolant = 40f, rpm = 3500f) }
        assertEquals(1, count(c, DriveEvent.MOTOR_FRIO))
    }

    @Test
    fun motorQuenteComAMesmaRotacaoNaoEFrio() {
        val c = coach()
        run(c, 0, 4000) { tel(coolant = 85f, rpm = 3500f) }
        assertEquals(0, count(c, DriveEvent.MOTOR_FRIO))
    }

    @Test
    fun paradoComMotorLigadoPorMuitoTempo() {
        val c = coach(DrivingStyle.ECONOMICA) // 60 s
        run(c, 0, 59000) { tel(speed = 0f, rpm = 800f) }
        assertEquals(0, count(c, DriveEvent.PARADO))
        run(c, 59500, 62000) { tel(speed = 0f, rpm = 800f) }
        assertEquals(1, count(c, DriveEvent.PARADO))
    }

    @Test
    fun velocidadeAcimaDoLimiteConfigurado() {
        val c = coach(speedMax = 100)
        run(c, 0, 4000) { tel(speed = 120f) }
        assertEquals(1, count(c, DriveEvent.VELOCIDADE))
        val d = coach(speedMax = 130)
        run(d, 0, 4000) { tel(speed = 120f) }
        assertEquals(0, count(d, DriveEvent.VELOCIDADE))
    }

    @Test
    fun motorArrastando() {
        val c = coach()
        run(c, 0, 4000) { tel(speed = 30f, rpm = 1200f, load = 85f) }
        assertEquals(1, count(c, DriveEvent.ARRASTANDO))
    }

    @Test
    fun semDadoDoCarroNaoInventaEvento() {
        val c = coach()
        // só a rotação respondeu: sem velocidade, borboleta, água ou carga
        run(c, 0, 20000) { Telemetry(rpm = 1500f) }
        assertEquals(0, c.snapshot(0).events)
    }

    @Test
    fun motorDesligadoNaoContaNada() {
        val c = coach()
        run(c, 0, 10000) { tel(speed = null, rpm = 0f) }
        assertFalse(c.snapshot(0).running)
        assertEquals(0, c.snapshot(0).events)
    }

    @Test
    fun novoEpisodioDepoisDeVoltarAoNormal() {
        val c = coach()
        run(c, 0, 3000) { tel(rpm = 4500f) }
        run(c, 3500, 6000) { tel(rpm = 2000f) }
        run(c, 6500, 9500) { tel(rpm = 4500f) }
        assertEquals(2, count(c, DriveEvent.ROTACAO))
    }

    @Test
    fun notaComecaEm100ECaiComEventos() {
        val c = coach()
        run(c, 0, 5000) { tel() }
        assertEquals(100, c.snapshot(0).score)
        run(c, 5500, 12000) { tel(rpm = 4500f) } // 1 evento de rotação (2 pontos)
        val s = c.snapshot(0)
        assertEquals(1, s.events)
        // 2 pontos em menos de 5 min: 100 - 2/5*40 = 84
        assertEquals(84, s.score)
    }

    @Test
    fun dicaAparecePor5Segundos() {
        val c = coach()
        run(c, 0, 4000) { tel(rpm = 4500f) }
        assertNotNull(c.snapshot(3000).tip)
        assertNull(c.snapshot(20000).tip)
    }

    @Test
    fun resetZeraTudo() {
        val c = coach()
        run(c, 0, 4000) { tel(rpm = 4500f) }
        c.reset()
        assertEquals(0, c.snapshot(0).events)
        assertEquals(100, c.snapshot(0).score)
    }

    @Test
    fun todosOsEventosTemTextoDeIdeal() {
        for (e in DriveEvent.values()) {
            assertTrue(e.ideal(DrivingStyle.EQUILIBRADA.profile, 110).isNotBlank())
            assertTrue(e.tip.isNotBlank())
        }
    }
}

class PontosEViagemTest {
    @Test
    fun motorMostraSensoresRespondidosESemDadoComoTraco() {
        val l = Hotspots.lines(Hotspot.MOTOR, Telemetry(rpm = 2000f, coolant = 90f), AlertLimits()).toMap()
        assertEquals("2000 rpm", l["Rotação"])
        assertEquals("90 °C", l["Temperatura da água"])
        assertEquals("--", l["Temperatura do óleo"])
    }

    @Test
    fun estadoDaBateria() {
        val lim = AlertLimits()
        fun estado(v: Float?) = Hotspots.lines(Hotspot.BATERIA, Telemetry(voltage = v), lim).toMap()["Estado"]
        assertEquals("Normal", estado(13.8f))
        assertEquals("Baixa", estado(11.0f))
        assertEquals("Alta", estado(15.5f))
        assertEquals("--", estado(null))
    }

    @Test
    fun escapamentoUsaOxigenioECatalisador() {
        val l = Hotspots.lines(Hotspot.ESCAPAMENTO, Telemetry(o2b1s1 = 0.45f, catTemp = 480f), AlertLimits()).toMap()
        assertEquals("0.45 V", l["Oxigênio antes do catalisador"])
        assertEquals("480 °C", l["Temperatura do catalisador"])
        assertEquals("--", l["Oxigênio depois do catalisador"])
    }

    @Test
    fun formulasDosNovosSensores() {
        // SAE J1979: O2 = A/200 V; catalisador = (256A + B)/10 - 40
        assertEquals(0.5f, Pids.byCode("14")!!.decode(intArrayOf(100, 0)), 0.0001f)
        assertEquals(0.9f, Pids.byCode("15")!!.decode(intArrayOf(180, 128)), 0.0001f)
        assertEquals(80f, Pids.byCode("3C")!!.decode(intArrayOf(0x04, 0xB0)), 0.0001f)
        assertEquals(-40f, Pids.byCode("3C")!!.decode(intArrayOf(0, 0)), 0.0001f)
    }

    @Test
    fun viagemGuardaANotaDeConducao() {
        val updates = mutableListOf<Trip>()
        val store = object : TripStore {
            override fun insert(startMs: Long) = 1L
            override fun update(trip: Trip, ended: Boolean) {
                updates.add(trip)
            }
        }
        var started = 0
        val rec = TripRecorder(store, onStart = { started++ }, coach = { 87 to 3 })
        for (t in 0..3000 step 500) rec.onSample(t.toLong(), Telemetry(rpm = 800f, speed = 30f))
        rec.finish(4000)
        assertEquals(1, started)
        assertEquals(87, updates.last().score)
        assertEquals(3, updates.last().events)
    }

    @Test
    fun viagemSemTreinadorFicaSemNota() {
        val updates = mutableListOf<Trip>()
        val store = object : TripStore {
            override fun insert(startMs: Long) = 1L
            override fun update(trip: Trip, ended: Boolean) {
                updates.add(trip)
            }
        }
        val rec = TripRecorder(store)
        for (t in 0..3000 step 500) rec.onSample(t.toLong(), Telemetry(rpm = 800f, speed = 30f))
        rec.finish(4000)
        assertNull(updates.last().score)
    }
}
