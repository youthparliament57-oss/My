package com.example.agent.automation

import android.app.usage.UsageStatsManager
import android.content.Context
import android.util.Log
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AutomationEngine @Inject constructor(
    private val detector: AutomationMethodDetector,
    private val accessibilityProvider: AccessibilityAutomationProvider,
    private val ladbProvider: LadbAutomationProvider,
    private val ladbConnection: LadbConnection,
    private val guard: com.example.agent.guard.BankingAppGuard,
    @ApplicationContext private val context: Context
) : AutomationProvider {

    fun getProvider(): AutomationProvider {
        return when (detector.getBestMethod()) {
            AutomationMethodDetector.Method.LADB -> ladbProvider
            else -> accessibilityProvider
        }
    }

    private fun getForegroundPackage(): String? {
        try {
            val usageStatsManager = context.getSystemService(Context.USAGE_STATS_SERVICE) as UsageStatsManager
            val time = System.currentTimeMillis()
            val stats = usageStatsManager.queryUsageStats(UsageStatsManager.INTERVAL_DAILY, time - 1000 * 60, time)
            var currentApp: String? = null
            var lastTimeUsed = 0L
            for (stat in stats) {
                if (stat.lastTimeUsed > lastTimeUsed) {
                    lastTimeUsed = stat.lastTimeUsed
                    currentApp = stat.packageName
                }
            }
            return currentApp
        } catch (e: Exception) {
            return null
        }
    }

    private fun isSafeToAutomate(): Boolean {
        return try {
            val snapshot = getProvider().getUiTree()
            !guard.isProtected(snapshot.packageName)
        } catch (e: Exception) {
            val pkg = getForegroundPackage() ?: return false
            !guard.isProtected(pkg)
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

    override fun takeScreenshot(): Boolean {
        if (!isSafeToAutomate()) return false
        return getProvider().takeScreenshot()
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
