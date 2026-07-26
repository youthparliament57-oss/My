package com.example.persona

import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withTimeoutOrNull
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class JitContextFetcher @Inject constructor(
    @ApplicationContext private val context: Context
) {
    // 300ms window fetcher
    suspend fun fetchContext(): String = coroutineScope {
        val batteryLevel = async {
            withTimeoutOrNull(200) { getBatteryLevel() } ?: "Unknown"
        }
        val mediaState = async {
            withTimeoutOrNull(200) { getMediaState() } ?: "Nothing playing"
        }
        val callLog = async {
            withTimeoutOrNull(200) { getRecentMissedCalls() } ?: "0 missed calls"
        }
        val usageStats = async {
            withTimeoutOrNull(200) { getMostUsedApps() } ?: "Unknown"
        }
        val interactionGap = async {
            withTimeoutOrNull(200) { getInteractionGap() } ?: "0 hours"
        }

        """
            Battery: ${batteryLevel.await()}
            Media: ${mediaState.await()}
            Missed Calls: ${callLog.await()}
            Top Apps: ${usageStats.await()}
            Time since last interaction: ${interactionGap.await()}
        """.trimIndent()
    }

    private fun getBatteryLevel(): String {
        val batteryStatus: Intent? = IntentFilter(Intent.ACTION_BATTERY_CHANGED).let { ifilter ->
            context.registerReceiver(null, ifilter)
        }
        val level: Int = batteryStatus?.getIntExtra(BatteryManager.EXTRA_LEVEL, -1) ?: -1
        val scale: Int = batteryStatus?.getIntExtra(BatteryManager.EXTRA_SCALE, -1) ?: -1
        val batteryPct = level * 100 / scale.toFloat()
        return "$batteryPct%"
    }

    private fun getMediaState(): String {
        // Stub for actual media state
        return "Not playing"
    }

    private fun getRecentMissedCalls(): String {
        // Stub for actual call log
        return "0 missed"
    }

    private fun getMostUsedApps(): String {
        // Stub for usage stats
        return "None"
    }

    private fun getInteractionGap(): String {
        return "1 hour"
    }
}

@Singleton
class GossipTriggerEvaluator @Inject constructor() {

    enum class GossipTrigger {
        BATTERY_HUNGRY, MUSIC_MOOD, USER_NEGLECT, SOCIAL_CHECK, NONE
    }

    fun evaluate(contextSummary: String): GossipTrigger {
        if (contextSummary.contains("Battery: ") && contextSummary.substringAfter("Battery: ").take(2).trim().toIntOrNull() ?: 100 < 20) {
            return GossipTrigger.BATTERY_HUNGRY
        }
        if (contextSummary.contains("Missed Calls: ") && !contextSummary.contains("0 missed")) {
            return GossipTrigger.SOCIAL_CHECK
        }
        if (contextSummary.contains("Time since last interaction: ") && contextSummary.substringAfter("Time since last interaction: ").take(1) == "2") { // simplified
            return GossipTrigger.USER_NEGLECT
        }
        if (!contextSummary.contains("Not playing")) {
            return GossipTrigger.MUSIC_MOOD
        }
        return GossipTrigger.NONE
    }
}
