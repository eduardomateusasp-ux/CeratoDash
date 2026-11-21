package br.com.ceratodash.service

import br.com.ceratodash.R
import br.com.ceratodash.data.*
import br.com.ceratodash.obd.*
import br.com.ceratodash.service.*
import br.com.ceratodash.ui.*

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

/**
 * Serviço em primeiro plano dono da conexão com o adaptador. A leitura continua quando outro app
 * (Google Maps, Waze) está na frente. A notificação é discreta (prioridade baixa, sem som).
 */
class ObdService : Service() {

    companion object {
        const val ACTION_START = "br.com.ceratodash.START"
        const val ACTION_STOP = "br.com.ceratodash.STOP"
        private const val CHANNEL = "leitura"
        private const val NOTIF_ID = 1

        /** Inicia (ou reinicia) a conexão com o adaptador salvo. */
        fun start(c: Context) {
            ContextCompat.startForegroundService(
                c, Intent(c, ObdService::class.java).setAction(ACTION_START)
            )
        }

        fun stop(c: Context) {
            c.startService(Intent(c, ObdService::class.java).setAction(ACTION_STOP))
        }
    }

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private val ioScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private lateinit var nm: NotificationManager
    private var recorder: TripRecorder? = null

    override fun onCreate() {
        super.onCreate()
        nm = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        if (Build.VERSION.SDK_INT >= 26) {
            nm.createNotificationChannel(
                NotificationChannel(CHANNEL, "Leitura do carro", NotificationManager.IMPORTANCE_LOW).apply {
                    setSound(null, null)
                    enableVibration(false)
                    setShowBadge(false)
                }
            )
        }
        startForeground(NOTIF_ID, notification(ObdStatus(ConnState.CONECTANDO, "")))
        scope.launch {
            ObdManager.status.collect { nm.notify(NOTIF_ID, notification(it)) }
        }
        startRecording()
    }

    /** Amostra a telemetria a cada 500 ms para o histórico dos gráficos e para as viagens. */
    private fun startRecording() {
        val db = TripDb.get(this)
        val rec = TripRecorder(db) { trip ->
            TripLive.current.value = trip
            if (trip == null) TripLive.version.value += 1
        }
        recorder = rec
        ioScope.launch {
            // Viagem que ficou aberta porque o app foi encerrado de repente.
            db.closeOpen()
            var last = ConnState.DESCONECTADO
            while (isActive) {
                delay(500)
                val now = System.currentTimeMillis()
                val state = ObdManager.status.value.state
                if (state == ConnState.CONECTADO && last != ConnState.CONECTADO) LiveHistory.history.reset()
                last = state
                val t = ObdManager.telemetry.value
                try {
                    rec.onSample(now, t)
                } catch (e: Exception) {
                    // Falha no banco não pode derrubar a leitura do carro.
                }
                LiveHistory.history.add(now, t)
            }
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_STOP) {
            ObdManager.stop()
            stopForeground(true)
            stopSelf()
            return START_NOT_STICKY
        }
        // Também vale para o reinício feito pelo sistema (intent nulo).
        ObdManager.fuel = Prefs.fuel(this)
        val addr = Prefs.adapter(this)
        if (addr == null) {
            ObdManager.status.value =
                ObdStatus(ConnState.DESCONECTADO, "toque em Ajustes e escolha o adaptador")
        } else {
            ObdManager.start(this, addr)
        }
        return START_STICKY
    }

    override fun onDestroy() {
        ioScope.cancel()
        try {
            recorder?.finish(System.currentTimeMillis())
        } catch (e: Exception) {
        }
        scope.cancel()
        ObdManager.stop()
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun notification(s: ObdStatus): Notification {
        val open = PendingIntent.getActivity(
            this, 0, Intent(this, MainActivity::class.java), PendingIntent.FLAG_IMMUTABLE
        )
        val stop = PendingIntent.getService(
            this, 1, Intent(this, ObdService::class.java).setAction(ACTION_STOP), PendingIntent.FLAG_IMMUTABLE
        )
        val text = if (s.message.isBlank()) s.state.label else "${s.state.label}: ${s.message}"
        return NotificationCompat.Builder(this, CHANNEL)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle("CeratoDash")
            .setContentText(text)
            .setContentIntent(open)
            .addAction(0, "Parar", stop)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setSilent(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setCategory(NotificationCompat.CATEGORY_SERVICE)
            .build()
    }
}
