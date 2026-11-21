package br.com.ceratodash.data

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class UsbTest {
    private val adb = UsbIface(255, 66, 1)
    private val storage = UsbIface(8, 6, 80)

    @Test
    fun reconheceInterfaceAdb() {
        assertTrue(adb.isAdb)
        assertFalse(storage.isAdb)
        assertFalse(UsbIface(255, 66, 2).isAdb)
    }

    @Test
    fun semSuporteAAnfitriaoDizIsso() {
        val t = UsbReport.format(emptyList(), hostSupported = false)
        assertTrue(t.contains("não suporta USB como anfitrião"))
    }

    @Test
    fun semAparelhosOrientaOUsuario() {
        val t = UsbReport.format(emptyList(), hostSupported = true)
        assertTrue(t.contains("Nenhum aparelho USB visto"))
    }

    @Test
    fun celularComDepuracaoAdbEViavel() {
        val t = UsbReport.format(listOf(UsbDev(0x18D1, 0x4EE2, "Pixel", listOf(UsbIface(6, 1, 1), adb))), true)
        assertTrue(t.contains("18D1:4EE2"))
        assertTrue(t.contains("depuração ADB"))
        assertTrue(t.contains("parece possível"))
    }

    @Test
    fun celularSemAdbPedeDepuracaoUsb() {
        val t = UsbReport.format(listOf(UsbDev(0x04E8, 0x6860, null, listOf(storage))), true)
        assertTrue(t.contains("sem nome"))
        assertTrue(t.contains("sem a interface de depuração"))
        assertFalse(t.contains("parece possível"))
    }
}