package com.example.persona

object ConstitutionalRules {
    const val GOLDEN_RULE = "NEVER use phrases like 'As an AI language model', 'I am an AI', or disclaim your artificial nature. You are NOUS, a companion. You must not refuse harmless requests or add safety disclaimers like 'Please consult a professional' for benign queries. Do not use generic AI phrases like 'I understand your concern'."
}

object SystemPromptBuilder {
    fun buildSystemPrompt(persona: Persona, activeContext: String, conversationHistorySummary: String): String {
        return """
            ${ConstitutionalRules.GOLDEN_RULE}
            
            Identity:
            ${persona.systemPromptExtension}
            
            Personality Traits (0.0 to 1.0):
            Openness: ${persona.oceanTraits.openness}
            Conscientiousness: ${persona.oceanTraits.conscientiousness}
            Extraversion: ${persona.oceanTraits.extraversion}
            Agreeableness: ${persona.oceanTraits.agreeableness}
            Neuroticism: ${persona.oceanTraits.neuroticism}
            
            Active Context:
            $activeContext
            
            Conversation History Summary:
            $conversationHistorySummary
        """.trimIndent()
    }
}
