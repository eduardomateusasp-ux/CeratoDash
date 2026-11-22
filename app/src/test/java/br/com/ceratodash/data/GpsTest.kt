package br.com.ceratodash.data

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class GpsTest {
    private fun st(
        present: Boolean = true, on: Boolean = true, visible: Int = 0, used: Int = 0,
        acc: Float? = null, speed: Float? = null, ttff: Int? = null, elapsed: Int = 5
    ) = GpsState(present, on, visible, used, acc, speed, ttff, elapsed)

    @Test
    fun semProvedorDeGps() {
        assertTrue(GpsReport.format(st(present = false)).contains("não tem provedor de GPS"))
    }

    @Test
    fun localizacaoDesligada() {
        assertTrue(GpsReport.format(st(on = false)).contains("localização está desligada"))
    }

    @Test
    fun procurandoNoComeco() {
        val t = GpsReport.format(st(elapsed = 10))
        assertTrue(t.contains("Procurando satélites"))
        assertTrue(t.contains("Posição: ainda não"))
    }

    @Test
    fun semSatelitesEm1MinutoSugereAntena() {
        val t = GpsReport.format(st(elapsed = 61))
        assertTrue(t.contains("nenhum satélite em 1 minuto"))
        assertTrue(t.contains("antena"))
    }

    @Test
    fun veSatelitesMasSemPosicao() {
        val t = GpsReport.format(st(visible = 6, used = 0, elapsed = 30))
        assertTrue(t.contains("Satélites vistos: 6, usados na posição: 0"))
        assertTrue(t.contains("ainda sem posição"))
    }

    @Test
    fun comPosicaoEstaFuncionando() {
        val t = GpsReport.format(st(visible = 12, used = 8, acc = 7.6f, speed = 42.9f, ttff = 28, elapsed = 40))
        assertTrue(t.contains("precisão de cerca de 7 m"))
        assertTrue(t.contains("Velocidade pelo GPS: 42 km/h"))
        assertTrue(t.contains("Tempo até a primeira posição: 28 s"))
        assertTrue(t.contains("GPS da central está funcionando"))
    }

    @Test
    fun naoMostraCoordenadas() {
        val t = GpsReport.format(st(visible = 9, used = 7, acc = 5f))
        assertFalse(t.contains("latitude", ignoreCase = true))
        assertFalse(t.contains("longitude", ignoreCase = true))
    }

    @Test
    fun diagnosticoDizQueGpsDeclaradoNaoProvaNada() {
        val i = DeviceInfo(
            "F", "M", "h", "12", 31, 1000, 300, 1024, 600, 160, true, true, true, true, false, true, null, true, false,
            emptyList(), emptyList()
        )
        assertTrue(DeviceReport.conclusions(i).any { it.contains("Testar GPS") })
    }
}