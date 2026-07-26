package com.example.brain.voice

import android.util.Log
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SerEngine @Inject constructor() {
    
    enum class UserEmotion {
        HAPPY, SAD, ANGRY, CALM, NEUTRAL
    }

    fun analyzeProsody(audioData: ByteArray): UserEmotion {
        // Heuristic fallback (Strategy 5.10: RMS energy + pitch variance)
        var sum = 0.0
        var maxAbs = 0
        val samples = mutableListOf<Short>()
        
        for (i in 0 until audioData.size step 2) {
            val sample = ((audioData[i+1].toInt() shl 8) or (audioData[i].toInt() and 0xFF)).toShort()
            sum += sample * sample
            val abs = Math.abs(sample.toInt())
            if (abs > maxAbs) maxAbs = abs
            samples.add(sample)
        }
        
        val rms = Math.sqrt(sum / samples.size)
        
        // Very simplified pitch estimation via zero-crossings
        var zeroCrossings = 0
        for (i in 0 until samples.size - 1) {
            if ((samples[i] > 0 && samples[i+1] < 0) || (samples[i] < 0 && samples[i+1] > 0)) {
                zeroCrossings++
            }
        }
        
        Log.d("SerEngine", "SER Analysis: RMS=$rms, ZC=$zeroCrossings")

        return when {
            rms > 2000 && zeroCrossings > 100 -> UserEmotion.ANGRY
            rms < 300 -> UserEmotion.CALM
            zeroCrossings > 120 -> UserEmotion.HAPPY
            rms > 500 && zeroCrossings < 60 -> UserEmotion.SAD
            else -> UserEmotion.NEUTRAL
        }
    }
}
