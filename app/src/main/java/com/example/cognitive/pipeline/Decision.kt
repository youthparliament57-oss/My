package com.example.cognitive.pipeline

import com.example.cognitive.models.Tone
import com.example.cognitive.models.UncertaintyAction
import com.example.domain.repository.NousRepository
import kotlinx.coroutines.flow.first
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class UncertaintyAwareness @Inject constructor() {
    fun checkConfidence(confidenceScore: Float, queryType: String = "factual", userPrefersHonesty: Boolean = true): UncertaintyAction? {
        val threshold = 0.6f
        if (confidenceScore >= threshold) return null
        
        return when {
            confidenceScore < 0.3f -> UncertaintyAction.Admit
            queryType == "factual" -> UncertaintyAction.Defer
            else -> UncertaintyAction.Ask
        }
    }
}

@Singleton
class DecisionMaker @Inject constructor(
    private val criteriaAnalyzer: CriteriaAnalyzer,
    private val riskAssessor: RiskAssessor,
    private val preferenceAligner: PreferenceAligner
) {
    fun decide(options: List<String>, criteria: List<String>, risks: List<String>, preferences: Map<String, Int>): String {
        if (options.isEmpty()) return "No viable options found."
        
        // Multi-criteria decision analysis (MCDA)
        val details = mutableMapOf<String, Float>()
        val scores = options.associateWith { option ->
            var score = 0f
            
            // Score each criterion
            for (criterion in criteria) {
                val criterionScore = criteriaAnalyzer.score(option, criterion)
                val weight = preferences[criterion] ?: 1
                score += criterionScore * weight
            }
            
            // Penalize for risks
            val riskPenalty = riskAssessor.calculateTotalPenalty(option, risks)
            score -= riskPenalty
            
            // Align with preferences
            score += preferenceAligner.alignmentBonus(option, preferences)
            
            details[option] = score
            score
        }
        
        val winner = scores.maxByOrNull { it.value }?.key ?: options.first()
        val winnerScore = scores[winner] ?: 0f
        
        val justification = """
            Selected Option: $winner
            
            Decision Matrix:
            ${scores.entries.joinToString("\n") { "- ${it.key}: ${it.value} points" }}
            
            Justification: $winner scored the highest based on your criteria (${criteria.joinToString()}) and preferences. 
            ${if (risks.isNotEmpty()) "Risks considered: ${risks.joinToString()}." else ""}
        """.trimIndent()
        
        return justification
    }
}

@Singleton
class CriteriaAnalyzer @Inject constructor() {
    fun score(option: String, criterion: String): Float {
        // Heuristic: check if option name or attributes match criterion
        val opt = option.lowercase()
        val crit = criterion.lowercase()
        
        return when {
            opt.contains(crit) -> 10f
            crit == "cost" && (opt.contains("budget") || opt.contains("cheap")) -> 8f
            crit == "quality" && (opt.contains("premium") || opt.contains("pro")) -> 9f
            else -> 5f
        }
    }
}

@Singleton
class RiskAssessor @Inject constructor() {
    fun calculateTotalPenalty(option: String, risks: List<String>): Float {
        var penalty = 0f
        val opt = option.lowercase()
        for (risk in risks) {
            val r = risk.lowercase()
            if (opt.contains(r)) {
                penalty += 5f
            }
        }
        return penalty
    }
    
    fun penalize(option: String, risks: List<String>): Float = calculateTotalPenalty(option, risks)
}

@Singleton
class PreferenceAligner @Inject constructor() {
    fun alignmentBonus(option: String, preferences: Map<String, Int>): Float {
        var bonus = 0f
        val opt = option.lowercase()
        for ((pref, weight) in preferences) {
            if (opt.contains(pref.lowercase())) {
                bonus += 2f * weight
            }
        }
        return bonus
    }

    fun weight(option: String, preferences: Map<String, Int>): Float = alignmentBonus(option, preferences)
}

@Singleton
class ProblemSolver @Inject constructor(
    private val nousRepository: NousRepository
) {
    suspend fun solve(problem: String, constraints: Map<String, String>): String {
        // Pattern matching: recall similar past problems
        val recentThoughts = nousRepository.getThoughts().first().take(10)
        val similarProblem = recentThoughts.find { it.title.contains(problem.take(5), ignoreCase = true) }
        
        val contextPrompt = similarProblem?.let { "I recall a similar problem: ${it.content}. " } ?: ""
        
        val prompt = """
            $contextPrompt
            Problem: $problem
            Constraints: $constraints
            
            Solve this problem considering the constraints. Provide a direct, effective solution.
        """.trimIndent()
        
        return nousRepository.askNousForInsight(prompt, emptyList())
    }
}

@Singleton
class ConfidenceModulator @Inject constructor() {
    fun modulate(answer: String, confidence: Float, tone: Tone): String {
        val prefix = when (tone) {
            Tone.CONFIDENT -> if (confidence > 0.8f) "I am certain that " else "I believe "
            Tone.TENTATIVE -> "Based on my analysis, it appears that "
            Tone.CAUTIOUS -> "I'm not entirely sure, but "
            Tone.ENTHUSIASTIC -> "Great news! "
            Tone.CASUAL -> "I'd say "
        }
        
        // Modulate based on confidence too if needed
        return if (confidence < 0.5f && tone == Tone.CONFIDENT) {
            "I think $answer, but I might be wrong."
        } else {
            "$prefix$answer"
        }
    }
}
