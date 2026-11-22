package br.com.ceratodash.data

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class DiagnosticoCentralTest {
    private fun info(
        ble: Boolean = true, wifi: Boolean = true, gps: Boolean = true, usb: Boolean = true,
        apps: List<Pair<String, Boolean>> = emptyList(), similar: List<String> = emptyList(),
        wifiOn: Boolean? = true, internet: Boolean? = false
    ) = DeviceInfo(
        manufacturer = "Fabricante", model = "Central", hardware = "mt8163", androidRelease = "12", sdk = 31,
        ramTotalMb = 1000, ramFreeMb = 350, widthPx = 1024, heightPx = 600, densityDpi = 160,
        bluetoothOn = true, bleFeature = ble, wifiFeature = wifi, wifiOn = wifiOn, internet = internet,
        gpsFeature = gps, locationOn = null, usbHost = usb, overlayAllowed = false,
        knownApps = apps, similarApps = similar
    )

    @Test
    fun mostraAparelhoMemoriaETela() {
        val t = DeviceReport.format(info())
        assertTrue(t.contains("Android 12 (API 31)"))
        assertTrue(t.contains("350 MB livres de 1000 MB"))
        assertTrue(t.contains("1024 x 600 px (1024 x 600 dp, 160 dpi)"))
    }

    @Test
    fun valorNaoInformadoNaoViraSimNemNao() {
        val t = DeviceReport.format(info())
        assertTrue(t.contains("localização ligada: não informado"))
    }

    @Test
    fun semBleAvisaQueAdaptadorBleNaoFunciona() {
        val c = DeviceReport.conclusions(info(ble = false))
        assertTrue(c.any { it.contains("não funcionam") && it.contains("Bluetooth LE") })
        assertFalse(DeviceReport.conclusions(info(ble = true)).any { it.contains("não funcionam") })
    }

    @Test
    fun semUsbAnfitriaoDescartaCarplayECabo() {
        val c = DeviceReport.conclusions(info(usb = false))
        assertTrue(c.any { it.contains("não tem USB anfitrião") })
    }

    @Test
    fun semWifiDescartaAirplay() {
        val c = DeviceReport.conclusions(info(wifi = false))
        assertTrue(c.any { it.contains("não tem Wi-Fi") })
    }

    @Test
    fun comPlayStoreEWifiSugereInstalarMaps() {
        val c = DeviceReport.conclusions(info(apps = listOf("Play Store" to true, "Google Maps" to false)))
        assertTrue(c.any { it.contains("instalar Maps ou Waze pela Play Store") })
    }

    @Test
    fun semPlayStoreSoComApk() {
        val c = DeviceReport.conclusions(info(apps = listOf("Play Store" to false, "Google Maps" to false)))
        assertTrue(c.any { it.contains("instalando APK") })
    }

    @Test
    fun mapasSemGpsAvisa() {
        val c = DeviceReport.conclusions(info(gps = false, apps = listOf("Play Store" to true)))
        assertTrue(c.any { it.contains("sem GPS") })
    }

    @Test
    fun listaAppsDeEspelhamentoEncontrados() {
        val t = DeviceReport.format(info(similar = listOf("ZLink (com.exemplo.zlink)")))
        assertTrue(t.contains("Possíveis apps de espelhamento ou CarPlay"))
        assertTrue(t.contains("ZLink (com.exemplo.zlink)"))
        assertTrue(DeviceReport.format(info()).contains("nenhum encontrado"))
    }
}