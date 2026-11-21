package br.com.ceratodash

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class PidsConsumoTest {

    private fun v(code: String, vararg b: Int) = Pids.byCode(code)!!.decode(b)

    @Test
    fun formulasDosSensores() {
        assertEquals(1726f, v("0C", 0x1A, 0xF8), 0.001f)
        assertEquals(50f, v("05", 90), 0.001f)
        assertEquals(-40f, v("0F", 0), 0.001f)
        assertEquals(100f, v("04", 255), 0.001f)
        assertEquals(0f, v("06", 128), 0.001f)
        assertEquals(-100f, v("06", 0), 0.001f)
        assertEquals(-64f, v("0E", 0), 0.001f)
        assertEquals(0f, v("0E", 128), 0.001f)
        assertEquals(12.5f, v("10", 0x04, 0xE2), 0.001f)
        assertEquals(14.222f, v("42", 0x37, 0x8E), 0.001f)
    }

    @Test
    fun aplicarNuloLimpaOCampo() {
        val t = Telemetry(rpm = 900f)
        assertNull(Pids.byCode("0C")!!.apply(t, null).rpm)
    }

    @Test
    fun consumoComMaf() {
        // 10 g/s * 3600 / (13,5 * 750) = 3,5556 L/h; a 60 km/h = 16,875 km/L
        val t = Consumption.apply(Telemetry(maf = 10f, speed = 60f), Fuel.GASOLINA)
        assertEquals(3.5556f, t.litersPerHour!!, 0.001f)
        assertEquals(16.875f, t.kmPerLiter!!, 0.01f)
    }

    @Test
    fun parado_naoCalculaKmPorLitro() {
        val t = Consumption.apply(Telemetry(maf = 2f, speed = 0f), Fuel.GASOLINA)
        assertNotNull(t.litersPerHour)
        assertNull(t.kmPerLiter)
    }

    @Test
    fun semMaf_naoInventaValor() {
        val t = Consumption.apply(Telemetry(speed = 80f, litersPerHour = 5f, kmPerLiter = 10f), Fuel.ETANOL)
        assertNull(t.litersPerHour)
        assertNull(t.kmPerLiter)
    }

    @Test
    fun etanolGastaMaisQueGasolinaParaOMesmoAr() {
        val g = Consumption.apply(Telemetry(maf = 10f, speed = 60f), Fuel.GASOLINA)
        val e = Consumption.apply(Telemetry(maf = 10f, speed = 60f), Fuel.ETANOL)
        assertTrue(e.litersPerHour!! > g.litersPerHour!!)
    }
}
