package com.example.brain.voice

import android.content.Context
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import android.os.Bundle
import android.util.Log
import com.example.persona.Persona
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TtsEngine @Inject constructor(
    @ApplicationContext private val context: Context
) : TextToSpeech.OnInitListener {

    private var tts: TextToSpeech? = null
    private var isInitialized = false

    private val _isSpeaking = MutableStateFlow(false)
    val isSpeaking = _isSpeaking.asStateFlow()

    init {
        tts = TextToSpeech(context, this)
    }

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            val result = tts?.setLanguage(Locale("en", "IN"))
            if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                Log.e("TtsEngine", "Language not supported")
            } else {
                isInitialized = true
                setupProgressListener()
            }
        } else {
            Log.e("TtsEngine", "Initialization failed")
        }
    }

    private fun setupProgressListener() {
        tts?.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
            override fun onStart(utteranceId: String?) {
                _isSpeaking.value = true
            }

            override fun onDone(utteranceId: String?) {
                _isSpeaking.value = false
            }

            override fun onError(utteranceId: String?) {
                _isSpeaking.value = false
            }
        })
    }

    enum class Emotion(val pitchMult: Float, val rateMult: Float, val pauseScale: Float) {
        NEUTRAL(1.0f, 1.0f, 1.0f),
        HAPPY(1.2f, 1.15f, 0.8f),
        SAD(0.8f, 0.75f, 1.5f),
        ANGRY(0.9f, 1.3f, 0.6f),
        URGENT(1.1f, 1.4f, 0.5f),
        CALM(0.95f, 0.85f, 1.2f),
        THOUGHTFUL(0.9f, 0.8f, 2.0f),
        APOLOGETIC(0.85f, 0.9f, 1.3f),
        WITTY(1.15f, 1.1f, 0.9f)
    }

    private data class Prosody(val pitch: Float, val rate: Float, val volume: Float = 1.0f)

    fun speak(text: String, persona: com.example.persona.Persona, emotionName: String = "NEUTRAL", userActivity: String = "IDLE") {
        if (!isInitialized) return

        val emotion = try {
            Emotion.valueOf(emotionName.uppercase())
        } catch (e: Exception) {
            Emotion.NEUTRAL
        }

        // 1. Prosody Computation (5.4: pure function logic)
        val prosody = computeProsody(persona, emotion, userActivity)
        
        tts?.setPitch(prosody.pitch)
        tts?.setSpeechRate(prosody.rate)
        
        // 2. Pause Insertion (5.4)
        val processedText = injectNaturalPauses(text, emotion)

        val params = Bundle().apply {
            putFloat(TextToSpeech.Engine.KEY_PARAM_VOLUME, prosody.volume)
        }

        Log.i("TtsEngine", "Speaking [$emotionName] | Persona: ${persona.name} | Activity: $userActivity | Pitch: ${prosody.pitch} | Rate: ${prosody.rate} | Vol: ${prosody.volume}")
        
        tts?.speak(processedText, TextToSpeech.QUEUE_FLUSH, params, "utterance_${System.currentTimeMillis()}")
    }

    private fun computeProsody(persona: com.example.persona.Persona, emotion: Emotion, activity: String): Prosody {
        // Base values from Persona Profile
        var basePitch = 1.0f
        var baseRate = 1.0f

        when (persona.name.uppercase()) {
            "ATLAS" -> { basePitch = 0.85f; baseRate = 0.95f }
            "NOVA" -> { basePitch = 1.15f; baseRate = 1.1f }
            "ONYX" -> { basePitch = 0.8f; baseRate = 1.2f }
            "SAGE" -> { basePitch = 0.9f; baseRate = 0.8f }
            "ECHO" -> { basePitch = 1.05f; baseRate = 1.15f }
        }

        var pitch = basePitch * emotion.pitchMult
        var rate = baseRate * emotion.rateMult
        var volume = 1.0f

        // 5.4 Activity-aware overrides
        when (activity.uppercase()) {
            "RUNNING", "EXERCISING" -> {
                rate *= 1.2f
                volume = 1.2f
            }
            "SLEEPING", "NIGHT" -> {
                // Whisper mode
                pitch *= 0.7f
                rate *= 0.8f
                volume = 0.4f
            }
            "MEETING" -> {
                // Should ideally be silent, handled by orchestrator, but here we damp it
                volume = 0.1f
            }
        }

        return Prosody(
            pitch = pitch.coerceIn(0.5f, 2.0f),
            rate = rate.coerceIn(0.5f, 2.0f),
            volume = volume.coerceIn(0.0f, 1.5f)
        )
    }

    private fun injectNaturalPauses(text: String, emotion: Emotion): String {
        // We simulate pauses using ellipses and punctuation based on emotion.pauseScale
        return when {
            emotion.pauseScale > 1.5f -> text.replace(", ", "... ").replace(". ", ".... ")
            emotion.pauseScale < 0.8f -> text.replace(", ", " ").replace("...", ".")
            else -> text
        }
    }

    fun stop() {
        tts?.stop()
        _isSpeaking.value = false
    }

    fun shutdown() {
        tts?.shutdown()
    }
}
