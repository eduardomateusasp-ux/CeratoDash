package br.com.ceratodash.obd

import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanResult
import android.bluetooth.le.ScanSettings
import android.os.Handler
import android.os.Looper

/**
 * Busca adaptadores Bluetooth LE por perto, dentro do app (adaptador só BLE não aparece na lista de
 * pareados). No Android 11 ou anterior a busca exige a permissão de localização e, em vários
 * aparelhos, a localização ligada. NÃO TESTADO na central.
 * onFound é chamado de novo a cada anúncio recebido (o sinal muda). onEnd traz null se terminou normalmente.
 */
class BleScan(
    private val onFound: (FoundDevice) -> Unit,
    private val onEnd: (problem: String?) -> Unit
) {
    private val handler = Handler(Looper.getMainLooper())
    private var scanner: android.bluetooth.le.BluetoothLeScanner? = null
    private var running = false

    private val callback = object : ScanCallback() {
        @SuppressLint("MissingPermission")
        override fun onScanResult(callbackType: Int, result: ScanResult) {
            val d = result.device ?: return
            val name = result.scanRecord?.deviceName ?: try {
                d.name
            } catch (e: SecurityException) {
                null
            }
            onFound(FoundDevice(d.address, name, result.rssi))
        }

        override fun onScanFailed(errorCode: Int) {
            finish("Falha na busca BLE (código $errorCode).")
        }
    }

    @SuppressLint("MissingPermission")
    fun start(durationMs: Long = 12000) {
        if (running) return
        val adapter = BluetoothAdapter.getDefaultAdapter()
        val sc = adapter?.bluetoothLeScanner
        if (adapter == null || !adapter.isEnabled || sc == null) {
            onEnd("Ligue o Bluetooth da central para buscar.")
            return
        }
        scanner = sc
        running = true
        try {
            sc.startScan(null, ScanSettings.Builder().setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY).build(), callback)
        } catch (e: Exception) {
            finish("Não consegui iniciar a busca: ${e.message ?: "erro desconhecido"}")
            return
        }
        handler.postDelayed({ finish(null) }, durationMs)
    }

    @SuppressLint("MissingPermission")
    fun stop() {
        if (!running) return
        running = false
        handler.removeCallbacksAndMessages(null)
        try {
            scanner?.stopScan(callback)
        } catch (_: Exception) {
        }
    }

    private fun finish(problem: String?) {
        if (!running) return
        stop()
        onEnd(problem)
    }
}
