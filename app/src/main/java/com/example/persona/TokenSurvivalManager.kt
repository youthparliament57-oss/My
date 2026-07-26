package com.example.persona

import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ApproximateTokenCounter @Inject constructor() {
    fun estimateTokens(text: String): Int {
        // Approximate: 1 token ~= 4 characters in English
        return text.length / 4
    }
}

@Singleton
class TokenSurvivalManager @Inject constructor(
    private val approximateTokenCounter: ApproximateTokenCounter
) {
    fun compressContext(
        systemPrompt: String,
        history: List<Pair<String, String>>, // Pair<Role, Content>
        maxTokens: Int
    ): String {
        val systemTokens = approximateTokenCounter.estimateTokens(systemPrompt)
        var remainingTokens = maxTokens - systemTokens
        
        if (remainingTokens < 0) {
            return systemPrompt // System prompt alone exceeds limit
        }

        val compressedHistory = mutableListOf<String>()
        
        // Strategy:
        // 1. Keep last 3 turns verbatim
        // 2. Drop low value turns
        // 3. Summarize older turns
        
        val last3Turns = history.takeLast(3)
        val olderTurns = history.dropLast(3)

        // Drop conversational filler from older turns
        val filteredOlderTurns = olderTurns.filter { (_, content) ->
            !isLowValueTurn(content)
        }
        
        // Summarize (simplified: just take the first sentence of older turns if needed)
        val bulletPoints = filteredOlderTurns.map { (role, content) ->
            "- $role: ${content.substringBefore('.')}"
        }

        // Add last 3 verbatim
        val verbatimTurns = last3Turns.map { (role, content) ->
            "$role: $content"
        }

        val finalContext = buildString {
            appendLine(systemPrompt)
            appendLine("\n--- Previous Context ---")
            bulletPoints.forEach { appendLine(it) }
            appendLine("\n--- Recent Conversation ---")
            verbatimTurns.forEach { appendLine(it) }
        }

        return finalContext
    }

    private fun isLowValueTurn(text: String): Boolean {
        val lower = text.lowercase()
        val lowValuePhrases = setOf("hello", "hi", "ok", "okay", "thanks", "thank you", "got it", "yes", "no")
        return lowValuePhrases.contains(lower.trim())
    }
}
