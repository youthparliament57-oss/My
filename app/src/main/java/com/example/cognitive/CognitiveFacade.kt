package com.example.cognitive

import com.example.brain.BrainContext
import com.example.cognitive.models.CognitiveResult
import com.example.cognitive.models.ReasoningTrace
import com.example.cognitive.models.Tone
import com.example.cognitive.models.UncertaintyAction
import com.example.cognitive.pipeline.ClarificationEngine
import com.example.cognitive.pipeline.ConfidenceModulator
import com.example.cognitive.pipeline.DecisionMaker
import com.example.cognitive.pipeline.FuzzyConstraintInterpreter
import com.example.cognitive.pipeline.ProblemSolver
import com.example.cognitive.pipeline.ReasoningCache
import com.example.cognitive.pipeline.ReasoningEngine
import com.example.cognitive.pipeline.ReasoningTraceStore
import com.example.cognitive.pipeline.TaskPlanner
import com.example.cognitive.pipeline.UncertaintyAwareness
import com.example.persona.PersonaId
import com.example.domain.repository.NousRepository
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CognitiveFacade @Inject constructor(
    private val nousRepository: NousRepository,
    private val cache: ReasoningCache,
    private val clarificationEngine: ClarificationEngine,
    private val taskPlanner: TaskPlanner,
    private val fuzzyConstraintInterpreter: FuzzyConstraintInterpreter,
    private val reasoningEngine: ReasoningEngine,
    private val uncertaintyAwareness: UncertaintyAwareness,
    private val decisionMaker: DecisionMaker,
    private val problemSolver: ProblemSolver,
    private val confidenceModulator: ConfidenceModulator,
    private val traceStore: ReasoningTraceStore
) {
    suspend fun processCognitiveTask(query: String, context: BrainContext): CognitiveResult {
        // 1. CACHE CHECK (< 50ms)
        val cacheKey = cache.generateKey(query, context.userId, context.activePersona.name)
        val cachedTrace = cache.get(cacheKey)
        if (cachedTrace != null) {
            return CognitiveResult.Trace(cachedTrace.finalAnswer ?: "", cachedTrace)
        }

        // 2. CLARIFICATION (< 100ms)
        val ambiguity = clarificationEngine.checkAmbiguity(query)
        if (ambiguity != null) {
            return CognitiveResult.NeedsClarification(ambiguity)
        }

        // 3. DECOMPOSITION (100ms - 1s)
        val subTasks = taskPlanner.decompose(query)
        val constraints = fuzzyConstraintInterpreter.interpret(query)

        // 4. REASONING (1s - 10s)
        val reasoningSteps = reasoningEngine.reasonStepByStep(subTasks, context)
        
        // Calculate average confidence from steps
        val avgConfidence = if (reasoningSteps.isNotEmpty()) {
            reasoningSteps.map { it.confidence }.average().toFloat()
        } else {
            0.8f // Default if no steps
        }

        // 5. UNCERTAINTY HANDLING (< 100ms)
        val uncertainty = uncertaintyAwareness.checkConfidence(avgConfidence)
        if (uncertainty != null) {
            val message = when (uncertainty) {
                is UncertaintyAction.Defer -> "I'm not sure, let me look it up."
                is UncertaintyAction.Admit -> "I don't know the answer to that."
                is UncertaintyAction.Ask -> "Could you clarify that?"
            }
            return CognitiveResult.Uncertainty(uncertainty, message)
        }

        // 6. DECISION / SOLVE (100ms - 5s)
        val rawAnswer = if (query.lowercase().contains("which") || query.lowercase().contains("choose") || query.lowercase().contains("compare")) {
            val decisionContext = extractDecisionContext(query)
            decisionMaker.decide(
                options = decisionContext.options,
                criteria = decisionContext.criteria,
                risks = decisionContext.risks,
                preferences = decisionContext.preferences
            )
        } else {
            problemSolver.solve(query, constraints)
        }

        // 7. CONFIDENCE MODULATION (< 50ms)
        val tone = when (context.activePersona.id) {
            PersonaId.ATLAS, PersonaId.VANGUARD -> Tone.CONFIDENT
            PersonaId.SAGE -> Tone.TENTATIVE
            PersonaId.WRAITH -> Tone.CAUTIOUS
            PersonaId.ARIA, PersonaId.NOVA -> Tone.ENTHUSIASTIC
            PersonaId.ECHO -> Tone.CASUAL
            else -> Tone.CONFIDENT
        }
        val finalAnswer = confidenceModulator.modulate(rawAnswer, avgConfidence, tone)

        // 8. CACHE + STORE (< 50ms)
        val trace = ReasoningTrace(
            query = query,
            subTasks = subTasks,
            steps = reasoningSteps,
            decisionCriteria = mapOf(), // Populate if needed
            finalAnswer = finalAnswer,
            confidenceScore = avgConfidence
        )
        
        cache.put(cacheKey, trace)
        traceStore.storeTrace(trace)

        return CognitiveResult.Trace(finalAnswer, trace)
    }

    private suspend fun extractDecisionContext(query: String): DecisionContext {
        val prompt = """
            Query: $query
            
            Extract the following for a decision making process:
            - OPTIONS: a comma-separated list of choices mentioned or implied.
            - CRITERIA: a comma-separated list of factors to consider (e.g., cost, speed, quality).
            - RISKS: a comma-separated list of potential downsides.
            - PREFERENCES: a comma-separated list of preferred attributes (e.g., cheap, fast).
            
            Format:
            OPTIONS: opt1, opt2
            CRITERIA: crit1, crit2
            RISKS: risk1
            PREFERENCES: pref1
        """.trimIndent()
        
        val response = nousRepository.askNousForInsight(prompt, emptyList<com.example.domain.model.Thought>())
        
        val options = response.substringAfter("OPTIONS:", "").substringBefore("\n").split(",").map { it.trim() }.filter { it.isNotEmpty() }
        val criteria = response.substringAfter("CRITERIA:", "").substringBefore("\n").split(",").map { it.trim() }.filter { it.isNotEmpty() }
        val risks = response.substringAfter("RISKS:", "").substringBefore("\n").split(",").map { it.trim() }.filter { it.isNotEmpty() }
        val preferences = response.substringAfter("PREFERENCES:", "").substringBefore("\n").split(",").associateWith { 2 }
        
        return DecisionContext(if (options.isEmpty()) listOf("Option A", "Option B") else options, criteria, risks, preferences)
    }

    private data class DecisionContext(
        val options: List<String>,
        val criteria: List<String>,
        val risks: List<String>,
        val preferences: Map<String, Int>
    )
}
