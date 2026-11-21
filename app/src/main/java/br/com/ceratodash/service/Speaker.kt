package br.com.ceratodash.service

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioManager
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import java.util.Locale

/**
 * Voz dos alertas, pelo mecanismo de voz (TextToSpeech) do próprio Android. Funciona sem internet
 * se a central tiver uma voz em português instalada; o app não sabe disso de antemão, então testa
 * ao iniciar e guarda o motivo em `problem`.
 * O volume da música abaixa enquanto o app fala (foco de áudio transitório).
 */
object Speaker {
    private var tts: TextToSpeech? = null
    private var app: Context? = null
    private val pending = ArrayList<String>()

    @Volatile
    var ready = false
        private set

    /** Motivo de a voz não estar disponível, em português; nulo se está pronta ou ainda iniciando. */
    @Volatile
    var problem: String? = null
        private set

    @Synchronized
    fun init(context: Context) {
        if (tts != null) return
        val c = context.applicationContext
        app = c
        tts = TextToSpeech(c) { status ->
            val engine = tts
            if (status != TextToSpeech.SUCCESS || engine == null) {
                problem = "A central não tem mecanismo de voz instalado."
                return@TextToSpeech
            }
            val r = engine.setLanguage(Locale("pt", "BR"))
            if (r == TextToSpeech.LANG_MISSING_DATA || r == TextToSpeech.LANG_NOT_SUPPORTED) {
                problem = "A central não tem voz em português. Instale uma nas configurações de voz do Android."
                return@TextToSpeech
            }
            engine.setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_ASSISTANCE_NAVIGATION_GUIDANCE)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                    .build()
            )
            engine.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
                override fun onStart(utteranceId: String?) {}
                override fun onDone(utteranceId: String?) = release()

                @Deprecated("Deprecated in Java")
                override fun onError(utteranceId: String?) = release()
            })
            problem = null
            ready = true
            synchronized(this) {
                for (p in pending) say(p)
                pending.clear()
            }
        }
    }

    @Suppress("DEPRECATION")
    private fun say(text: String) {
        val c = app ?: return
        val am = c.getSystemService(Context.AUDIO_SERVICE) as AudioManager
        am.requestAudioFocus(null, AudioManager.STREAM_MUSIC, AudioManager.AUDIOFOCUS_GAIN_TRANSIENT_MAY_DUCK)
        tts?.speak(text, TextToSpeech.QUEUE_ADD, null, "ceratodash")
    }

    @Suppress("DEPRECATION")
    private fun release() {
        val c = app ?: return
        (c.getSystemService(Context.AUDIO_SERVICE) as AudioManager).abandonAudioFocus(null)
    }

    /** Fala (ou guarda para falar quando a voz ficar pronta). Sem voz disponível, não faz nada. */
    @Synchronized
    fun speak(text: String) {
        if (tts == null || problem != null) return
        if (ready) say(text) else if (pending.size < 3) pending.add(text)
    }

    fun shutdown() {
        synchronized(this) {
            tts?.stop()
            tts?.shutdown()
            tts = null
            ready = false
            pending.clear()
        }
    }
}
