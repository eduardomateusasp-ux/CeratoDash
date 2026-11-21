package br.com.ceratodash.data

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class FaixaSobreMapaTest {
    @Test
    fun apareceSoComTudoCerto() {
        assertTrue(OverlayRules.shouldShow(enabled = true, canDraw = true, appVisible = false))
    }

    @Test
    fun desligadaNaoAparece() {
        assertFalse(OverlayRules.shouldShow(enabled = false, canDraw = true, appVisible = false))
    }

    @Test
    fun semPermissaoNaoForca() {
        assertFalse(OverlayRules.shouldShow(enabled = true, canDraw = false, appVisible = false))
    }

    @Test
    fun comOPainelNaTelaNaoRepete() {
        assertFalse(OverlayRules.shouldShow(enabled = true, canDraw = true, appVisible = true))
    }
}
