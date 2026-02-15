package com.example.copilotovirtual.utils

import android.content.Context
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import android.util.Log
import java.util.*

class TextToSpeechHelper(private val context: Context) {

    private var tts: TextToSpeech? = null
    private var isInitialized = false
    private val queuedMessages = mutableListOf<String>()

    init {
        Log.d("TTS", "Inicializando TextToSpeech...")
        tts = TextToSpeech(context) { status ->
            Log.d("TTS", "Estado de inicializacion: $status")

            if (status == TextToSpeech.SUCCESS) {
                tts?.let { engine ->
                    // Intentar español de Perú
                    var result = engine.setLanguage(Locale("es", "PE"))
                    Log.d("TTS", "Resultado setLanguage(es-PE): $result")

                    if (result == TextToSpeech.LANG_MISSING_DATA ||
                        result == TextToSpeech.LANG_NOT_SUPPORTED) {
                        // Fallback a español genérico
                        result = engine.setLanguage(Locale("es"))
                        Log.d("TTS", "Resultado setLanguage(es): $result")
                    }

                    engine.setPitch(1.0f)
                    engine.setSpeechRate(0.9f)

                    isInitialized = true
                    Log.d("TTS", "TTS inicializado correctamente")

                    // Procesar mensajes en cola
                    processQueuedMessages()

                    // Prueba inmediata
                    speak("Sistema de voz activado")
                }
            } else {
                Log.e("TTS", "Error al inicializar TTS: $status")
            }
        }

        tts?.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
            override fun onStart(utteranceId: String?) {
                Log.d("TTS", "Iniciando habla: $utteranceId")
            }

            override fun onDone(utteranceId: String?) {
                Log.d("TTS", "Completado: $utteranceId")
            }

            override fun onError(utteranceId: String?) {
                Log.e("TTS", "Error en habla: $utteranceId")
            }
        })
    }

    fun speak(text: String, queueMode: Int = TextToSpeech.QUEUE_ADD) {
        Log.d("TTS", "Intentando hablar: $text")

        if (isInitialized && tts != null) {
            val result = tts?.speak(
                text,
                queueMode,
                null,
                "utterance_${System.currentTimeMillis()}"
            )
            Log.d("TTS", "Resultado de speak(): $result")
        } else {
            Log.w("TTS", "TTS no inicializado, agregando a cola: $text")
            queuedMessages.add(text)
        }
    }

    private fun processQueuedMessages() {
        if (queuedMessages.isNotEmpty()) {
            Log.d("TTS", "Procesando ${queuedMessages.size} mensajes en cola")
            queuedMessages.forEach { message ->
                speak(message, TextToSpeech.QUEUE_ADD)
            }
            queuedMessages.clear()
        }
    }

    fun stop() {
        tts?.stop()
    }

    fun shutdown() {
        Log.d("TTS", "Apagando TTS")
        queuedMessages.clear()
        tts?.stop()
        tts?.shutdown()
        tts = null
        isInitialized = false
    }

    fun isSpeaking(): Boolean = tts?.isSpeaking ?: false
}