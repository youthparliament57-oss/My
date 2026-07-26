package com.example.brain.voice

import android.annotation.SuppressLint
import android.content.Context
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import android.media.audiofx.AcousticEchoCanceler
import android.media.audiofx.AutomaticGainControl
import android.media.audiofx.NoiseSuppressor
import android.util.Log
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.sqrt

@Singleton
class AudioCapturePipeline @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val scope = CoroutineScope(Dispatchers.IO + Job())
    
    private val sampleRate = 16000
    private val channelConfig = AudioFormat.CHANNEL_IN_MONO
    private val audioFormat = AudioFormat.ENCODING_PCM_16BIT
    private val bufferSize = AudioRecord.getMinBufferSize(sampleRate, channelConfig, audioFormat) * 2

    private var audioRecord: AudioRecord? = null
    private var isRecording = false

    // Hardware Audio Effects
    private var aec: AcousticEchoCanceler? = null
    private var ns: NoiseSuppressor? = null
    private var agc: AutomaticGainControl? = null

    private val _audioChunks = MutableSharedFlow<ByteArray>()
    val audioChunks = _audioChunks.asSharedFlow()

    private val _onVoiceDetected = MutableSharedFlow<Boolean>()
    val onVoiceDetected = _onVoiceDetected.asSharedFlow()

    private var lastVoiceState = false

    @SuppressLint("MissingPermission")
    fun startCapture() {
        if (isRecording) return

        try {
            audioRecord = AudioRecord(
                MediaRecorder.AudioSource.VOICE_RECOGNITION,
                sampleRate,
                channelConfig,
                audioFormat,
                bufferSize
            )

            if (audioRecord?.state != AudioRecord.STATE_INITIALIZED) {
                Log.e("AudioCapture", "AudioRecord initialization failed")
                return
            }

            // Enable Hardware Effects (Strategy 5.2)
            val audioSessionId = audioRecord?.audioSessionId ?: -1
            if (audioSessionId != -1) {
                if (AcousticEchoCanceler.isAvailable()) {
                    aec = AcousticEchoCanceler.create(audioSessionId)?.apply { enabled = true }
                    Log.i("AudioCapture", "Hardware AEC enabled")
                }
                if (NoiseSuppressor.isAvailable()) {
                    ns = NoiseSuppressor.create(audioSessionId)?.apply { enabled = true }
                    Log.i("AudioCapture", "Hardware Noise Suppression enabled")
                }
                if (AutomaticGainControl.isAvailable()) {
                    agc = AutomaticGainControl.create(audioSessionId)?.apply { enabled = true }
                    Log.i("AudioCapture", "Hardware AGC enabled")
                }
            }

            audioRecord?.startRecording()
            isRecording = true
            
            scope.launch {
                val buffer = ByteArray(640) // 20ms chunks (320 samples * 2 bytes)
                while (isRecording) {
                    val read = audioRecord?.read(buffer, 0, buffer.size) ?: 0
                    if (read > 0) {
                        val chunk = buffer.copyOf(read)
                        _audioChunks.emit(chunk)
                        performHeuristicVad(chunk)
                    }
                }
            }
        } catch (e: Exception) {
            Log.e("AudioCapture", "Error starting capture", e)
        }
    }

    private suspend fun performHeuristicVad(chunk: ByteArray) {
        // Simple RMS-based VAD (Strategy 5.2 fallback)
        var sum = 0.0
        for (i in 0 until chunk.size - 1 step 2) {
            val sample = ((chunk[i+1].toInt() shl 8) or (chunk[i].toInt() and 0xFF)).toShort()
            sum += sample * sample
        }
        val rms = sqrt(sum / (chunk.size / 2))
        
        // Threshold: ~300-500 is a reasonable baseline for human voice in 16-bit PCM
        val hasVoice = rms > 450 
        
        if (hasVoice != lastVoiceState) {
            lastVoiceState = hasVoice
            _onVoiceDetected.emit(hasVoice)
        }
    }

    fun stopCapture() {
        isRecording = false
        audioRecord?.stop()
        audioRecord?.release()
        audioRecord = null
        
        aec?.release()
        ns?.release()
        agc?.release()
        
        aec = null
        ns = null
        agc = null
    }
}
