package br.com.ceratodash.obd

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class BluetoothLeTest {

    @Test
    fun enderecoDigitadoEhNormalizado() {
        assertEquals("AA:BB:CC:DD:EE:FF", BleAddress.normalize("AA:BB:CC:DD:EE:FF"))
        assertEquals("AA:BB:CC:DD:EE:FF", BleAddress.normalize("aa:bb:cc:dd:ee:ff"))
        assertEquals("AA:BB:CC:DD:EE:FF", BleAddress.normalize("  aa-bb-cc-dd-ee-ff "))
        assertEquals("AA:BB:CC:DD:EE:FF", BleAddress.normalize("AABBCCDDEEFF"))
        assertEquals("00:1D:A5:68:98:8B", BleAddress.normalize("00 1D A5 68 98 8B"))
    }

    @Test
    fun enderecoInvalidoEhRecusado() {
        assertNull(BleAddress.normalize(""))
        assertNull(BleAddress.normalize("AA:BB:CC:DD:EE"))
        assertNull(BleAddress.normalize("AA:BB:CC:DD:EE:GG"))
        assertNull(BleAddress.normalize("AA:BB:CC:DD:EE:FF:11"))
        assertNull(BleAddress.normalize("OBDII"))
    }

    @Test
    fun aBuscaMostraPrimeiroOsComNomeEOMaisForte() {
        val lista = listOf(
            FoundDevice("11:11:11:11:11:11", null, -40),
            FoundDevice("22:22:22:22:22:22", "OBDII", -80),
            FoundDevice("33:33:33:33:33:33", "vLinker", -55),
            FoundDevice("44:44:44:44:44:44", "", -30)
        )
        val ordem = FoundDevice.sorted(lista).map { it.address }
        assertEquals(listOf("33:33:33:33:33:33", "22:22:22:22:22:22", "44:44:44:44:44:44", "11:11:11:11:11:11"), ordem)
    }

    @Test
    fun formaCurtaDosUuids() {
        assertEquals("fff0", SerialServices.short("0000FFF0-0000-1000-8000-00805F9B34FB"))
        assertEquals("ffe0", SerialServices.short("0000ffe0-0000-1000-8000-00805f9b34fb"))
        assertEquals("6e400001", SerialServices.short("6E400001-B5A3-F393-E0A9-E50E24DCCA9E"))
    }

    @Test
    fun servicosConhecidosTemPreferencia() {
        val fff0 = "0000fff0-0000-1000-8000-00805f9b34fb"
        val ffe0 = "0000ffe0-0000-1000-8000-00805f9b34fb"
        val desconhecido = "12345678-0000-1000-8000-00805f9b34fb"
        assertTrue(SerialServices.rank(fff0) < SerialServices.rank(ffe0))
        assertTrue(SerialServices.rank(ffe0) < SerialServices.rank(desconhecido))
    }

    @Test
    fun servicosGenericosSaoIgnorados() {
        assertTrue(SerialServices.isIgnored("00001800-0000-1000-8000-00805f9b34fb"))
        assertTrue(SerialServices.isIgnored("00001801-0000-1000-8000-00805f9b34fb"))
        assertTrue(SerialServices.isIgnored("0000180a-0000-1000-8000-00805f9b34fb"))
        assertTrue(!SerialServices.isIgnored("0000fff0-0000-1000-8000-00805f9b34fb"))
    }

    @Test
    fun ordemDeTentativasDeConexao() {
        assertEquals(listOf(true), linkOrder(Transport.BLE, false))
        assertEquals(listOf(false), linkOrder(Transport.CLASSIC, true))
        assertEquals(listOf(false, true), linkOrder(Transport.AUTO, false))
        assertEquals(listOf(true, false), linkOrder(Transport.AUTO, true))
    }
}
