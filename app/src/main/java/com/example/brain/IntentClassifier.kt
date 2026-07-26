package com.example.brain

import java.util.regex.Pattern
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class IntentClassifier @Inject constructor() {

    sealed class Intent {
        data class Call(val recipient: String) : Intent()
        data class SendSms(val recipient: String, val message: String) : Intent()
        data class SetAlarm(val timeExpression: String) : Intent()
        data class SetTorch(val state: Boolean) : Intent()
        data class SetVolume(val level: Int) : Intent() // 0-100 percentage
        data class SetBrightness(val level: Int) : Intent() // 0-100 percentage
        object Mute : Intent()
        object Unmute : Intent()
        data class ToggleDnd(val enabled: Boolean) : Intent()
        data class ToggleBatterySaver(val enabled: Boolean) : Intent()
        data class OpenApp(val appName: String) : Intent()
        data class RecallMemory(val query: String) : Intent()
        data class Calculate(val expression: String) : Intent()
        data class WebSearch(val query: String) : Intent()
        data class FetchUrl(val url: String) : Intent()
        data class AskLlm(val prompt: String, val forceLayer: String? = null) : Intent()
        object Unknown : Intent()
    }

    private val PHONE_PATTERN = Pattern.compile("(?i)\\b(\\+?\\d{1,3}[- ]?)?\\(?\\d{3}\\)?[- ]?\\d{3}[- ]?\\d{4}\\b")
    private val URL_PATTERN = Pattern.compile("(?i)\\b(https?://)?[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,6}(/[a-zA-Z0-9_.-]*)*\\b")

    fun classify(input: String): Intent {
        val trimmed = input.trim()
        if (trimmed.isEmpty()) return Intent.Unknown

        // STAGE 1: Explicit Command Prefixes (< 1ms)
        val stage1Result = stage1PrefixRouting(trimmed)
        if (stage1Result != null) return stage1Result

        // STAGE 2: Keyword Map Matching (< 2ms)
        val stage2Result = stage2KeywordMatching(trimmed)
        if (stage2Result != null) return stage2Result

        // STAGE 3: Regex & Pattern Analysis (< 5ms)
        val stage3Result = stage3PatternAnalysis(trimmed)
        if (stage3Result != null) return stage3Result

        // STAGE 4: Lightweight ML / Fuzzy Matcher (< 20ms)
        // Note: Real TFLite implementation would go here in Module 6
        val stage4Result = stage4FuzzyMatching(trimmed)
        if (stage4Result != null) return stage4Result

        // STAGE 5: Fallback
        return Intent.Unknown
    }

    private fun stage1PrefixRouting(input: String): Intent? {
        if (!input.startsWith("!")) return null
        
        val spaceIndex = input.indexOf(' ')
        val prefix = if (spaceIndex != -1) input.substring(0, spaceIndex) else input
        val remaining = if (spaceIndex != -1) input.substring(spaceIndex + 1).trim() else ""
        
        return when (prefix.lowercase()) {
            "!local" -> Intent.AskLlm(remaining, "local")
            "!cloud" -> Intent.AskLlm(remaining, "cloud")
            "!agent" -> Intent.AskLlm(remaining, "agentic")
            "!openai" -> Intent.AskLlm(remaining, "openai")
            "!anthropic" -> Intent.AskLlm(remaining, "anthropic")
            "!gemini" -> Intent.AskLlm(remaining, "gemini")
            "!groq" -> Intent.AskLlm(remaining, "groq")
            "!openrouter" -> Intent.AskLlm(remaining, "openrouter")
            else -> Intent.AskLlm(remaining, prefix.substring(1))
        }
    }

    private fun stage2KeywordMatching(input: String): Intent? {
        val lowercase = input.lowercase()
        val keywords = mapOf(
            "mute" to Intent.Mute,
            "silent" to Intent.Mute,
            "unmute" to Intent.Unmute,
            "speak" to Intent.Unmute,
            "torch" to Intent.SetTorch(true),
            "flashlight" to Intent.SetTorch(true),
            "dnd" to Intent.ToggleDnd(true)
        )
        return keywords[lowercase]
    }

    private fun stage3PatternAnalysis(input: String): Intent? {
        val lowercase = input.lowercase()

        // Call patterns
        if (lowercase.startsWith("call ") || lowercase.startsWith("dial ")) {
            val target = input.substring(lowercase.indexOf(' ') + 1).trim()
            return Intent.Call(target)
        }

        // SMS patterns
        if (lowercase.startsWith("send sms ") || lowercase.startsWith("message ") || lowercase.startsWith("text ")) {
            val content = input.substring(lowercase.indexOf(' ') + 1).trim()
            val phoneMatcher = PHONE_PATTERN.matcher(content)
            if (phoneMatcher.find()) {
                val phone = phoneMatcher.group()
                val msg = content.replace(phone, "").trim()
                return Intent.SendSms(phone, if (msg.isEmpty()) "Ping from NOUS" else msg)
            }
        }

        // Brightness/Volume with numbers
        val numberMatcher = Pattern.compile("\\b(\\d{1,3})\\b").matcher(lowercase)
        if (numberMatcher.find()) {
            val num = numberMatcher.group(1).toIntOrNull() ?: 50
            if (lowercase.contains("volume")) return Intent.SetVolume(num.coerceIn(0, 100))
            if (lowercase.contains("brightness")) return Intent.SetBrightness(num.coerceIn(0, 100))
        }

        return null
    }

    private fun stage4FuzzyMatching(input: String): Intent? {
        val lowercase = input.lowercase()
        
        if (lowercase.contains("remember") || lowercase.contains("what is my")) {
            return Intent.RecallMemory(input)
        }
        
        if (lowercase.contains("calculate") || lowercase.matches(Regex(".*[0-9]+[\\s]*[+\\-*/][\\s]*[0-9]+.*"))) {
            return Intent.Calculate(input)
        }

        if (lowercase.startsWith("open ")) {
            val app = input.substring(5).replace("app", "").trim()
            if (app.isNotEmpty()) return Intent.OpenApp(app)
        }

        return null
    }
}

