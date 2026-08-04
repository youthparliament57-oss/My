package com.example.brain
import kotlinx.coroutines.flow.firstOrNull
import javax.inject.Inject
import java.util.regex.Pattern
import javax.inject.Singleton

import android.util.Log
import com.example.domain.model.Thought
import com.example.domain.repository.NousRepository
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.util.concurrent.TimeUnit

interface AgentTool {
    val name: String
    val description: String
    suspend fun execute(args: String, context: BrainContext): String
}


@Singleton
class AgenticOrchestrator @Inject constructor(
    private val memoryInterface: com.example.brain.memory.MemoryInterface,
    private val cloudLlmLayer: CloudLlmLayer,
    private val cognitiveFacade: com.example.cognitive.CognitiveFacade,
    private val agentFacade: com.example.agent.AgentFacade
) {
    private val client = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .build()

    // 5 Built-in Tools
    private val tools: Map<String, AgentTool> = mapOf(
        "RecallMemory" to RecallMemoryTool(memoryInterface),
        "Calculate" to CalculateTool(),
        "WebSearch" to WebSearchTool(client),
        "FetchUrl" to FetchUrlTool(client),
        "DeepReasoning" to DeepReasoningTool(cognitiveFacade),
        "PlaceCall" to PlaceCallTool(agentFacade),
        "SendSms" to SendSmsTool(agentFacade),
        "AutomationTap" to AutomationTapTool(agentFacade),
        "AutomationSwipe" to AutomationSwipeTool(agentFacade),
        "CaptureUiTree" to CaptureUiTreeTool(agentFacade),
        "GetSystemStatus" to GetSystemStatusTool(agentFacade)
    )

    data class AgentStep(
        val iteration: Int,
        val thought: String,
        val toolName: String?,
        val toolArgs: String?,
        val observation: String?
    )

    suspend fun executeAgentLoop(query: String, brainContext: BrainContext): String {
        Log.i("AgenticOrchestrator", "Spinning up Layer 5 Agentic orchestrator. Target: $query")
        val traces = mutableListOf<AgentStep>()
        var currentPrompt = query
        var iteration = 1
        val maxIterations = 10

        while (iteration <= maxIterations) {
            // Guardrail check on every single iteration step
            val safetyCheck = ConstitutionalGuardrails.checkSafety(currentPrompt)
            if (safetyCheck is ConstitutionalGuardrails.SafetyResult.Blocked) {
                return safetyCheck.response
            }

            // Assemble a rich prompt that instructs the LLM to use tools in format:
            // THOUGHT: [reasoning]
            // TOOL: [ToolName] | ARGS: [arguments]
            // or FINAL ANSWER: [answer]
            val lastSteps = traces.takeLast(3)
            val tracesSummary = if (lastSteps.isEmpty()) "No steps executed yet." else {
                lastSteps.joinToString("\n") { step ->
                    "Step ${step.iteration}: Thought: ${step.thought} | Tool: ${step.toolName} | Observation: ${step.observation ?: "pending"}"
                }
            }

            val agentInstruction = """
                You are executing a multi-step planning loop for user query: "$query".
                Active Iteration: $iteration/$maxIterations.
                
                Available tools:
                - RecallMemory: Search stored memory nodes. Args: string keywords.
                - Calculate: Solve math. Args: standard math expression.
                - WebSearch: Search internet for live data. Args: search queries.
                - FetchUrl: Get content of URL. Args: URL string.
                - DeepReasoning: Deep analysis for complex logic.
                - PlaceCall: Dial a phone number. Args: phone number.
                - SendSms: Send text message. Args: "number | message".
                - AutomationTap: Click screen at (x,y). Args: "x | y".
                - AutomationSwipe: Drag on screen. Args: "x1 | y1 | x2 | y2 | duration".
                - CaptureUiTree: Get current screen hierarchy (JSON). No args.
                - GetSystemStatus: Check battery, thermal, network. No args.
                
                Past ${lastSteps.size} Steps Summary (recent only):
                $tracesSummary
                
                Based on past steps, declare your next action. Use EXACTLY one of these formats:
                THOUGHT: [explain what you know and what you need next]
                TOOL: [ToolName] | ARGS: [precise arguments]
                
                OR if you have the final complete answer:
                THOUGHT: [summarize reasoning]
                FINAL ANSWER: [write your elegant final answer to the user]
            """.trimIndent()

            // Call Cloud LLM layer to generate planning step
            val response = try {
                cloudLlmLayer.processCloudQuery(agentInstruction, "gemini", brainContext)
            } catch (e: Exception) {
                Log.e("AgenticOrchestrator", "Planning call failed. Terminating agent loop. Fallback triggered.")
                return "Agent processing failed due to remote engine connection loss: ${e.message}"
            }

            // Parse planning decision
            val parsedDecision = parseDecision(response)
            
            if (parsedDecision.isFinalAnswer && parsedDecision.finalAnswer != null) {
                Log.i("AgenticOrchestrator", "Final Agent Answer generated in $iteration iterations.")
                return parsedDecision.finalAnswer
            }

            val toolName = parsedDecision.toolName
            val toolArgs = parsedDecision.toolArgs

            if (toolName != null && tools.containsKey(toolName) && toolArgs != null) {
                Log.i("AgenticOrchestrator", "Executing Tool: $toolName with Args: $toolArgs")
                val observation = try {
                    tools[toolName]?.execute(toolArgs, brainContext) ?: "Tool not found."
                } catch (e: Exception) {
                    "Tool execution failed: ${e.message}. Replanning initiated."
                }

                traces.add(AgentStep(
                    iteration = iteration,
                    thought = parsedDecision.thought,
                    toolName = toolName,
                    toolArgs = toolArgs,
                    observation = observation
                ))
            } else {
                // If LLM returned bad format or tool doesn't exist, log error as observation to force correct format
                val errorObs = if (toolName != null) "Tool '$toolName' is not a valid tool. Choose from: ${tools.keys}." 
                               else "Invalid format. You MUST use 'TOOL: [Name] | ARGS: [Args]' or 'FINAL ANSWER: [Answer]'."
                traces.add(AgentStep(
                    iteration = iteration,
                    thought = parsedDecision.thought.ifEmpty { "Evaluating state." },
                    toolName = null,
                    toolArgs = null,
                    observation = errorObs
                ))
            }

            iteration++
        }

        return "Agentic loop exceeded maximum execution safety depth of $maxIterations steps. Try narrowing down your request."
    }

    private fun formatTraces(traces: List<AgentStep>): String {
        if (traces.isEmpty()) return "No steps executed yet."
        return traces.joinToString("\n") { step ->
            "Step ${step.iteration}:\n  Thought: ${step.thought}\n  Action: ${step.toolName ?: "None"}(${step.toolArgs ?: ""})\n  Observation: ${step.observation ?: ""}"
        }
    }

    data class Decision(
        val thought: String,
        val toolName: String?,
        val toolArgs: String?,
        val isFinalAnswer: Boolean,
        val finalAnswer: String?
    )

    private fun parseDecision(response: String): Decision {
        var thought = ""
        var toolName: String? = null
        var toolArgs: String? = null
        var isFinal = false
        var finalAnswer: String? = null

        val lines = response.lines()
        for (line in lines) {
            val trimmed = line.trim()
            when {
                trimmed.startsWith("THOUGHT:") -> {
                    thought = trimmed.substring(8).trim()
                }
                trimmed.startsWith("TOOL:") -> {
                    val parts = trimmed.substring(5).split("|")
                    toolName = parts.firstOrNull()?.trim()
                    if (parts.size > 1) {
                        val argPart = parts[1].trim()
                        if (argPart.startsWith("ARGS:")) {
                            toolArgs = argPart.substring(5).trim()
                        } else {
                            toolArgs = argPart
                        }
                    }
                }
                trimmed.startsWith("FINAL ANSWER:") -> {
                    isFinal = true
                    finalAnswer = trimmed.substring(13).trim()
                }
            }
        }

        // Broad fallback patterns if the model didn't use strict lines
        if (!isFinal && toolName == null) {
            if (response.contains("FINAL ANSWER:")) {
                isFinal = true
                finalAnswer = response.substringAfter("FINAL ANSWER:").trim()
            } else if (response.contains("FINAL_ANSWER")) {
                isFinal = true
                finalAnswer = response.substringAfter("FINAL_ANSWER").replace(":", "").trim()
            } else {
                // If it looks like a text answer, treat it as final
                isFinal = true
                finalAnswer = response.trim()
            }
        }

        return Decision(thought, toolName, toolArgs, isFinal, finalAnswer)
    }

    // --- Concrete Tools ---

    class RecallMemoryTool(private val memoryInterface: com.example.brain.memory.MemoryInterface) : AgentTool {
        override val name = "RecallMemory"
        override val description = "Searches stored mental thoughts in the space database."

        override suspend fun execute(args: String, context: BrainContext): String {
            return try {
                val memoryContext = memoryInterface.recall(args)
                val results = mutableListOf<String>()
                results.addAll(memoryContext.relevantEpisodicEvents)
                results.addAll(memoryContext.relevantSemanticFacts)
                results.addAll(memoryContext.graphInsights)
                
                if (results.isEmpty()) {
                    "No matching memory nodes found for search keywords: '$args'."
                } else {
                    results.joinToString("\n") { "- $it" }
                }
            } catch (e: Exception) {
                "Failed to access local memory indexes: ${e.message}"
            }
        }
    }

    class CalculateTool : AgentTool {
        override val name = "Calculate"
        override val description = "Computes basic mathematical expressions."

        override suspend fun execute(args: String, context: BrainContext): String {
            val clean = args.replace(" ", "")
            return try {
                val result = evaluateSimpleMath(clean)
                "Result: $result"
            } catch (e: Exception) {
                "Math parse error: ${e.message}. Use standard format: 'number operator number'."
            }
        }

private fun evaluateSimpleMath(expr: String): Double {
            return MathParser(expr).parse()
        }

        private class MathParser(val str: String) {
            var pos = -1
            var ch = 0
            
            fun nextChar() {
                ch = if (++pos < str.length) str[pos].code else -1
            }
            
            fun eat(charToEat: Int): Boolean {
                while (ch == ' '.code) nextChar()
                if (ch == charToEat) {
                    nextChar()
                    return true
                }
                return false
            }
            
            fun parse(): Double {
                nextChar()
                val x = parseExpression()
                if (pos < str.length) throw RuntimeException("Unexpected: " + ch.toChar())
                return x
            }
            
            fun parseExpression(): Double {
                var x = parseTerm()
                while (true) {
                    if (eat('+'.code)) x += parseTerm() // addition
                    else if (eat('-'.code)) x -= parseTerm() // subtraction
                    else return x
                }
            }
            
            fun parseTerm(): Double {
                var x = parseFactor()
                while (true) {
                    if (eat('*'.code)) x *= parseFactor() // multiplication
                    else if (eat('/'.code)) x /= parseFactor() // division
                    else return x
                }
            }
            
            fun parseFactor(): Double {
                if (eat('+'.code)) return parseFactor() // unary plus
                if (eat('-'.code)) return -parseFactor() // unary minus
                var x: Double
                val startPos = pos
                if (eat('('.code)) { // parentheses
                    x = parseExpression()
                    eat(')'.code)
                } else if ((ch >= '0'.code && ch <= '9'.code) || ch == '.'.code) { // numbers
                    while ((ch >= '0'.code && ch <= '9'.code) || ch == '.'.code) nextChar()
                    x = str.substring(startPos, pos).toDouble()
                } else {
                    throw RuntimeException("Unexpected: " + ch.toChar())
                }
                if (eat('^'.code)) x = Math.pow(x, parseFactor()) // exponentiation
                return x
            }
        }
    }

    class WebSearchTool(private val client: OkHttpClient) : AgentTool {
        override val name = "WebSearch"
        override val description = "Searches the web for current data."

        override suspend fun execute(args: String, context: BrainContext): String = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
            val searchUrl = "https://html.duckduckgo.com/html/?q=${java.net.URLEncoder.encode(args, "UTF-8")}"
            val request = Request.Builder()
                .url(searchUrl)
                .addHeader("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36")
                .build()

            return@withContext try {
                client.newCall(request).execute().use { response ->
                    if (!response.isSuccessful) return@use "WebSearch returned error status ${response.code}."
                    val html = response.body?.string() ?: ""
                    val textBlocks = extractTextSnippets(html)
                    if (textBlocks.isEmpty()) "No web search snippets resolved." else textBlocks
                }
            } catch (e: Exception) {
                "Web search request timeout/failed: ${e.message}"
            }
        }

        private fun extractTextSnippets(html: String): String {
            val snippets = mutableListOf<String>()
            val pattern = Pattern.compile("<a class=\"result__snippet\"[^>]*>(.*?)</a>")
            val matcher = pattern.matcher(html)
            var count = 0
            while (matcher.find() && count < 5) {
                val cleanText = matcher.group(1)
                    .replace(Regex("<[^>]*>"), "") // strip nested tags
                    .replace("&amp;", "&")
                    .replace("&quot;", "\"")
                snippets.add(cleanText)
                count++
            }
            return snippets.joinToString("\n") { "- $it" }
        }
    }

    class FetchUrlTool(private val client: OkHttpClient) : AgentTool {
        override val name = "FetchUrl"
        override val description = "Downloads raw text content from target URL."

        override suspend fun execute(args: String, context: BrainContext): String = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
            val request = Request.Builder()
                .url(args)
                .addHeader("User-Agent", "Mozilla/5.0")
                .build()

            return@withContext try {
                client.newCall(request).execute().use { response ->
                    if (!response.isSuccessful) return@use "FetchUrl error status: ${response.code}."
                    val body = response.body?.string() ?: ""
                    val clean = body.replace(Regex("<script[^>]*>([\\s\\S]*?)</script>"), "")
                        .replace(Regex("<style[^>]*>([\\s\\S]*?)</style>"), "")
                        .replace(Regex("<[^>]*>"), "")
                        .replace(Regex("\\s+"), " ")
                        .trim()

                    if (clean.length > 1500) clean.substring(0, 1500) + "... [truncated]" else clean
                }
            } catch (e: Exception) {
                "Failed to download URL target: ${e.message}"
            }
        }
    }

    class DeepReasoningTool(private val cognitiveFacade: com.example.cognitive.CognitiveFacade) : AgentTool {
        override val name = "DeepReasoning"
        override val description = "Performs deep multi-step reasoning, decision making, and uncertainty analysis. Args: complex query."

        override suspend fun execute(args: String, context: BrainContext): String {
            val result = cognitiveFacade.processCognitiveTask(args, context)
            return when (result) {
                is com.example.cognitive.models.CognitiveResult.Trace -> {
                    "Reasoning Trace Summary:\n" +
                    "Query: ${result.trace.query}\n" +
                    "Confidence: ${result.trace.confidenceScore}\n" +
                    "Answer: ${result.finalAnswer}"
                }
                is com.example.cognitive.models.CognitiveResult.NeedsClarification -> {
                    "Ambiguity detected: ${result.question.promptText}"
                }
                is com.example.cognitive.models.CognitiveResult.Uncertainty -> {
                    "Uncertainty: ${result.message}"
                }
                else -> "Reasoning failed to yield a specific trace."
            }
        }
    }

    class PlaceCallTool(private val agent: com.example.agent.AgentFacade) : AgentTool {
        override val name = "PlaceCall"
        override val description = "Dials a phone number."

        override suspend fun execute(args: String, context: BrainContext): String {
            agent.telephony.call.placeCall(args.trim())
            return "Call initiated to $args"
        }
    }

    class SendSmsTool(private val agent: com.example.agent.AgentFacade) : AgentTool {
        override val name = "SendSms"
        override val description = "Sends an SMS. Args: 'number | message'"

        override suspend fun execute(args: String, context: BrainContext): String {
            val parts = args.split("|")
            if (parts.size < 2) return "Error: Args must be 'number | message'"
            agent.telephony.sms.sendSms(parts[0].trim(), parts[1].trim())
            return "SMS sent to ${parts[0].trim()}"
        }
    }

    class AutomationTapTool(private val agent: com.example.agent.AgentFacade) : AgentTool {
        override val name = "AutomationTap"
        override val description = "Performs a screen tap. Args: 'x | y'"

        override suspend fun execute(args: String, context: BrainContext): String {
            val parts = args.split("|")
            if (parts.size < 2) return "Error: Args must be 'x | y'"
            val x = parts[0].trim().toIntOrNull() ?: return "Invalid X"
            val y = parts[1].trim().toIntOrNull() ?: return "Invalid Y"
            val success = agent.automation.tap(x, y)
            return if (success) "Tap at ($x, $y) executed." else "Tap failed (No permission or guard blocked)."
        }
    }

    class AutomationSwipeTool(private val agent: com.example.agent.AgentFacade) : AgentTool {
        override val name = "AutomationSwipe"
        override val description = "Performs a screen swipe. Args: 'x1 | y1 | x2 | y2 | duration'"

        override suspend fun execute(args: String, context: BrainContext): String {
            val parts = args.split("|")
            if (parts.size < 5) return "Error: Args must be 'x1 | y1 | x2 | y2 | duration'"
            val x1 = parts[0].trim().toIntOrNull() ?: return "Invalid X1"
            val y1 = parts[1].trim().toIntOrNull() ?: return "Invalid Y1"
            val x2 = parts[2].trim().toIntOrNull() ?: return "Invalid X2"
            val y2 = parts[3].trim().toIntOrNull() ?: return "Invalid Y2"
            val dur = parts[4].trim().toLongOrNull() ?: return "Invalid Duration"
            val success = agent.automation.swipe(x1, y1, x2, y2, dur)
            return if (success) "Swipe from ($x1,$y1) to ($x2,$y2) executed." else "Swipe failed."
        }
    }

    class CaptureUiTreeTool(private val agent: com.example.agent.AgentFacade) : AgentTool {
        override val name = "CaptureUiTree"
        override val description = "Captures the current screen hierarchy for understanding."

        override suspend fun execute(args: String, context: BrainContext): String {
            return try {
                val tree = agent.automation.getProvider().getUiTree()
                "UI Tree Captured. Package: ${tree.packageName}. Root: ${tree.rootNode.className}"
            } catch (e: Exception) {
                "Error capturing UI Tree: ${e.message}"
            }
        }
    }

    class GetSystemStatusTool(private val agent: com.example.agent.AgentFacade) : AgentTool {
        override val name = "GetSystemStatus"
        override val description = "Retrieves live device telemetry."

        override suspend fun execute(args: String, context: BrainContext): String {
            return agent.getStatusSummary()
        }
    }
}
