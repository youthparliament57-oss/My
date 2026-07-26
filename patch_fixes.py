import sys

# Fix LadbStack.kt
ladb = open("app/src/main/java/com/example/agent/automation/LadbStack.kt").read()
ladb = ladb.replace("foregroundPackage = pkg.trim(),", "packageName = pkg.trim(),")
ladb = ladb.replace("nodes = emptyList(),", "rootNode = NodeInfo(null, null, null, null, \"\", false, emptyList()),")
open("app/src/main/java/com/example/agent/automation/LadbStack.kt", "w").write(ladb)

# Fix AutomationEngine.kt
engine = open("app/src/main/java/com/example/agent/automation/AutomationEngine.kt").read()
aliases = """
    // Backward compatibility aliases for Orchestrator
    fun executeTap(x: Int, y: Int): Boolean = tap(x, y)
    fun executeType(text: String, nodeId: String? = null): Boolean = type(text, nodeId)
    fun executeSwipe(startX: Int, startY: Int, endX: Int, endY: Int, duration: Long): Boolean = swipe(startX, startY, endX, endY, duration)
    fun executeScroll(direction: ScrollDirection): Boolean = scroll(direction)
}
"""
engine = engine.replace("}\n", aliases)
open("app/src/main/java/com/example/agent/automation/AutomationEngine.kt", "w").write(engine)
print("Patched LadbStack and AutomationEngine")
