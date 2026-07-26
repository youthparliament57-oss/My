package com.example.brain.voice

import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DeltaContextUpdater @Inject constructor() {
    
    private var lastTranscript = ""

    fun getDelta(currentTranscript: String): String {
        val trimmedCurrent = currentTranscript.trim()
        val trimmedLast = lastTranscript.trim()

        if (trimmedCurrent == trimmedLast) return ""

        // Simple delta: if current starts with last, take the rest
        return if (trimmedCurrent.startsWith(trimmedLast)) {
            val delta = trimmedCurrent.substring(trimmedLast.length).trim()
            lastTranscript = trimmedCurrent
            delta
        } else {
            // Divergence: reset and return current as full delta
            lastTranscript = trimmedCurrent
            trimmedCurrent
        }
    }

    fun reset() {
        lastTranscript = ""
    }
}
