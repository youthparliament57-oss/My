package com.example.cognitive.pipeline

import com.example.cognitive.models.AmbiguityType
import com.example.cognitive.models.ClarificationQuestion
import com.example.domain.repository.NousRepository
import kotlinx.coroutines.flow.first
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ClarificationEngine @Inject constructor(
    private val nousRepository: NousRepository
) {
    suspend fun checkAmbiguity(query: String): ClarificationQuestion? {
        val lowercase = query.lowercase()
        
        // 1. Vague Reference Check
        if (lowercase.contains("call her") || lowercase.contains("call him")) {
            // Check memory for recently mentioned people
            val recentThoughts = nousRepository.getThoughts().first().sortedByDescending { it.timestamp }.take(5)
            val mentionedPerson = recentThoughts.find { 
                it.title.contains("Person", ignoreCase = true) || it.content.contains("Person", ignoreCase = true)
            }
            
            if (mentionedPerson == null) {
                return ClarificationQuestion(
                    originalQuery = query,
                    ambiguityType = AmbiguityType.VAGUE_REFERENCE,
                    promptText = "Who did you want to call?"
                )
            }
            // If found, we could potentially resolve it here or in the Brain.
            // For now, if we can't be sure, we ask.
        }

        // 2. Missing Parameter Check
        if (lowercase.contains("set an alarm") && !lowercase.contains(Regex("\\d"))) {
            return ClarificationQuestion(
                originalQuery = query,
                ambiguityType = AmbiguityType.MISSING_PARAMETER,
                promptText = "For what time should I set the alarm?"
            )
        }

        // 3. Conflicting Intent Check
        if (lowercase.contains("turn off") && lowercase.contains("keep it on")) {
            return ClarificationQuestion(
                originalQuery = query,
                ambiguityType = AmbiguityType.CONFLICTING_INTENT,
                promptText = "That sounds contradictory. Should I turn it off or keep it on?"
            )
        }

        // 4. LLM-based Fallback Ambiguity Check
        val prompt = """
            Query: $query
            
            Analyze if this query is ambiguous or missing critical information to be executed by an AI assistant.
            If it is ambiguous, respond with:
            AMBIGUITY: <MISSING_PARAMETER | VAGUE_REFERENCE | CONFLICTING_INTENT>
            PROMPT: <a direct clarification question for the user>
            
            If it is NOT ambiguous and can be processed, respond with:
            CLEAR
        """.trimIndent()
        
        val response = nousRepository.askNousForInsight(prompt, emptyList<com.example.domain.model.Thought>())
        if (response.contains("AMBIGUITY:")) {
            val typeStr = response.substringAfter("AMBIGUITY:").substringBefore("\n").trim()
            val promptText = if (response.contains("PROMPT:")) {
                response.substringAfter("PROMPT:").substringBefore("\n").trim().ifEmpty {
                    response.substringAfter("PROMPT:").trim()
                }
            } else ""

            if (promptText.isNotEmpty()) {
                val type = when (typeStr) {
                    "MISSING_PARAMETER" -> AmbiguityType.MISSING_PARAMETER
                    "VAGUE_REFERENCE" -> AmbiguityType.VAGUE_REFERENCE
                    "CONFLICTING_INTENT" -> AmbiguityType.CONFLICTING_INTENT
                    else -> AmbiguityType.MISSING_PARAMETER
                }
                return ClarificationQuestion(query, type, promptText)
            }
        }

        return null
    }
}

@Singleton
class TaskPlanner @Inject constructor(
    private val nousRepository: NousRepository
) {
    /**
     * ⚡ Bolt Optimization: Uses `.asSequence()` on string lines before filtering and mapping
     * to optimize garbage collection when generating planning lists.
     */
    suspend fun decompose(query: String): List<String> {
        val prompt = """
            Query: $query
            
            Decompose this complex query into a list of simple, executable sub-tasks. 
            Provide the tasks as a bulleted list.
        """.trimIndent()
        
        val response = nousRepository.askNousForInsight(prompt, emptyList<com.example.domain.model.Thought>())
        val lines = response.lines().asSequence()
            .filter { it.trim().startsWith("-") || it.trim().startsWith("*") || it.trim().firstOrNull()?.isDigit() == true }
            .map { it.trim().substringAfter("-").substringAfter("*").trim() }
            .filter { it.isNotEmpty() }
            .toList()
            
        return if (lines.isNotEmpty()) lines else listOf(query)
    }
}

@Singleton
class FuzzyConstraintInterpreter @Inject constructor(
    private val nousRepository: NousRepository
) {
    suspend fun interpret(query: String): Map<String, String> {
        val constraints = mutableMapOf<String, String>()
        val lowercase = query.lowercase()
        
        // 1. Basic Regex fallbacks
        val budgetMatch = Regex("around (\\d+) rupees").find(lowercase)
        budgetMatch?.let {
            val amount = it.groupValues[1].toInt()
            constraints["budgetRange"] = "${amount - 50}-${amount + 50}"
        }
        
        // 2. LLM-based refinement
        val prompt = """
            Query: $query
            
            Extract constraints from this query (e.g., date ranges, budget limits, quality requirements, location preferences).
            Respond with a key-value list:
            KEY: VALUE
            
            Example:
            budget: <500
            time: next week
        """.trimIndent()
        
        val response = nousRepository.askNousForInsight(prompt, emptyList<com.example.domain.model.Thought>())
        response.lines().forEach { line ->
            if (line.contains(":")) {
                val parts = line.split(":")
                val key = parts[0].trim().lowercase()
                val value = parts.drop(1).joinToString(":").trim()
                if (key.isNotEmpty() && value.isNotEmpty()) {
                    constraints[key] = value
                }
            }
        }
        
        return constraints
    }
}
