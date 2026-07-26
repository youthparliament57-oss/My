package com.example.brain
import javax.inject.Inject
import javax.inject.Singleton

import android.util.Log

data class BrainResponse(
    val rawText: String,
    val cleanTextForTts: String,
    val detectedEmotion: String,
    val layerUsed: String,
    val latencyMs: Long,
    val cost: Double,
    val tokensUsed: Int
)


@Singleton
class BrainPostProcessor @Inject constructor() {

    fun processResponse(
        rawOutput: String,
        layer: String,
        startTimeMs: Long,
        brainContext: BrainContext
    ): BrainResponse {
        val latencyMs = System.currentTimeMillis() - startTimeMs

        // 1. Safety check output guardrails
        val safetyCheck = ConstitutionalGuardrails.checkSafety(rawOutput)
        val finalRaw = if (safetyCheck is ConstitutionalGuardrails.SafetyResult.Blocked) {
            Log.w("BrainPostProcessor", "CRITICAL WARNING: Layer output triggered safety block. Redacting response.")
            safetyCheck.response
        } else {
            rawOutput
        }

        // 2. Markdown to plain-text formatting for TTS
        val cleanTextForTts = convertMarkdownToPlainText(finalRaw)

        // 3. Emotion injection based on sentiment/semantic markers
        val detectedEmotion = detectEmotion(finalRaw, brainContext.activePersona)

        // 4. Token & Cost estimates
        val inputTokens = brainContext.conversationHistory.sumOf { it.prompt.length / 4 }
        val outputTokens = finalRaw.length / 4
        val totalTokens = inputTokens + outputTokens
        
        // Approximate cost
        val cost = (inputTokens * 0.000075 / 1000) + (outputTokens * 0.00030 / 1000)

        val response = BrainResponse(
            rawText = finalRaw,
            cleanTextForTts = cleanTextForTts,
            detectedEmotion = detectedEmotion,
            layerUsed = layer,
            latencyMs = latencyMs,
            cost = cost,
            tokensUsed = totalTokens
        )

        Log.i("BrainPostProcessor", "Post-processing complete. Response Layer: '$layer'. Emotion: '$detectedEmotion'. Latency: ${latencyMs}ms. Cost: \$$cost.")
        return response
    }

    private fun convertMarkdownToPlainText(markdown: String): String {
        var text = markdown
        // Remove bold/italic markup
        text = text.replace(Regex("\\*\\*|\\*|__|_"), "")
        // Remove headers
        text = text.replace(Regex("#+\\s+"), "")
        // Remove markdown links but keep the label [label](url) -> label
        text = text.replace(Regex("\\[(.*?)\\]\\(.*?\\)"), "$1")
        // Remove inline code ticks
        text = text.replace(Regex("`"), "")
        // Remove block quotes
        text = text.replace(Regex("^>\\s+", RegexOption.MULTILINE), "")
        // Remove list hyphens and bullet points
        text = text.replace(Regex("^[\\s]*[-*+]\\s+", RegexOption.MULTILINE), "")
        // Replace multiple newlines with single spaces for continuous TTS voice reading
        text = text.replace(Regex("\\n+"), " ").trim()
        return text
    }

    private fun detectEmotion(text: String, persona: com.example.persona.Persona): String {
        val lowercase = text.lowercase()
        val emotions = listOf("HAPPY", "CALM", "URGENT", "WITTY")

        // Sentiment Score
        val positiveWords = listOf("happy", "great", "excellent", "good", "wonderful", "love", "joy", "success", "resolved", "solved")
        val negativeWords = listOf("sad", "bad", "error", "failed", "unhappy", "problem", "difficult", "hard", "wrong", "sorry")
        
        var sentiment = 0
        positiveWords.forEach { if (lowercase.contains(it)) sentiment++ }
        negativeWords.forEach { if (lowercase.contains(it)) sentiment-- }

        return when {
            lowercase.contains("urgent") || lowercase.contains("danger") || lowercase.contains("alert") || lowercase.contains("stop") -> {
                if (emotions.contains("URGENT")) "URGENT" else "HAPPY"
            }
            sentiment > 1 || lowercase.contains("haha") || lowercase.contains("lol") || lowercase.contains("witty") -> {
                if (emotions.contains("WITTY") && sentiment > 0) "WITTY" else "HAPPY"
            }
            sentiment < -1 || lowercase.contains("sad") || lowercase.contains("sorry") || lowercase.contains("unfortunately") -> {
                if (emotions.contains("SAD")) "SAD" else "CALM"
            }
            lowercase.contains("peaceful") || lowercase.contains("relax") || lowercase.contains("calm") -> {
                if (emotions.contains("CALM")) "CALM" else "HAPPY"
            }
            else -> {
                emotions.firstOrNull() ?: "HAPPY"
            }
        }
    }
}
