package br.com.ceratodash.ui

import android.app.ActivityManager
import android.bluetooth.BluetoothAdapter
import android.content.Context
import android.content.DialogInterface
import android.content.pm.PackageManager
import android.location.LocationManager
import android.net.ConnectivityManager
import android.net.wifi.WifiManager
import android.os.Build
import android.provider.Settings
import android.graphics.Typeface
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import br.com.ceratodash.data.DeviceInfo
import br.com.ceratodash.data.DeviceReport

/** Diagnóstico da central: coleta o que o Android informa e mostra um resumo (e o que ele permite). */
object DeviceDialog {
    private val known = listOf(
        "Google Maps" to "com.google.android.apps.maps",
        "Waze" to "com.waze",
        "Organic Maps" to "app.organicmaps",
        "OsmAnd" to "net.osmand",
        "Play Store" to "com.android.vending",
        "Serviços do Google" to "com.google.android.gms",
        "Android Auto" to "com.google.android.projection.gearhead"
    )
    private val keywords = listOf("zlink", "easyconn", "carplay", "carlink", "mirror", "espelh", "autokit", "airplay", "miracast", "cast", "wifidisplay")

    private fun installed(pm: PackageManager, pkg: String): Boolean = try {
        pm.getPackageInfo(pkg, 0)
        true
    } catch (e: Exception) {
        false
    }

    private fun <T> safe(block: () -> T): T? = try {
        block()
    } catch (e: Exception) {
        null
    }

    @Suppress("DEPRECATION")
    fun collect(activity: AppCompatActivity): DeviceInfo {
        val pm = activity.packageManager
        val am = activity.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        val mi = ActivityManager.MemoryInfo()
        am.getMemoryInfo(mi)
        val dm = activity.resources.displayMetrics
        val bt = safe { BluetoothAdapter.getDefaultAdapter()?.isEnabled }
        val wifiMgr = activity.applicationContext.getSystemService(Context.WIFI_SERVICE) as? WifiManager
        val conn = activity.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
        val loc = activity.getSystemService(Context.LOCATION_SERVICE) as? LocationManager
        val similar = ArrayList<String>()
        safe {
            for (app in pm.getInstalledApplications(0)) {
                if (app.packageName == activity.packageName) continue
                val label = pm.getApplicationLabel(app).toString()
                val hay = (label + " " + app.packageName).lowercase()
                if (keywords.any { it in hay }) similar.add("$label (${app.packageName})")
            }
        }
        return DeviceInfo(
            manufacturer = Build.MANUFACTURER ?: "?",
            model = Build.MODEL ?: "?",
            hardware = Build.HARDWARE ?: "?",
            androidRelease = Build.VERSION.RELEASE ?: "?",
            sdk = Build.VERSION.SDK_INT,
            ramTotalMb = mi.totalMem / (1024 * 1024),
            ramFreeMb = mi.availMem / (1024 * 1024),
            widthPx = dm.widthPixels,
            heightPx = dm.heightPixels,
            densityDpi = dm.densityDpi,
            bluetoothOn = bt,
            bleFeature = pm.hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE),
            wifiFeature = pm.hasSystemFeature(PackageManager.FEATURE_WIFI),
            wifiOn = safe { wifiMgr?.isWifiEnabled },
            internet = safe { conn?.activeNetworkInfo?.isConnected ?: false },
            gpsFeature = pm.hasSystemFeature(PackageManager.FEATURE_LOCATION_GPS),
            locationOn = safe { loc?.isProviderEnabled(LocationManager.GPS_PROVIDER) == true || loc?.isProviderEnabled(LocationManager.NETWORK_PROVIDER) == true },
            usbHost = pm.hasSystemFeature(PackageManager.FEATURE_USB_HOST),
            overlayAllowed = if (Build.VERSION.SDK_INT >= 23) safe { Settings.canDrawOverlays(activity) } else true,
            knownApps = known.map { it.first to installed(pm, it.second) },
            similarApps = similar.sorted().take(15)
        )
    }

    fun show(activity: AppCompatActivity) {
        val text = TextView(activity).apply {
            setTextColor(Theme.TEXT)
            textSize = Theme.MIN_SIZE
            typeface = Typeface.MONOSPACE
            setTextIsSelectable(true)
        }
        val scroll = ScrollView(activity)
        scroll.addView(text)
        val box = LinearLayout(activity).apply {
            setPadding(activity.dp(16), activity.dp(8), activity.dp(16), activity.dp(8))
            addView(scroll, LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, activity.dp(380)))
        }
        val d = AlertDialog.Builder(activity)
            .setTitle("Diagnóstico da central")
            .setView(box)
            .setPositiveButton("Atualizar", null)
            .setNegativeButton("Fechar", null)
            .create()
        d.show()
        d.getButton(DialogInterface.BUTTON_POSITIVE).setOnClickListener { text.text = DeviceReport.format(collect(activity)) }
        text.text = DeviceReport.format(collect(activity))
    }
}
