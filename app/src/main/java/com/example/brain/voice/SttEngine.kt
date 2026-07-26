package com.example.brain.voice

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.util.Log
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SttEngine @Inject constructor(
    @ApplicationContext private val context: Context,
    private val audioCapture: AudioCapturePipeline,
    private val deltaUpdater: DeltaContextUpdater
) : RecognitionListener {

    private var speechRecognizer: SpeechRecognizer? = null
    private val recognizerIntent: Intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
        putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
        putExtra(RecognizerIntent.EXTRA_CALLING_PACKAGE, context.packageName)
        putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
        // Enable low-latency/streaming modes if supported
        putExtra("android.speech.extra.DICTATION_MODE", true)
    }

    private val _isListening = MutableStateFlow(false)
    val isListening = _isListening.asStateFlow()

    private val _partialText = MutableStateFlow("")
    val partialText = _partialText.asStateFlow()

    private val _finalText = MutableStateFlow("")
    val finalText = _finalText.asStateFlow()

    private val _deltaText = MutableStateFlow("")
    val deltaText = _deltaText.asStateFlow()

    // DeltaContextUpdater is now constructor-injected

    private var lastFullTranscript = ""

    init {
        if (SpeechRecognizer.isRecognitionAvailable(context)) {
            speechRecognizer = SpeechRecognizer.createSpeechRecognizer(context)
            speechRecognizer?.setRecognitionListener(this)
        }
    }

    fun startListening() {
        _partialText.value = ""
        _finalText.value = ""
        _deltaText.value = ""
        deltaUpdater.reset()
        lastFullTranscript = ""
        
        // Tier 1: System Recognition
        speechRecognizer?.startListening(recognizerIntent)
        
        // Start raw audio capture for potential Tier 2 (Offline Whisper) or VAD
        audioCapture.startCapture()
        
        _isListening.value = true
    }

    fun stopListening() {
        speechRecognizer?.stopListening()
        audioCapture.stopCapture()
        _isListening.value = false
    }

    fun cancel() {
        speechRecognizer?.cancel()
        audioCapture.stopCapture()
        _isListening.value = false
    }

    // RecognitionListener Implementations

    override fun onReadyForSpeech(params: Bundle?) {
        Log.d("SttEngine", "Ready for speech")
    }

    override fun onBeginningOfSpeech() {
        Log.d("SttEngine", "Beginning of speech")
    }

    override fun onRmsChanged(rmsdB: Float) {}

    override fun onBufferReceived(buffer: ByteArray?) {}

    override fun onEndOfSpeech() {
        _isListening.value = false
    }

    override fun onError(error: Int) {
        val errorMessage = when (error) {
            SpeechRecognizer.ERROR_AUDIO -> "Audio recording error"
            SpeechRecognizer.ERROR_CLIENT -> "Client side error"
            SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS -> "Insufficient permissions"
            SpeechRecognizer.ERROR_NETWORK -> "Network error"
            SpeechRecognizer.ERROR_NETWORK_TIMEOUT -> "Network timeout"
            SpeechRecognizer.ERROR_NO_MATCH -> "No match found"
            SpeechRecognizer.ERROR_RECOGNIZER_BUSY -> "Recognizer busy"
            SpeechRecognizer.ERROR_SERVER -> "Server error"
            SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> "No speech input"
            else -> "Unknown error"
        }
        Log.e("SttEngine", "Error: $errorMessage ($error)")
        _isListening.value = false
    }

    override fun onResults(results: Bundle?) {
        val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
        if (!matches.isNullOrEmpty()) {
            _finalText.value = matches[0]
            Log.i("SttEngine", "Final result: ${matches[0]}")
        }
    }

    override fun onPartialResults(partialResults: Bundle?) {
        val matches = partialResults?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
        if (!matches.isNullOrEmpty()) {
            val transcript = matches[0]
            _partialText.value = transcript
            val delta = deltaUpdater.getDelta(transcript)
            if (delta.isNotEmpty()) {
                _deltaText.value = delta
                Log.d("SttEngine", "Delta: $delta")
            }
        }
    }

    override fun onEvent(eventType: Int, params: Bundle?) {}

    fun shutdown() {
        speechRecognizer?.destroy()
    }
}
