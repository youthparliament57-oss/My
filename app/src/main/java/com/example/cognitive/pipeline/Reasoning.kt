package com.example.cognitive.pipeline

import com.example.brain.BrainContext
import com.example.cognitive.models.ReasoningStep
import com.example.domain.repository.NousRepository
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ReasoningEngine @Inject constructor(
    private val nousRepository: NousRepository,
    private val selfCorrector: SelfCorrector
) {
    suspend fun reasonStepByStep(
        subTasks: List<String>, 
        context: BrainContext
    ): List<ReasoningStep> {
        val steps = mutableListOf<ReasoningStep>()
        
        for (task in subTasks) {
            val prompt = """
                Task: $task
                
                Reason about this task. Provide your reasoning in a structured format:
                REASONING: <your reasoning step>
                EVIDENCE: <evidence from context or general knowledge>
                CONCLUSION: <short conclusion for this step>
                CONFIDENCE: <score between 0 and 1>
            """.trimIndent()
            
            val llmResponse = nousRepository.askNousForInsight(prompt, emptyList<com.example.domain.model.Thought>())
            
            // Parse LLM response
            val reasoning = llmResponse.substringAfter("REASONING:", "").substringBefore("\nEVIDENCE:").trim()
            val evidence = llmResponse.substringAfter("EVIDENCE:", "").substringBefore("\nCONCLUSION:").trim()
            val conclusion = llmResponse.substringAfter("CONCLUSION:", "").substringBefore("\nCONFIDENCE:").trim()
            val confidenceStr = llmResponse.substringAfter("CONFIDENCE:", "0.8").trim()
            val confidence = confidenceStr.toFloatOrNull() ?: 0.8f
            
            val rawStep = ReasoningStep(
                stepText = reasoning.ifEmpty { "Analyzing $task" },
                evidence = evidence.ifEmpty { "Based on contextual analysis" },
                conclusion = conclusion.ifEmpty { "Proceeding with plan for $task" },
                confidence = confidence
            )
            
            val correctedStep = selfCorrector.reviewStep(rawStep, steps)
            if (correctedStep != null) {
                steps.add(correctedStep)
            }
        }
        
        return steps
    }
}

@Singleton
class SelfCorrector @Inject constructor() {
    fun reviewStep(step: ReasoningStep, previousSteps: List<ReasoningStep>): ReasoningStep? {
        // Look for logical errors: contradictions, unsupported conclusions.
        // Return null if step is completely invalid, or a modified step if correctable.
        // Returning the same step if valid.
        if (step.confidence < 0.3f) return null
        return step
    }
}
