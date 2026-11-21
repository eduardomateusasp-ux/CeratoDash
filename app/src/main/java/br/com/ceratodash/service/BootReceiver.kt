package br.com.ceratodash.service

import br.com.ceratodash.data.*
import br.com.ceratodash.obd.*
import br.com.ceratodash.service.*
import br.com.ceratodash.ui.*

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

/**
 * Inicia a leitura junto com o sistema da central, só se a opção estiver ligada nos Ajustes.
 * Só o serviço sobe: abrir a tela sozinho pelo segundo plano o Android 10 ou mais novo bloqueia.
 */
class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val a = intent.action
        if ((a == Intent.ACTION_BOOT_COMPLETED || a == "android.intent.action.QUICKBOOT_POWERON") &&
            Prefs.autostart(context)
        ) {
            ObdService.start(context)
        }
    }
}
