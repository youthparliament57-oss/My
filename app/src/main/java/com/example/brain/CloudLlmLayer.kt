package com.example.brain

import android.util.Log
import com.example.BuildConfig
import com.example.brain.security.CredentialVault
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject
import java.io.IOException
import java.util.concurrent.TimeUnit
import java.util.regex.Pattern
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CloudLlmLayer @Inject constructor(
    private val credentialVault: CredentialVault
) {

    private val client: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .build()

    // Regex definitions for robust PII detection
    private val PHONE_PATTERN = Pattern.compile("\\b(\\+?\\d{1,3}[- ]?)?\\(?\\d{3}\\)?[- ]?\\d{3}[- ]?\\d{4}\\b")
    private val EMAIL_PATTERN = Pattern.compile("\\b[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,6}\\b")
    private val AADHAAR_PATTERN = Pattern.compile("\\b\\d{4}[- ]?\\d{4}[- ]?\\d{4}\\b")
    private val CREDIT_CARD_PATTERN = Pattern.compile("\\b(?:\\d[ -]?){13,16}\\b")
    private val OTP_PATTERN = Pattern.compile("\\b\\d{4,6}\\b")

    // Dynamic cost config per 1K tokens (Input, Output)
    private val PROVIDER_COSTS = mapOf(
        "openai" to Pair(0.00015 / 1000, 0.00060 / 1000), // GPT-4o-mini
        "anthropic" to Pair(0.00080 / 1000, 0.00400 / 1000), // Claude 3.5 Haiku
        "gemini" to Pair(0.000075 / 1000, 0.00030 / 1000), // Gemini 1.5 Flash
        "groq" to Pair(0.00059 / 1000, 0.00079 / 1000), // Llama 3.1 70B on Groq
        "openrouter" to Pair(0.00020 / 1000, 0.00020 / 1000)
    )

    suspend fun processCloudQuery(
        prompt: String,
        provider: String,
        brainContext: BrainContext
    ): String {
        // 1. Budget and Token Check
        val (inputCostRate, outputCostRate) = PROVIDER_COSTS[provider.lowercase()] ?: Pair(0.0, 0.0)
        val estimatedInputTokens = prompt.length / 4
        val estimatedCost = estimatedInputTokens * inputCostRate

        if (brainContext.budget.remainingCost < estimatedCost) {
            Log.w("CloudLlmLayer", "Exceeded conversation cost limit of \$${brainContext.budget.maxCostPerTurn}. Gracefully falling back to LocalLlmLayer.")
            return "Budget limit exceeded. Switching to secure localized processing."
        }

        // 2. Perform Bi-directional PII Scrubbing
        val scrubbedData = scrubPii(prompt)
        val requestPrompt = scrubbedData.scrubbedText
        Log.d("CloudLlmLayer", "Scrubbing completed. PII Items Masked: ${scrubbedData.replacements.size}")

        // 3. Resolve API Key from CredentialVault
        val resolvedApiKey = credentialVault.getApiKey(provider) ?: throw IllegalStateException("No API key configured for provider: $provider")

        // 4. Dispatch HTTP request to API Gateway or Provider Endpoint
        val rawResponse = try {
            when (provider.lowercase()) {
                "openai" -> makeOpenAiRequest(requestPrompt, resolvedApiKey, brainContext.activePersona)
                "anthropic" -> makeAnthropicRequest(requestPrompt, resolvedApiKey, brainContext.activePersona)
                "gemini" -> makeGeminiRequest(requestPrompt, resolvedApiKey, brainContext.activePersona)
                "groq" -> makeGroqRequest(requestPrompt, resolvedApiKey, brainContext.activePersona)
                "openrouter" -> makeOpenRouterRequest(requestPrompt, resolvedApiKey, brainContext.activePersona)
                else -> makeGeminiRequest(requestPrompt, resolvedApiKey, brainContext.activePersona)
            }
        } catch (e: Exception) {
            Log.e("CloudLlmLayer", "Network routing failed to target: $provider. Error: ${e.message}")
            throw e
        }

        // 5. Restore PII context
        val finalResponse = restorePii(rawResponse, scrubbedData.replacements)
        
        // Update Budget Usage
        val outputTokens = finalResponse.length / 4
        val actualCost = estimatedCost + (outputTokens * outputCostRate)
        // Note: brainContext.budget cannot be mutated directly now. The caller should manage it.

        return finalResponse
    }

    private data class ScrubResult(val scrubbedText: String, val replacements: Map<String, String>)

    private fun scrubPii(text: String): ScrubResult {
        var tempText = text
        val replacements = mutableMapOf<String, String>()
        var counter = 0

        fun applyScrub(pattern: Pattern, type: String) {
            val matcher = pattern.matcher(tempText)
            val buffer = StringBuffer()
            while (matcher.find()) {
                val match = matcher.group()
                val token = "<${type}_$counter>"
                replacements[token] = match
                matcher.appendReplacement(buffer, token)
                counter++
            }
            matcher.appendTail(buffer)
            tempText = buffer.toString()
        }

        applyScrub(CREDIT_CARD_PATTERN, "CREDIT_CARD")
        applyScrub(AADHAAR_PATTERN, "AADHAAR")
        applyScrub(EMAIL_PATTERN, "EMAIL")
        applyScrub(PHONE_PATTERN, "PHONE")
        applyScrub(OTP_PATTERN, "OTP")

        return ScrubResult(tempText, replacements)
    }

    private fun restorePii(text: String, replacements: Map<String, String>): String {
        var restoredText = text
        for ((key, value) in replacements) {
            restoredText = restoredText.replace(key, value)
        }
        return restoredText
    }

    // --- Provider HTTP Implementations ---

    private suspend fun makeOpenAiRequest(prompt: String, apiKey: String, persona: com.example.persona.Persona): String = withContext(Dispatchers.IO) {
        val url = "https://api.openai.com/v1/chat/completions"
        val systemMessage = "${ConstitutionalGuardrails.GOLDEN_RULE}\n\n${persona.systemPromptExtension}"
        
        val json = JSONObject().apply {
            put("model", "gpt-4o-mini")
            put("messages", JSONArray().apply {
                put(JSONObject().apply {
                    put("role", "system")
                    put("content", systemMessage)
                })
                put(JSONObject().apply {
                    put("role", "user")
                    put("content", prompt)
                })
            })
            put("temperature", persona.temperature)
            put("top_p", persona.topP)
            put("max_tokens", persona.maxTokens)
        }

        val request = Request.Builder()
            .url(url)
            .addHeader("Authorization", "Bearer $apiKey")
            .post(json.toString().toRequestBody("application/json".toMediaType()))
            .build()

        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) throw IOException("OpenAI API returned code ${response.code}")
            val bodyStr = response.body?.string() ?: throw IOException("Empty response body")
            try {
                JSONObject(bodyStr)
                    .getJSONArray("choices")
                    .getJSONObject(0)
                    .getJSONObject("message")
                    .getString("content")
            } catch (e: JSONException) {
                throw IOException("Malformed JSON response from OpenAI: ${e.message}", e)
            }
        }
    }

    private suspend fun makeAnthropicRequest(prompt: String, apiKey: String, persona: com.example.persona.Persona): String = withContext(Dispatchers.IO) {
        val url = "https://api.anthropic.com/v1/messages"
        val systemMessage = "${ConstitutionalGuardrails.GOLDEN_RULE}\n\n${persona.systemPromptExtension}"
        
        val json = JSONObject().apply {
            put("model", "claude-3-5-haiku-20241022")
            put("max_tokens", persona.maxTokens)
            put("messages", JSONArray().apply {
                put(JSONObject().apply {
                    put("role", "user")
                    put("content", prompt)
                })
            })
            put("system", systemMessage)
            put("temperature", persona.temperature)
            put("top_p", persona.topP)
        }

        val request = Request.Builder()
            .url(url)
            .addHeader("x-api-key", apiKey)
            .addHeader("anthropic-version", "2023-06-01")
            .post(json.toString().toRequestBody("application/json".toMediaType()))
            .build()

        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) throw IOException("Anthropic API returned code ${response.code}")
            val bodyStr = response.body?.string() ?: throw IOException("Empty response body")
            try {
                JSONObject(bodyStr)
                    .getJSONArray("content")
                    .getJSONObject(0)
                    .getString("text")
            } catch (e: JSONException) {
                throw IOException("Malformed JSON response from Anthropic: ${e.message}", e)
            }
        }
    }

    private suspend fun makeGeminiRequest(prompt: String, apiKey: String, persona: com.example.persona.Persona): String = withContext(Dispatchers.IO) {
        val url = "https://generativelanguage.googleapis.com/v1beta/models/gemini-1.5-flash:generateContent?key=$apiKey"
        val systemMessage = "${ConstitutionalGuardrails.GOLDEN_RULE}\n\n${persona.systemPromptExtension}"
        
        val json = JSONObject().apply {
            put("contents", JSONArray().apply {
                put(JSONObject().apply {
                    put("parts", JSONArray().apply {
                        put(JSONObject().apply {
                            put("text", "$systemMessage\n\n$prompt")
                        })
                    })
                })
            })
            put("generationConfig", JSONObject().apply {
                put("temperature", persona.temperature)
                put("topP", persona.topP)
                put("maxOutputTokens", persona.maxTokens)
            })
        }

        val request = Request.Builder()
            .url(url)
            .post(json.toString().toRequestBody("application/json".toMediaType()))
            .build()

        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) throw IOException("Gemini API returned code ${response.code}")
            val bodyStr = response.body?.string() ?: throw IOException("Empty response body")
            try {
                JSONObject(bodyStr)
                    .getJSONArray("candidates")
                    .getJSONObject(0)
                    .getJSONObject("content")
                    .getJSONArray("parts")
                    .getJSONObject(0)
                    .getString("text")
            } catch (e: JSONException) {
                throw IOException("Malformed JSON response from Gemini: ${e.message}", e)
            }
        }
    }

    private suspend fun makeGroqRequest(prompt: String, apiKey: String, persona: com.example.persona.Persona): String = withContext(Dispatchers.IO) {
        val url = "https://api.groq.com/openai/v1/chat/completions"
        val systemMessage = "${ConstitutionalGuardrails.GOLDEN_RULE}\n\n${persona.systemPromptExtension}"
        
        val json = JSONObject().apply {
            put("model", "llama-3.1-70b-versatile")
            put("messages", JSONArray().apply {
                put(JSONObject().apply {
                    put("role", "system")
                    put("content", systemMessage)
                })
                put(JSONObject().apply {
                    put("role", "user")
                    put("content", prompt)
                })
            })
            put("temperature", persona.temperature)
            put("top_p", persona.topP)
            put("max_tokens", persona.maxTokens)
        }

        val request = Request.Builder()
            .url(url)
            .addHeader("Authorization", "Bearer $apiKey")
            .post(json.toString().toRequestBody("application/json".toMediaType()))
            .build()

        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) throw IOException("Groq API returned code ${response.code}")
            val bodyStr = response.body?.string() ?: throw IOException("Empty response body")
            try {
                JSONObject(bodyStr)
                    .getJSONArray("choices")
                    .getJSONObject(0)
                    .getJSONObject("message")
                    .getString("content")
            } catch (e: JSONException) {
                throw IOException("Malformed JSON response from Groq: ${e.message}", e)
            }
        }
    }

    private suspend fun makeOpenRouterRequest(prompt: String, apiKey: String, persona: com.example.persona.Persona): String = withContext(Dispatchers.IO) {
        val url = "https://openrouter.ai/api/v1/chat/completions"
        val systemMessage = "${ConstitutionalGuardrails.GOLDEN_RULE}\n\n${persona.systemPromptExtension}"
        
        val json = JSONObject().apply {
            put("model", "meta-llama/llama-3.1-8b-instruct:free")
            put("messages", JSONArray().apply {
                put(JSONObject().apply {
                    put("role", "system")
                    put("content", systemMessage)
                })
                put(JSONObject().apply {
                    put("role", "user")
                    put("content", prompt)
                })
            })
            put("temperature", persona.temperature)
            put("top_p", persona.topP)
            put("max_tokens", persona.maxTokens)
        }

        val request = Request.Builder()
            .url(url)
            .addHeader("Authorization", "Bearer $apiKey")
            .addHeader("HTTP-Referer", "https://ai.studio/build")
            .post(json.toString().toRequestBody("application/json".toMediaType()))
            .build()

        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) throw IOException("OpenRouter API returned code ${response.code}")
            val bodyStr = response.body?.string() ?: throw IOException("Empty response body")
            try {
                JSONObject(bodyStr)
                    .getJSONArray("choices")
                    .getJSONObject(0)
                    .getJSONObject("message")
                    .getString("content")
            } catch (e: JSONException) {
                throw IOException("Malformed JSON response from OpenRouter: ${e.message}", e)
            }
        }
    }
}
