package com.example.brain

import com.example.persona.Persona
import com.example.persona.PersonaDefinitions
import java.util.UUID

data class BrainContext(
    val userId: String = "default_user",
    val activePersona: Persona = PersonaDefinitions.ATLAS,
    val ambientSnapshot: AmbientSnapshot = AmbientSnapshot(),
    val conversationHistory: List<ConversationTurn> = emptyList(),
    val activeIntents: List<String> = emptyList(),
    val budget: BrainBudget = BrainBudget(),
    val correlationId: String = UUID.randomUUID().toString(),
    val memoryContext: com.example.brain.memory.MemoryContext = com.example.brain.memory.MemoryContext()
)

data class AmbientSnapshot(
    val timestampMs: Long = System.currentTimeMillis(),
    val batteryLevel: Float = 1.0f,
    val isCharging: Boolean = false,
    val networkType: String = "WIFI", // WIFI, CELLULAR, OFFLINE
    val currentApp: String? = null,
    val latitude: Double? = null,
    val longitude: Double? = null,
    val stepCount: Int = 0
)

data class ConversationTurn(
    val id: String = UUID.randomUUID().toString(),
    val prompt: String,
    val response: String,
    val timestampMs: Long = System.currentTimeMillis(),
    val layerUsed: String,
    val cost: Double = 0.0,
    val tokensUsed: Int = 0
)

data class BrainBudget(
    val maxCostPerTurn: Double = 0.05, // in USD
    val maxTokensPerTurn: Int = 4096,
    val timeLimitMs: Long = 10000,
    val remainingCost: Double = 0.05,
    val remainingTokens: Int = 4096
)
