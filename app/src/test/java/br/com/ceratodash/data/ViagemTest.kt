package br.com.ceratodash.data

import br.com.ceratodash.obd.Telemetry
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

private class FakeStore : TripStore {
    var inserts = 0
    val updates = mutableListOf<Pair<Trip, Boolean>>()
    override fun insert(startMs: Long): Long {
        inserts++
        return inserts.toLong()
    }

    override fun update(trip: Trip, ended: Boolean) {
        updates.add(trip to ended)
    }
}

class ViagemTest {

    private fun andando(speed: Float = 36f, lph: Float? = null) =
        Telemetry(rpm = 800f, speed = speed, litersPerHour = lph)

    private val parado = Telemetry() // sem resposta do carro

    @Test
    fun soComecaDepoisDe3SegundosComMotorLigado() {
        val store = FakeStore()
        val rec = TripRecorder(store)
        for (t in listOf(0L, 1000L, 2000L)) rec.onSample(t, andando())
        assertEquals(0, store.inserts)
        rec.onSample(3000, andando())
        assertEquals(1, store.inserts)
    }

    @Test
    fun rotacaoBaixaNaoIniciaViagem() {
        val store = FakeStore()
        val rec = TripRecorder(store)
        for (i in 0..20) rec.onSample(i * 1000L, Telemetry(rpm = 0f, speed = 0f))
        assertEquals(0, store.inserts)
    }

    @Test
    fun somaDistanciaEVelocidadeMaxima() {
        val store = FakeStore()
        var ultima: Trip? = null
        val rec = TripRecorder(store) { if (it != null) ultima = it }
        for (t in 0..3000 step 500) rec.onSample(t.toLong(), andando())
        // 20 amostras de 500 ms a 36 km/h = 10 s = 0,1 km
        for (t in 3500..13000 step 500) rec.onSample(t.toLong(), andando(speed = if (t == 8000) 90f else 36f))
        assertEquals(1, store.inserts)
        val trip = ultima!!
        assertEquals(0.1 + (90 - 36) * 0.5 / 3600.0, trip.distanceKm, 1e-6)
        assertEquals(90f, trip.maxSpeed, 0.001f)
    }

    @Test
    fun terminaDepoisDe20SegundosSemMotorEGravaOFimNoUltimoInstanteLigado() {
        val store = FakeStore()
        var terminou = false
        val rec = TripRecorder(store) { if (it == null) terminou = true }
        for (t in 0..10000 step 500) rec.onSample(t.toLong(), andando())
        rec.onSample(15000, parado)
        assertTrue(!terminou)
        rec.onSample(30000, parado)
        assertTrue(terminou)
        val (trip, ended) = store.updates.last()
        assertTrue(ended)
        assertEquals(10000L, trip.endMs)
        assertEquals(0L, trip.startMs)
    }

    @Test
    fun quedaCurtaDaConexaoNaoQuebraAViagem() {
        val store = FakeStore()
        val rec = TripRecorder(store)
        for (t in 0..5000 step 500) rec.onSample(t.toLong(), andando())
        for (t in 5500..14000 step 500) rec.onSample(t.toLong(), parado) // 9 s sem dado
        for (t in 14500..20000 step 500) rec.onSample(t.toLong(), andando())
        assertEquals(1, store.inserts)
        assertTrue(store.updates.none { it.second })
    }

    @Test
    fun viagemNovaDepoisDoFim() {
        val store = FakeStore()
        val rec = TripRecorder(store)
        for (t in 0..5000 step 500) rec.onSample(t.toLong(), andando())
        rec.onSample(40000, parado)
        for (t in 60000..65000 step 500) rec.onSample(t.toLong(), andando())
        assertEquals(2, store.inserts)
    }

    @Test
    fun finishFechaViagemEmAndamento() {
        val store = FakeStore()
        val rec = TripRecorder(store)
        for (t in 0..5000 step 500) rec.onSample(t.toLong(), andando())
        rec.finish(5100)
        assertTrue(store.updates.last().second)
    }

    @Test
    fun consumoMedioComFluxoDeAr() {
        val store = FakeStore()
        var ultima: Trip? = null
        val rec = TripRecorder(store) { if (it != null) ultima = it }
        for (t in listOf(0L, 1000L, 2000L, 3000L)) rec.onSample(t, andando(100f, 8f))
        // 12 amostras espaçadas de 2 s: 100 km/h e 8 L/h
        for (i in 1..12) rec.onSample(3000L + i * 2000L, andando(100f, 8f))
        val trip = ultima!!
        assertEquals(12.5f, trip.kmPerLiter!!, 0.01f)
    }

    @Test
    fun semFluxoDeArNaoHaConsumo() {
        val store = FakeStore()
        var ultima: Trip? = null
        val rec = TripRecorder(store) { if (it != null) ultima = it }
        for (t in listOf(0L, 1000L, 2000L, 3000L)) rec.onSample(t, andando(100f, null))
        for (i in 1..12) rec.onSample(3000L + i * 2000L, andando(100f, null))
        assertNull(ultima!!.kmPerLiter)
    }

    @Test
    fun velocidadeMedia() {
        val t = Trip(startMs = 0, endMs = 3_600_000, distanceKm = 50.0)
        assertEquals(50f, t.avgSpeed!!, 0.001f)
        assertNull(Trip(startMs = 0, endMs = 60_000).avgSpeed)
    }
}

class DtcTableTest {
    @Test
    fun descreveCodigoGenericoDaTabela() {
        assertNotNull(DtcTable.describe("P0133"))
        assertNotNull(DtcTable.describe("p0420"))
    }

    @Test
    fun naoInventaDescricao() {
        assertNull(DtcTable.describe("P1234"))
        assertNull(DtcTable.describe("U0100"))
        assertNull(DtcTable.describe("P0999"))
    }

    @Test
    fun tabelaTemEntradasValidas() {
        assertTrue(DtcTable.size > 50)
    }
}
