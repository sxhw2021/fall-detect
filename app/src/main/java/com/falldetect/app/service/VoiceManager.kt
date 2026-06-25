package com.falldetect.app.service

import android.content.Context
import android.speech.tts.TextToSpeech
import java.util.Locale

class VoiceManager(private val context: Context) {
    private var tts: TextToSpeech? = null
    private var isInitialized = false

    fun initialize(onReady: () -> Unit = {}) {
        tts = TextToSpeech(context) { status ->
            if (status == TextToSpeech.SUCCESS) {
                tts?.language = Locale.CHINESE
                isInitialized = true
                onReady()
            }
        }
    }

    fun speak(text: String) {
        if (!isInitialized) {
            initialize { speak(text) }
            return
        }

        val params = android.os.Bundle().apply {
            putFloat(TextToSpeech.Engine.KEY_PARAM_VOLUME, 1.0f)
        }

        tts?.speak(text, TextToSpeech.QUEUE_FLUSH, params, "alarm_utterance")
    }

    fun stop() {
        tts?.stop()
    }

    fun shutdown() {
        tts?.stop()
        tts?.shutdown()
        tts = null
        isInitialized = false
    }
}