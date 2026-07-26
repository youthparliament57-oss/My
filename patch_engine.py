import sys

content = """package com.example.agent.automation

import javax.inject.Inject
import javax.inject.Singleton
import android.util.Log

@Singleton
class AutomationEngine @Inject constructor(
    private val accessibilityProvider: AccessibilityAutomationProvider,
    private val ladbProvider: LadbAutomationProvider,
    private val ladbConnection: LadbConnection,
    private val guard: com.example.agent.guard.BankingAppGuard
) : AutomationProvider {

    fun getProvider(): AutomationProvider {
        return if (ladbConnection.getState() == LadbConnection.State.READY) {
            ladbProvider
        } else {
            accessibilityProvider
        }
    }

    private fun isSafeToAutomate(): Boolean {
        return try {
            val snapshot = getProvider().getUiTree()
            !guard.isProtected(snapshot.packageName)
        } catch (e: Exception) {
            Log.e("AutomationEngine", "Failed to check safe automation", e)
            false
        }
    }

    override fun tap(x: Int, y: Int): Boolean {
        if (!isSafeToAutomate()) return false
        return getProvider().tap(x, y)
    }

    override fun type(text: String, nodeId: String?): Boolean {
        if (!isSafeToAutomate()) return false
        return getProvider().type(text, nodeId)
    }

    override fun swipe(startX: Int, startY: Int, endX: Int, endY: Int, duration: Long): Boolean {
        if (!isSafeToAutomate()) return false
        return getProvider().swipe(startX, startY, endX, endY, duration)
    }

    override fun scroll(direction: ScrollDirection): Boolean {
        if (!isSafeToAutomate()) return false
        return getProvider().scroll(direction)
    }

    override fun getUiTree(): UiTreeSnapshot {
        if (!isSafeToAutomate()) throw SecurityException("Cannot read UI tree of protected app")
        return getProvider().getUiTree()
    }

    // Backward compatibility aliases for Orchestrator
    fun executeTap(x: Int, y: Int): Boolean = tap(x, y)
    fun executeType(text: String, nodeId: String? = null): Boolean = type(text, nodeId)
    fun executeSwipe(startX: Int, startY: Int, endX: Int, endY: Int, duration: Long): Boolean = swipe(startX, startY, endX, endY, duration)
    fun executeScroll(direction: ScrollDirection): Boolean = scroll(direction)
}
"""
open("app/src/main/java/com/example/agent/automation/AutomationEngine.kt", "w").write(content)
print("Patched AutomationEngine.kt")
