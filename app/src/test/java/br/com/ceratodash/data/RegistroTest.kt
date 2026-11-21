package br.com.ceratodash.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class RegistroTest {
    @Test
    fun comecaVazio() {
        ConnLog.clear()
        assertEquals("Nenhum registro ainda.", ConnLog.text())
    }

    @Test
    fun guardaAsLinhasEmOrdemComHorario() {
        ConnLog.clear()
        ConnLog.add("primeira")
        ConnLog.add("segunda")
        val linhas = ConnLog.text().lines()
        assertEquals(2, linhas.size)
        assertTrue(linhas[0].endsWith(" primeira"))
        assertTrue(linhas[1].endsWith(" segunda"))
        assertTrue(Regex("^\\d{2}:\\d{2}:\\d{2} ").containsMatchIn(linhas[0]))
    }

    @Test
    fun guardaSoAsUltimasLinhas() {
        ConnLog.clear()
        for (i in 1..(ConnLog.MAX_LINES + 30)) ConnLog.add("linha $i")
        assertEquals(ConnLog.MAX_LINES, ConnLog.size())
        val texto = ConnLog.text()
        assertFalse(texto.contains("linha 1\n"))
        assertTrue(texto.endsWith("linha ${ConnLog.MAX_LINES + 30}"))
    }

    @Test
    fun limparEsvazia() {
        ConnLog.add("x")
        ConnLog.clear()
        assertEquals(0, ConnLog.size())
    }
}