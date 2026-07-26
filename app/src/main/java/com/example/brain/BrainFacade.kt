package com.example.brain

import android.content.Context
import android.util.Log
import com.example.domain.repository.NousRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
open class BrainFacade @Inject constructor(
    @ApplicationContext private val context: Context,
    private val repository: NousRepository,
    private val memory: com.example.brain.memory.MemoryInterface,
    private val localLlmLayer: LocalLlmLayer,
    private val cloudLlmLayer: CloudLlmLayer,
    private val memoryDao: com.example.brain.memory.MemoryDao,
    private val intentClassifier: IntentClassifier,
    private val ruleEngine: RuleEngine,
    private val skillRouter: SkillRouter,
    private val agenticOrchestrator: AgenticOrchestrator,
    private val cognitiveFacade: com.example.cognitive.CognitiveFacade,
    private val preProcessor: BrainPreProcessor,
    private val postProcessor: BrainPostProcessor
) : BrainInterface {

    override suspend fun processQuery(
        rawQuery: String,
        history: List<ConversationTurn>
    ): BrainResponse {
        val result = processQueryInternal(rawQuery, history)
        if (result.layerUsed != "Guardrails") {
            try {
                memory.storeEpisodicEvent(
                    eventText = "User asked: '$rawQuery'. NOUS responded: '${result.rawText}'",
                    category = "ConversationTurn",
                    confidence = 1.0f
                )
            } catch (e: Exception) {
                Log.e("BrainFacade", "Failed to store episodic event: ${e.message}")
            }
        }
        return result
    }

    private suspend fun processQueryInternal(
        rawQuery: String,
        history: List<ConversationTurn> = emptyList()
    ): BrainResponse {
        val startTime = System.currentTimeMillis()

        // 1. Trim, Normalize, and run Pre-Processing Guardrails
        val preResult = preProcessor.preProcess(rawQuery, history, memory)
        if (preResult is BrainPreProcessor.PreProcessResult.Blocked) {
            return postProcessor.processResponse(
                rawOutput = preResult.safeResponse,
                layer = "Guardrails",
                startTimeMs = startTime,
                brainContext = BrainContext()
            )
        }

        if (preResult !is BrainPreProcessor.PreProcessResult.Success) {
             return postProcessor.processResponse(
                rawOutput = "System error during pre-processing.",
                layer = "Guardrails",
                startTimeMs = startTime,
                brainContext = BrainContext()
            )
        }
        val cleanQuery = preResult.query
        val forcedLayer = preResult.forcedLayer
        val brainContext = preResult.brainContext

        // LAYER 0: Intent Classifier (< 5ms)
        val intent = intentClassifier.classify(cleanQuery)
        Log.i("BrainFacade", "Layer 0 Intent: $intent")

        // 3. Evaluate forced layer overrides
        if (forcedLayer != null) {
            Log.i("BrainFacade", "Manual layer override: '$forcedLayer'")
            when (forcedLayer) {
                "local" -> {
                    val result = localLlmLayer.processLocalQuery(cleanQuery, brainContext)
                    return postProcessor.processResponse(result, "Layer 3: Local LLM (Forced)", startTime, brainContext)
                }
                "cloud", "gemini", "openai", "anthropic", "groq", "openrouter" -> {
                    val provider = if (forcedLayer == "cloud") "gemini" else forcedLayer
                    val result = cloudLlmLayer.processCloudQuery(cleanQuery, provider, brainContext)
                    return postProcessor.processResponse(result, "Layer 4: Cloud LLM (Forced)", startTime, brainContext)
                }
                "agent", "agentic" -> {
                    val result = agenticOrchestrator.executeAgentLoop(cleanQuery, brainContext)
                    return postProcessor.processResponse(result, "Layer 5: Agentic (Forced)", startTime, brainContext)
                }
                "rule" -> {
                    val ruleResult = ruleEngine.process(intent)
                    if (ruleResult != null) return handleSkillOutput(ruleResult, "Layer 1: Rule Engine (Forced)", startTime, brainContext)
                }
                "cognitive", "reasoning" -> {
                    val cogResult = cognitiveFacade.processCognitiveTask(cleanQuery, brainContext)
                    return handleCognitiveResult(cogResult, "Layer 8: Cognitive (Forced)", startTime, brainContext)
                }
            }
        }

        // LAYER 1: Rule Engine (< 10ms)
        val ruleOutput = ruleEngine.process(intent)
        if (ruleOutput != null) {
            return handleSkillOutput(ruleOutput, "Layer 1: Rule Engine", startTime, brainContext)
        }

        // LAYER 2: Skill Router (< 20ms)
        val skillOutput = skillRouter.process(intent)
        if (skillOutput != null) {
            return handleSkillOutput(skillOutput, "Layer 2: Skill Router", startTime, brainContext)
        }

        // LAYER 8: Cognitive Reasoning (Complex Queries)
        val needsCognitive = cleanQuery.contains("reason", ignoreCase = true) ||
                             cleanQuery.contains("evaluate", ignoreCase = true) ||
                             cleanQuery.contains("decide", ignoreCase = true) ||
                             cleanQuery.contains("compare", ignoreCase = true)
        
        if (needsCognitive) {
            val cogResult = cognitiveFacade.processCognitiveTask(cleanQuery, brainContext)
            return handleCognitiveResult(cogResult, "Layer 8: Cognitive Reasoning", startTime, brainContext)
        }

        // LAYER 3: Local LLM (500ms - 5s)
        if (brainContext.ambientSnapshot.networkType == "OFFLINE" || brainContext.activePersona.temperature < 0.3f) {
            val localResult = localLlmLayer.processLocalQuery(cleanQuery, brainContext)
            return postProcessor.processResponse(localResult, "Layer 3: Local LLM", startTime, brainContext)
        }

        // LAYER 4 / 5: Cloud LLM or Agentic Orchestrator
        val isComplex = cleanQuery.contains("calculate", ignoreCase = true) ||
                        cleanQuery.contains("search", ignoreCase = true) ||
                        cleanQuery.contains("solve", ignoreCase = true) ||
                        cleanQuery.contains("remember", ignoreCase = true) ||
                        cleanQuery.contains("why", ignoreCase = true)

        val response = if (isComplex) {
            val agentResult = agenticOrchestrator.executeAgentLoop(cleanQuery, brainContext)
            postProcessor.processResponse(agentResult, "Layer 5: Agentic Orchestrator", startTime, brainContext)
        } else {
            val activeProviderPref = memoryDao.getUserPreference("active_llm_provider")
            val activeProvider = activeProviderPref?.encryptedValue?.let { com.example.brain.memory.MemoryEncryption.decrypt(it) } ?: "gemini"
            val cloudResult = try {
                cloudLlmLayer.processCloudQuery(cleanQuery, activeProvider, brainContext)
            } catch (e: Exception) {
                localLlmLayer.processLocalQuery(cleanQuery, brainContext)
            }
            postProcessor.processResponse(cloudResult, "Layer 4: Cloud LLM ($activeProvider)", startTime, brainContext)
        }

        // Auto-Memory Logging
        memory.storeEpisodicEvent(
            eventText = "Interaction: User said '$cleanQuery', NOUS responded with '${response.rawText.take(100)}...'",
            category = "Chat",
            confidence = 1.0f,
            latitude = brainContext.ambientSnapshot.latitude,
            longitude = brainContext.ambientSnapshot.longitude
        )

        return response
    }

    private suspend fun handleCognitiveResult(
        result: com.example.cognitive.models.CognitiveResult,
        layer: String,
        startTime: Long,
        brainContext: BrainContext
    ): BrainResponse {
        val text = when (result) {
            is com.example.cognitive.models.CognitiveResult.Trace -> result.finalAnswer
            is com.example.cognitive.models.CognitiveResult.NeedsClarification -> result.question.promptText
            is com.example.cognitive.models.CognitiveResult.Uncertainty -> result.message
        }
        return postProcessor.processResponse(text, layer, startTime, brainContext)
    }

    private suspend fun handleSkillOutput(
        output: SkillOutput,
        layer: String,
        startTime: Long,
        brainContext: BrainContext
    ): BrainResponse {
        val rawText = when (output) {
            is SkillOutput.Success -> output.message
            is SkillOutput.Failure -> "Operation failed: ${output.reason}"
            is SkillOutput.NeedsPermission -> "Needs permission: '${output.permission}'"
        }
        val response = postProcessor.processResponse(rawText, layer, startTime, brainContext)

        // Auto-Memory Logging for Skills
        memory.storeEpisodicEvent(
            eventText = "Skill Execution ($layer): $rawText",
            category = "Skill",
            confidence = 1.0f,
            latitude = brainContext.ambientSnapshot.latitude,
            longitude = brainContext.ambientSnapshot.longitude
        )

        return response
    }
}
