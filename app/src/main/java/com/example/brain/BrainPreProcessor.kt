package com.example.brain

import android.content.Context
import java.text.Normalizer
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton
import com.example.persona.PersonaDefinitions
import com.example.persona.PersonaFacade

@Singleton
class BrainPreProcessor @Inject constructor(
    @dagger.hilt.android.qualifiers.ApplicationContext private val context: Context,
    private val personaFacade: PersonaFacade,
    private val healthManager: HealthManager
) {

    suspend fun preProcess(
        rawQuery: String,
        history: List<ConversationTurn> = emptyList(),
        memory: com.example.brain.memory.MemoryInterface? = null
    ): PreProcessResult {
        // 1. Unicode NFC Normalization, trim, and strip zero-width spaces
        val step1 = rawQuery.trim()
            .replace("\u200B", "")
            .replace("\u200C", "")
            .replace("\u200D", "")
            .replace("\uFEFF", "")
        
        val normalized = Normalizer.normalize(step1, Normalizer.Form.NFC)

        // 2. Hardcoded Banned safety checks (ConstitutionalGuardrails)
        val safetyCheck = ConstitutionalGuardrails.checkSafety(normalized)
        if (safetyCheck is ConstitutionalGuardrails.SafetyResult.Blocked) {
            return PreProcessResult.Blocked(safetyCheck.response)
        }

        // 3. Command Prefix Extraction
        var forcedLayer: String? = null
        var forcedPersona: String? = null
        var cleanQuery = normalized
        
        if (normalized.startsWith("!")) {
            val spaceIndex = normalized.indexOf(' ')
            val prefix = if (spaceIndex != -1) normalized.substring(0, spaceIndex) else normalized
            val command = prefix.substring(1).lowercase()
            
            // Check if it's a layer or a persona
            val allPersonas = PersonaDefinitions.ALL.map { it.name.lowercase() }
            if (allPersonas.contains(command)) {
                forcedPersona = command
            } else {
                forcedLayer = command
            }
            
            cleanQuery = if (spaceIndex != -1) normalized.substring(spaceIndex + 1).trim() else ""
        }

        // 4. Create Ambient State Snapshot
        val ambient = AmbientSnapshot(
            timestampMs = System.currentTimeMillis(),
            batteryLevel = getBatteryLevel(),
            isCharging = getIsCharging(),
            networkType = getNetworkType(),
            stepCount = healthManager.stepCount.value
        )

        // 5. Recall relevant memory context using Hybrid Recall Engine
        val recalledMemoryCtx = memory?.recall(cleanQuery) ?: com.example.brain.memory.MemoryContext()

        // 6. Build immutable Context
        val activePersona = if (forcedPersona != null) {
            PersonaDefinitions.ALL.find { it.name.equals(forcedPersona, ignoreCase = true) } ?: PersonaDefinitions.ATLAS
        } else if (forcedLayer == "nova") {
            PersonaDefinitions.NOVA 
        } else {
            PersonaDefinitions.ATLAS
        }

        val brainContext = BrainContext(
            userId = "default_user",
            activePersona = activePersona,
            ambientSnapshot = ambient,
            conversationHistory = history,
            activeIntents = emptyList(),
            budget = BrainBudget(),
            correlationId = UUID.randomUUID().toString(),
            memoryContext = recalledMemoryCtx
        )

        return PreProcessResult.Success(cleanQuery, forcedLayer, brainContext)
    }

    private fun getBatteryLevel(): Float {
        return try {
            val filter = android.content.IntentFilter(android.content.Intent.ACTION_BATTERY_CHANGED)
            val batteryStatus = context.registerReceiver(null, filter)
            val level = batteryStatus?.getIntExtra(android.os.BatteryManager.EXTRA_LEVEL, -1) ?: -1
            val scale = batteryStatus?.getIntExtra(android.os.BatteryManager.EXTRA_SCALE, -1) ?: -1
            level / scale.toFloat()
        } catch (e: Exception) {
            1.0f
        }
    }

    private fun getIsCharging(): Boolean {
        return try {
            val filter = android.content.IntentFilter(android.content.Intent.ACTION_BATTERY_CHANGED)
            val batteryStatus = context.registerReceiver(null, filter)
            val status = batteryStatus?.getIntExtra(android.os.BatteryManager.EXTRA_STATUS, -1) ?: -1
            status == android.os.BatteryManager.BATTERY_STATUS_CHARGING || status == android.os.BatteryManager.BATTERY_STATUS_FULL
        } catch (e: Exception) {
            false
        }
    }

    private fun getNetworkType(): String {
        return try {
            val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as android.net.ConnectivityManager
            val network = cm.activeNetwork ?: return "OFFLINE"
            val caps = cm.getNetworkCapabilities(network) ?: return "OFFLINE"
            when {
                caps.hasTransport(android.net.NetworkCapabilities.TRANSPORT_WIFI) -> "WIFI"
                caps.hasTransport(android.net.NetworkCapabilities.TRANSPORT_CELLULAR) -> "CELLULAR"
                caps.hasTransport(android.net.NetworkCapabilities.TRANSPORT_ETHERNET) -> "ETHERNET"
                else -> "OFFLINE"
            }
        } catch (e: Exception) {
            "WIFI"
        }
    }

    sealed class PreProcessResult {
        data class Success(
            val query: String,
            val forcedLayer: String?,
            val brainContext: BrainContext
        ) : PreProcessResult()
        
        data class Blocked(val safeResponse: String) : PreProcessResult()
    }
}
