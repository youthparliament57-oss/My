import sys

content = open("app/src/main/java/com/example/brain/AgenticOrchestrator.kt").read()
target = """            val agentInstruction = \"\"\"
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
                
                Past Steps Execution Logs:
                ${formatTraces(traces)}
                
                Based on past steps, declare your next action. Use EXACTLY one of these formats:
                THOUGHT: [explain what you know and what you need next]
                TOOL: [ToolName] | ARGS: [precise arguments]
                
                OR if you have the final complete answer:
                THOUGHT: [summarize reasoning]
                FINAL ANSWER: [write your elegant final answer to the user]
            \"\"\".trimIndent()"""

replacement = """            val lastSteps = traces.takeLast(3)
            val tracesSummary = if (lastSteps.isEmpty()) "No steps executed yet." else {
                lastSteps.joinToString("\\n") { step ->
                    "Step ${step.iteration}: Thought: ${step.thought} | Tool: ${step.toolName} | Observation: ${step.observation ?: "pending"}"
                }
            }

            val agentInstruction = \"\"\"
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
            \"\"\".trimIndent()"""

if target in content:
    content = content.replace(target, replacement)
    open("app/src/main/java/com/example/brain/AgenticOrchestrator.kt", "w").write(content)
    print("Success")
else:
    print("Target not found")
