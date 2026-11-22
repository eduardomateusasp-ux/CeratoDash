package br.com.ceratodash.ui

import android.annotation.SuppressLint
import android.content.Context
import android.content.DialogInterface
import android.graphics.Typeface
import android.location.GnssStatus
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import br.com.ceratodash.data.GpsReport
import br.com.ceratodash.data.GpsState

/**
 * Teste de GPS: pede posição ao GPS da central e mostra os satélites vistos, a precisão e o tempo até a
 * primeira posição. Precisa da permissão de localização (já conferida por quem chama). As coordenadas não
 * são mostradas nem guardadas. NÃO TESTADO na central.
 */
object GpsDialog {
    @SuppressLint("MissingPermission")
    fun show(activity: AppCompatActivity) {
        val lm = activity.getSystemService(Context.LOCATION_SERVICE) as? LocationManager
        val text = TextView(activity).apply {
            setTextColor(Theme.TEXT)
            textSize = Theme.MIN_SIZE
            typeface = Typeface.MONOSPACE
        }
        val scroll = ScrollView(activity)
        scroll.addView(text)
        val box = LinearLayout(activity).apply {
            setPadding(activity.dp(16), activity.dp(8), activity.dp(16), activity.dp(8))
            addView(scroll, LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, activity.dp(300)))
        }
        val start = SystemClock.elapsedRealtime()
        val handler = Handler(Looper.getMainLooper())
        var visible = 0
        var used = 0
        var accuracy: Float? = null
        var speed: Float? = null
        var ttff: Int? = null

        fun render() {
            val present = try {
                lm?.allProviders?.contains(LocationManager.GPS_PROVIDER) == true
            } catch (e: Exception) {
                false
            }
            val on = try {
                lm?.isProviderEnabled(LocationManager.GPS_PROVIDER) == true
            } catch (e: Exception) {
                false
            }
            text.text = GpsReport.format(
                GpsState(present, on, visible, used, accuracy, speed, ttff, ((SystemClock.elapsedRealtime() - start) / 1000).toInt())
            )
        }

        val listener = object : LocationListener {
            override fun onLocationChanged(l: Location) {
                accuracy = if (l.hasAccuracy()) l.accuracy else 0f
                speed = if (l.hasSpeed()) l.speed * 3.6f else null
                render()
            }

            @Deprecated("Deprecated in Java")
            override fun onStatusChanged(provider: String?, status: Int, extras: Bundle?) {}
            override fun onProviderEnabled(provider: String) = render()
            override fun onProviderDisabled(provider: String) = render()
        }
        val gnss: GnssStatus.Callback? = if (Build.VERSION.SDK_INT >= 24) object : GnssStatus.Callback() {
            override fun onSatelliteStatusChanged(status: GnssStatus) {
                visible = status.satelliteCount
                used = (0 until status.satelliteCount).count { status.usedInFix(it) }
                render()
            }

            override fun onFirstFix(ttffMillis: Int) {
                ttff = ttffMillis / 1000
                render()
            }
        } else null

        fun begin() {
            try {
                lm?.requestLocationUpdates(LocationManager.GPS_PROVIDER, 1000L, 0f, listener)
                if (gnss != null && Build.VERSION.SDK_INT >= 24) lm?.registerGnssStatusCallback(gnss, handler)
            } catch (e: Exception) {
                // Sem provedor ou sem permissão: o texto de render() explica.
            }
        }

        fun end() {
            handler.removeCallbacksAndMessages(null)
            try {
                lm?.removeUpdates(listener)
                if (gnss != null && Build.VERSION.SDK_INT >= 24) lm?.unregisterGnssStatusCallback(gnss)
            } catch (_: Exception) {
            }
        }

        fun tick() {
            render()
            handler.postDelayed({ tick() }, 1000)
        }

        val d = AlertDialog.Builder(activity)
            .setTitle("Teste de GPS")
            .setView(box)
            .setPositiveButton("Atualizar", null)
            .setNegativeButton("Fechar", null)
            .create()
        d.setOnDismissListener { end() }
        d.show()
        d.getButton(DialogInterface.BUTTON_POSITIVE).setOnClickListener { render() }
        begin()
        tick()
    }
}
