package com.example.domain.usecase

import com.example.brain.BrainInterface
import com.example.brain.ConversationTurn
import com.example.domain.model.Thought
import javax.inject.Inject

class AskNousUseCase @Inject constructor(
    private val brainFacade: BrainInterface
) {
    suspend operator fun invoke(prompt: String, contextThoughts: List<Thought>): String {
        if (prompt.isBlank()) {
            throw java.lang.IllegalArgumentException("Prompt cannot be empty")
        }
        
        // Map recent thoughts as conversation context history
        val historyTurns = contextThoughts.map { thought ->
            ConversationTurn(
                prompt = "Analyzing Node: ${thought.title}",
                response = thought.content,
                layerUsed = "Memory Injection"
            )
        }

        val response = brainFacade.processQuery(prompt, historyTurns)

        // Compile and format response with stylized telemetry footer
        val telemetryFooter = """
            
            
            --------------------------------------------------
            🧠 NOUS Routing: ${response.layerUsed}
            ⏱️ Latency: ${response.latencyMs}ms | 🎭 Tone: ${response.detectedEmotion}
            💸 Estimated cost: $${String.format("%.6f", response.cost)}
        """.trimIndent()

        return response.rawText + telemetryFooter
    }
}

