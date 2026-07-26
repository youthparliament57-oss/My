import sys

content = open("app/src/main/java/com/example/brain/memory/MemoryInterfaceImpl.kt").read()

target = """        for (event in recentEpisodic) {
            val text = event.eventText
            if (text.contains("prefer", ignoreCase = true) || text.contains("like", ignoreCase = true) || text.contains("always", ignoreCase = true)) {
                extractedFacts.add("Extracted Fact from event: $text")
                storeSemanticFact(
                    factText = "User preferences/habits noticed: $text",
                    category = "Preference",
                    confidence = 0.8f
                )
                processedCount++
            }
        }"""

replacement = """        for (event in recentEpisodic) {
            val text = event.eventText
            if (localLlm != null) {
                try {
                    val prompt = "Extract core semantic facts from this episodic memory. Return ONLY the extracted facts (no preamble). Memory: '$text'"
                    val dummyContext = com.example.brain.BrainContext(null, "", null) // Minimal context
                    val extracted = localLlm.processLocalQuery(prompt, dummyContext)
                    if (extracted.isNotBlank() && !extracted.startsWith("Error")) {
                        extractedFacts.add(extracted)
                        storeSemanticFact(
                            factText = extracted,
                            category = "Extracted",
                            confidence = 0.8f
                        )
                        processedCount++
                    }
                } catch (e: Exception) {
                    android.util.Log.e("MemoryInterface", "LLM fact extraction failed", e)
                }
            } else {
                if (text.contains("prefer", ignoreCase = true) || text.contains("like", ignoreCase = true) || text.contains("always", ignoreCase = true)) {
                    extractedFacts.add("Extracted Fact from event: $text")
                    storeSemanticFact(
                        factText = "User preferences/habits noticed: $text",
                        category = "Preference",
                        confidence = 0.8f
                    )
                    processedCount++
                }
            }
        }"""

if target in content:
    content = content.replace(target, replacement)
    open("app/src/main/java/com/example/brain/memory/MemoryInterfaceImpl.kt", "w").write(content)
    print("Success")
else:
    print("Target not found")
