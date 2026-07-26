import sys

content = open("app/src/main/java/com/example/brain/AgenticOrchestrator.kt").read()
target = """        override suspend fun execute(args: String, context: BrainContext): String {
            return try {
                val flowValue = repository.getThoughts().firstOrNull()
                val matchedThoughts = flowValue?.filter { t -> 
                     t.title.contains(args, ignoreCase = true) ||
                     t.content.contains(args, ignoreCase = true) 
                 } ?: emptyList()
                if (matchedThoughts.isEmpty()) {
                    "No matching memory nodes found for search keywords: '$args'."
                } else {
                    matchedThoughts.joinToString("\n") { t ->
                        "- Node [ID ${t.id}]: '${t.title}' -> ${t.content}"
                    }
                }
            } catch (e: Exception) {
                "Failed to access local memory indexes: ${e.message}"
            }
        }"""
replacement = """        override suspend fun execute(args: String, context: BrainContext): String {
            return try {
                val memoryContext = memoryInterface.recall(args)
                val results = mutableListOf<String>()
                results.addAll(memoryContext.relevantEpisodicEvents)
                results.addAll(memoryContext.relevantSemanticFacts)
                results.addAll(memoryContext.graphInsights)
                
                if (results.isEmpty()) {
                    "No matching memory nodes found for search keywords: '$args'."
                } else {
                    results.joinToString("\\n") { "- $it" }
                }
            } catch (e: Exception) {
                "Failed to access local memory indexes: ${e.message}"
            }
        }"""
if target in content:
    content = content.replace(target, replacement)
    open("app/src/main/java/com/example/brain/AgenticOrchestrator.kt", "w").write(content)
    print("Success")
else:
    print("Target not found")
