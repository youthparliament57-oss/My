package com.example.persona

import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PersonaFacade @Inject constructor(
    private val jitContextFetcher: JitContextFetcher,
    private val gossipTriggerEvaluator: GossipTriggerEvaluator,
    private val tokenSurvivalManager: TokenSurvivalManager,
    private val emotionStateTracker: EmotionStateTracker,
    private val dynamicFillerEngine: DynamicFillerEngine
) {
    private var activePersona: Persona = PersonaDefinitions.ATLAS

    fun setActivePersona(personaId: PersonaId) {
        activePersona = PersonaDefinitions.ALL.find { it.id == personaId } ?: PersonaDefinitions.ATLAS
    }

    fun getActivePersona(): Persona = activePersona

    suspend fun getJitContext(): String {
        return jitContextFetcher.fetchContext()
    }

    fun evaluateGossipTrigger(contextSummary: String): GossipTriggerEvaluator.GossipTrigger {
        return gossipTriggerEvaluator.evaluate(contextSummary)
    }

    fun buildSystemPrompt(activeContext: String, history: List<Pair<String, String>>): String {
        val basePrompt = SystemPromptBuilder.buildSystemPrompt(activePersona, activeContext, "")
        // Compress context using TokenSurvivalManager
        return tokenSurvivalManager.compressContext(basePrompt, history, activePersona.maxTokens)
    }

    fun updateUserEmotion(emotion: Emotion) {
        emotionStateTracker.updateUserEmotion(emotion)
    }

    fun getCurrentEmotion(): Emotion = emotionStateTracker.getCurrentEmotion()

    fun getFiller(category: FillerCategory, containsSensitiveInfo: Boolean): String {
        return dynamicFillerEngine.getFiller(activePersona, category, containsSensitiveInfo)
    }
}
