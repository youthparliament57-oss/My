package com.example.brain.memory

import kotlin.math.exp
import kotlin.math.ln

object EbbinghausForgettingCurve {

    /**
     * Calculates the decayed confidence level of a memory using the Ebbinghaus forgetting formula:
     * R = C * exp(-t / S)
     * where:
     * - C = Initial confidence
     * - t = Time since last recall (in days)
     * - S = Strength parameter (related to half-life)
     */
    fun calculateDecayedConfidence(
        initialConfidence: Float,
        lastRecalledMs: Long,
        currentMs: Long,
        halfLifeDays: Float
    ): Float {
        if (halfLifeDays <= 0f) return 0.0f
        
        val elapsedMs = (currentMs - lastRecalledMs).coerceAtLeast(0L)
        val elapsedDays = elapsedMs.toFloat() / (1000f * 60f * 60f * 24f)
        
        // Decay constant lambda = ln(2) / halfLife
        val lambda = ln(2.0) / halfLifeDays.toDouble()
        val decayed = initialConfidence.toDouble() * exp(-lambda * elapsedDays.toDouble())
        
        return decayed.coerceIn(0.0, 1.0).toFloat()
    }

    /**
     * Strengthens the memory half-life upon a successful recall.
     * Frequently recalled memories consolidate and resist forgetting better (larger half-life).
     */
    fun strengthenHalfLife(currentHalfLifeDays: Float, recallCount: Int): Float {
        // Human spacing effect: each subsequent recall extends memory retention significantly
        val multiplier = 1.5f + (recallCount * 0.2f).coerceAtMost(2.0f)
        return (currentHalfLifeDays * multiplier).coerceIn(1.0f, 365.0f)
    }

    /**
     * Decays memory confidence if it has not been recalled for a threshold period (e.g., 7 days).
     * Strategy: "memories not recalled in 7 days have their confidence halved"
     */
    fun checkAndApplySevenDayDecay(
        confidence: Float,
        lastRecalledMs: Long,
        currentMs: Long
    ): Float {
        val elapsedMs = currentMs - lastRecalledMs
        val elapsedDays = elapsedMs.toFloat() / (1000f * 60f * 60f * 24f)
        
        return if (elapsedDays >= 7.0f) {
            val intervals = (elapsedDays / 7.0f).toInt()
            var decayedConfidence = confidence
            repeat(intervals) {
                decayedConfidence *= 0.5f
            }
            decayedConfidence.coerceAtLeast(0.0f)
        } else {
            confidence
        }
    }
}
