package com.example.brain.voice

import android.util.Log
import com.example.brain.HealthManager
import com.example.persona.Persona
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ProactiveManager @Inject constructor(
    private val healthManager: HealthManager,
    private val ttsEngine: TtsEngine,
    private val habitTracker: com.example.brain.memory.HabitTracker
) {
    private val scope = CoroutineScope(Dispatchers.Main + Job())
    private var lastStepCount = 0

    fun startMonitoring() {
        scope.launch {
            healthManager.stepCount.collect { steps ->
                if (steps > 0 && steps % 1000 == 0 && steps != lastStepCount) {
                    lastStepCount = steps
                    suggestHealthMilestone(steps)
                }
            }
        }
        
        // Behavioral / Temporal triggers (5.9)
        scope.launch {
            while (true) {
                delay(1000 * 60 * 30) // Every 30 minutes
                checkHabitTriggers()
            }
        }
    }

    private suspend fun checkHabitTriggers() {
        val nextActivity = habitTracker.getNextLikelyActivity()
        if (nextActivity != null) {
            Log.i("ProactiveManager", "Triggering proactive habit suggestion: $nextActivity")
            ttsEngine.speak(
                "I noticed you usually $nextActivity around this time. Shall I prepare the context?",
                com.example.persona.PersonaDefinitions.ATLAS,
                "CALM"
            )
        }
    }

    private fun suggestHealthMilestone(steps: Int) {
        Log.i("ProactiveManager", "Triggering health milestone proactive speech.")
        ttsEngine.speak(
            "Impressive. You've reached $steps steps. Your kinetic energy is optimizing. Keep moving.",
            com.example.persona.PersonaDefinitions.ATLAS,
            "HAPPY"
        )
    }
}
