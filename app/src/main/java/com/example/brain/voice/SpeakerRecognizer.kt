package com.example.brain.voice

import android.util.Log
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.sqrt

@Singleton
class SpeakerRecognizer @Inject constructor() {
    
    private var enrolledPrint: DoubleArray? = null

    fun enroll(audioData: ByteArray) {
        val features = extractFeatures(audioData)
        enrolledPrint = features
        Log.i("SpeakerRecognizer", "Voice print enrolled successfully.")
    }

    fun verify(audioData: ByteArray): Boolean {
        val print = enrolledPrint ?: return true // No enrollment = everyone allowed

        val currentFeatures = extractFeatures(audioData)
        val similarity = cosineSimilarity(print, currentFeatures)
        
        Log.d("SpeakerRecognizer", "Speaker similarity: $similarity")
        return similarity > 0.85 // Threshold (Strategy 5.5)
    }

    private fun extractFeatures(audioData: ByteArray): DoubleArray {
        // Simplified "Tier 1" MFCC-like features: spectral energy in 12 bands
        val bands = 12
        val features = DoubleArray(bands)
        val samples = audioData.size / 2
        if (samples == 0) return features

        // Split chunk into 'bands' and calculate energy for each
        val samplesPerBand = samples / bands
        for (b in 0 until bands) {
            var sum = 0.0
            for (s in 0 until samplesPerBand) {
                val idx = (b * samplesPerBand + s) * 2
                if (idx + 1 < audioData.size) {
                    val sample = ((audioData[idx+1].toInt() shl 8) or (audioData[idx].toInt() and 0xFF)).toShort()
                    sum += sample * sample
                }
            }
            features[b] = sqrt(sum / samplesPerBand)
        }
        
        // Normalize vector
        val magnitude = sqrt(features.sumOf { it * it })
        if (magnitude > 0) {
            for (i in features.indices) features[i] /= magnitude
        }
        
        return features
    }

    private fun cosineSimilarity(v1: DoubleArray, v2: DoubleArray): Double {
        var dotProduct = 0.0
        for (i in v1.indices) dotProduct += v1[i] * v2[i]
        return dotProduct // Vectors are already normalized
    }
}
