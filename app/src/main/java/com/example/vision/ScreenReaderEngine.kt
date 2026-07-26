package com.example.vision

import android.graphics.Bitmap
import android.util.Log
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ScreenReaderEngine @Inject constructor(
    private val ocrEngine: OcrEngine
) {

    /**
     * Strategy 6.2: Screen Reading.
     * Analyzes a bitmap representing the user's screen to extract UI hierarchy
     * and textual context for the Brain.
     */
    suspend fun analyzeScreen(bitmap: Bitmap): ScreenContext {
        Log.i("ScreenReader", "Analyzing screen bitmap for semantic context.")
        
        val fullText = ocrEngine.recognizeText(bitmap) ?: ""
        
        // Strategy: Parse text into logical clusters (header, body, buttons)
        // based on spatial density (simulated here)
        val clusters = clusterText(fullText)
        
        return ScreenContext(
            fullText = fullText,
            uiClusters = clusters,
            isSensitive = checkForSensitiveInfo(fullText)
        )
    }

    private fun clusterText(text: String): List<UiCluster> {
        // In a real impl, this would use the spatial coordinates from ML Kit's Text results
        return listOf(UiCluster("General", text))
    }

    private fun checkForSensitiveInfo(text: String): Boolean {
        // Look for common patterns: credit cards, OTPs, Aadhaar numbers (as per strategy 5.12)
        val otpRegex = Regex("\\b\\d{4,6}\\b")
        val ccRegex = Regex("\\b\\d{4}[ -]?\\d{4}[ -]?\\d{4}[ -]?\\d{4}\\b")
        return otpRegex.containsMatchIn(text) || ccRegex.containsMatchIn(text)
    }

    data class ScreenContext(
        val fullText: String,
        val uiClusters: List<UiCluster>,
        val isSensitive: Boolean
    )

    data class UiCluster(val label: String, val text: String)
}
