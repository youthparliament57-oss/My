package com.example.persona

import javax.inject.Inject
import javax.inject.Singleton
import kotlin.random.Random

enum class FillerCategory {
    THINKING, SEARCHING, CLARIFYING, ERROR
}

@Singleton
class DynamicFillerEngine @Inject constructor() {
    private val lastFillersUsed = mutableMapOf<FillerCategory, String>()

    fun getFiller(persona: Persona, category: FillerCategory, containsSensitiveInfo: Boolean): String {
        val fillers = when (persona.id) {
            PersonaId.ATLAS -> getAtlasFillers(category)
            PersonaId.ECHO -> getEchoFillers(category)
            PersonaId.WRAITH -> getWraithFillers(category)
            else -> getDefaultFillers(category)
        }

        // Masking: if contains sensitive info, ensure the filler doesn't leak context
        // (In our simplified implementation, we just use generic fillers regardless, 
        // but this flag is here per strategy 7.7)
        
        var selected = fillers.random()
        // Prevent repeating the same filler twice
        while (fillers.size > 1 && selected == lastFillersUsed[category]) {
            selected = fillers.random()
        }
        
        lastFillersUsed[category] = selected
        
        if (containsSensitiveInfo) {
            return "Processing securely..."
        }
        
        return selected
    }

    private fun getAtlasFillers(category: FillerCategory): List<String> = when (category) {
        FillerCategory.THINKING -> listOf("Allow me a moment, sir.", "Considering the options...", "Just a moment.")
        FillerCategory.SEARCHING -> listOf("Retrieving the requested information...", "Searching the archives.")
        FillerCategory.CLARIFYING -> listOf("Could you elaborate, sir?", "I need more details to proceed.")
        FillerCategory.ERROR -> listOf("I apologize, an error occurred.", "There seems to be an issue.")
    }

    private fun getEchoFillers(category: FillerCategory): List<String> = when (category) {
        FillerCategory.THINKING -> listOf("Hold up, thinking...", "Wait, let me see...", "Hmm...")
        FillerCategory.SEARCHING -> listOf("Looking that up...", "Gimme a sec to search.")
        FillerCategory.CLARIFYING -> listOf("Wait, what do you mean?", "I'm a bit lost, tell me more.")
        FillerCategory.ERROR -> listOf("Oops, my bad.", "Uh oh, something broke.")
    }

    private fun getWraithFillers(category: FillerCategory): List<String> = when (category) {
        FillerCategory.THINKING -> listOf("...")
        FillerCategory.SEARCHING -> listOf("...")
        FillerCategory.CLARIFYING -> listOf("?")
        FillerCategory.ERROR -> listOf("Error.")
    }

    private fun getDefaultFillers(category: FillerCategory): List<String> = when (category) {
        FillerCategory.THINKING -> listOf("Let me think...", "Hmm...", "Give me a moment.")
        FillerCategory.SEARCHING -> listOf("Searching...", "Looking for answers.")
        FillerCategory.CLARIFYING -> listOf("Could you clarify?", "I need more context.")
        FillerCategory.ERROR -> listOf("An error occurred.", "Something went wrong.")
    }
}
