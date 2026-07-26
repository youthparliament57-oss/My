package com.example.brain.voice

import android.util.Log
import com.example.brain.BrainFacade
import com.example.persona.Persona
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.isActive
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class VoiceOrchestrator @Inject constructor(
    private val sttEngine: SttEngine,
    private val ttsEngine: TtsEngine,
    private val wakeWordDetector: WakeWordDetector,
    private val brainFacade: BrainFacade,
    private val conversationEngine: ConversationEngine,
    private val audioCapture: AudioCapturePipeline,
    private val speakerRecognizer: SpeakerRecognizer
) {
    private val scope = CoroutineScope(Dispatchers.Main + Job())
    private var activePersona: com.example.persona.Persona = com.example.persona.PersonaDefinitions.ATLAS
    private var isOwnerVerified = true

    private val _voiceState = MutableStateFlow(VoiceState.IDLE)
    val voiceState = _voiceState.asStateFlow()

    enum class VoiceState {
        IDLE, LISTENING, THINKING, SPEAKING
    }

    private val accumulatedAudio = mutableListOf<ByteArray>()
    private var monitorJob: kotlinx.coroutines.Job? = null

    init {
        scope.launch {
            audioCapture.audioChunks.collect { chunk ->
                if (_voiceState.value == VoiceState.LISTENING) {
                    accumulatedAudio.add(chunk)
                } else if (_voiceState.value == VoiceState.IDLE) {
                    accumulatedAudio.clear()
                }
            }
        }

        scope.launch {
            wakeWordDetector.onWakeWordDetected.collect {
                onWakeWordTriggered()
            }
        }

        scope.launch {
            sttEngine.deltaText.collect { delta ->
                if (delta.isNotEmpty() && _voiceState.value == VoiceState.LISTENING) {
                    Log.d("VoiceOrchestrator", "Streaming delta to Brain context: $delta")
                    // In a full implementation, this would update the LLM's prompt prefix
                }
            }
        }

        scope.launch {
            sttEngine.finalText.collect { text ->
                if (text.isNotEmpty()) {
                    onSpeechCaptured(text)
                }
            }
        }

        scope.launch {
            ttsEngine.isSpeaking.collect { isSpeaking ->
                if (isSpeaking) {
                    _voiceState.value = VoiceState.SPEAKING
                    ttsEngine.stop()
                    // Start audio capture for barge-in if not already active
                    audioCapture.startCapture()
                } else if (_voiceState.value == VoiceState.SPEAKING) {
                    _voiceState.value = VoiceState.IDLE
                    audioCapture.stopCapture()
                    wakeWordDetector.startDetection()
                }
            }
        }

        // Barge-in & EOS Detection (5.8)
        var silenceStart: Long = 0
        scope.launch {
            audioCapture.onVoiceDetected.collect { hasVoice ->
                if (hasVoice) {
                    silenceStart = 0
                    if (_voiceState.value == VoiceState.SPEAKING) {
                        Log.i("VoiceOrchestrator", "Barge-in detected! Stopping TTS.")
                        onWakeWordTriggered()
                    }
                } else {
                    silenceStart = System.currentTimeMillis()
                }
            }
        }

        // EOS Monitor Loop
        monitorJob = scope.launch {
            while (isActive) {
                if (_voiceState.value == VoiceState.LISTENING && silenceStart > 0) {
                    val silenceDuration = System.currentTimeMillis() - silenceStart
                    if (silenceDuration > 800) { // 800ms silence threshold for EOS (5.8 mentions 500ms + VAP)
                        Log.i("VoiceOrchestrator", "EOS detected via 800ms silence. Stopping STT.")
                        sttEngine.stopListening()
                        _voiceState.value = VoiceState.THINKING
                        silenceStart = 0
                    }
                }
                delay(100)
            }
        }
    }

    fun startVoiceMode() {
        wakeWordDetector.startDetection()
    }

    private fun onWakeWordTriggered() {
        Log.i("VoiceOrchestrator", "Wake word or Barge-in triggered. Transitioning to LISTENING.")
        ttsEngine.stop()
        sttEngine.startListening()
        _voiceState.value = VoiceState.LISTENING
    }

    private fun onSpeechCaptured(text: String) {
        Log.i("VoiceOrchestrator", "Speech captured: $text. Transitioning to THINKING.")
        _voiceState.value = VoiceState.THINKING
        
        // Combine audio and verify speaker
        val fullAudioSize = accumulatedAudio.sumOf { it.size }
        val fullBuffer = ByteArray(fullAudioSize)
        var offset = 0
        for (chunk in accumulatedAudio) {
            System.arraycopy(chunk, 0, fullBuffer, offset, chunk.size)
            offset += chunk.size
        }
        isOwnerVerified = speakerRecognizer.verify(fullBuffer)
        accumulatedAudio.clear()
        
        // Play filler to manage user expectation
        conversationEngine.playFiller(activePersona)
        
        scope.launch(Dispatchers.IO) {
            try {
                val response = brainFacade.processQuery(text)
                kotlinx.coroutines.withContext(Dispatchers.Main) {
                    _voiceState.value = VoiceState.SPEAKING
                    ttsEngine.stop()
                    ttsEngine.speak(
                        text = response.rawText,
                        persona = activePersona,
                        emotionName = response.detectedEmotion
                    )
                }
            } catch (e: Exception) {
                Log.e("VoiceOrchestrator", "Failed to process query", e)
                kotlinx.coroutines.withContext(Dispatchers.Main) {
                    _voiceState.value = VoiceState.IDLE
                }
            }
        }
    }

    fun stopEverything() {
        monitorJob?.cancel()
        sttEngine.stopListening()
        ttsEngine.stop()
        wakeWordDetector.stopDetection()
        _voiceState.value = VoiceState.IDLE
    }
}
