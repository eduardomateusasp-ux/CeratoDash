package br.com.ceratodash.ui

import android.content.Context
import android.content.DialogInterface
import android.content.pm.PackageManager
import android.graphics.Typeface
import android.hardware.usb.UsbManager
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import br.com.ceratodash.data.UsbDev
import br.com.ceratodash.data.UsbIface
import br.com.ceratodash.data.UsbReport

/** Diagnóstico de USB: mostra o que a porta da central enxerga (para avaliar o espelhamento por cabo). */
object UsbDialog {
    private fun report(activity: AppCompatActivity): String {
        val host = activity.packageManager.hasSystemFeature(PackageManager.FEATURE_USB_HOST)
        val mgr = activity.getSystemService(Context.USB_SERVICE) as? UsbManager
        val list = try {
            mgr?.deviceList?.values?.map { d ->
                val name = try {
                    d.productName
                } catch (e: Exception) {
                    null
                }
                val ifaces = (0 until d.interfaceCount).map { i ->
                    val f = d.getInterface(i)
                    UsbIface(f.interfaceClass, f.interfaceSubclass, f.interfaceProtocol)
                }
                UsbDev(d.vendorId, d.productId, name, ifaces)
            } ?: emptyList()
        } catch (e: Exception) {
            emptyList()
        }
        return UsbReport.format(list, host)
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
            addView(scroll, LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, activity.dp(320)))
        }
        val d = AlertDialog.Builder(activity)
            .setTitle("USB: o que a central enxerga")
            .setView(box)
            .setPositiveButton("Atualizar", null)
            .setNegativeButton("Fechar", null)
            .create()
        d.show()
        d.getButton(DialogInterface.BUTTON_POSITIVE).setOnClickListener { text.text = report(activity) }
        text.text = report(activity)
    }
}
