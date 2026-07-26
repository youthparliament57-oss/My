package com.example.cognitive.models

import com.example.persona.Persona

data class ReasoningTrace(
    val query: String,
    val subTasks: List<String> = emptyList(),
    val steps: List<ReasoningStep> = emptyList(),
    val decisionCriteria: Map<String, Float> = emptyMap(),
    val finalAnswer: String? = null,
    val confidenceScore: Float = 0.0f,
    val timestamp: Long = System.currentTimeMillis(),
    val userFeedback: String? = null
)

data class ReasoningStep(
    val stepText: String,
    val evidence: String,
    val conclusion: String,
    val confidence: Float
)

sealed class CognitiveResult {
    data class Trace(val finalAnswer: String, val trace: ReasoningTrace) : CognitiveResult()
    data class NeedsClarification(val question: ClarificationQuestion) : CognitiveResult()
    data class Uncertainty(val action: UncertaintyAction, val message: String) : CognitiveResult()
}

data class ClarificationQuestion(
    val originalQuery: String,
    val ambiguityType: AmbiguityType,
    val promptText: String
)

enum class AmbiguityType {
    MISSING_PARAMETER, VAGUE_REFERENCE, CONFLICTING_INTENT
}

sealed class UncertaintyAction {
    object Defer : UncertaintyAction()
    object Admit : UncertaintyAction()
    object Ask : UncertaintyAction()
}

enum class Tone {
    CONFIDENT, TENTATIVE, CAUTIOUS, ENTHUSIASTIC, CASUAL
}
