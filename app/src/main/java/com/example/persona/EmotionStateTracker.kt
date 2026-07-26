package com.example.persona

import android.util.Log
import javax.inject.Inject
import javax.inject.Singleton

enum class Emotion {
    NEUTRAL, ANGER, SADNESS, JOY, FEAR
}

@Singleton
class CooldownRegistry @Inject constructor() {
    private var lastEmotionSwitchTime: Long = 0
    private val COOLDOWN_PERIOD_MS = 5 * 60 * 1000L // 5 minutes

    fun canSwitchEmotion(): Boolean {
        val now = System.currentTimeMillis()
        if (now - lastEmotionSwitchTime > COOLDOWN_PERIOD_MS) {
            lastEmotionSwitchTime = now
            return true
        }
        return false
    }
}

@Singleton
class EmotionStateTracker @Inject constructor(
    private val cooldownRegistry: CooldownRegistry
) {
    private var currentDominantEmotion: Emotion = Emotion.NEUTRAL
    private val emotionHistory = mutableListOf<Pair<Long, Emotion>>()

    fun updateUserEmotion(emotion: Emotion) {
        val now = System.currentTimeMillis()
        emotionHistory.add(Pair(now, emotion))
        
        // Decay older emotions - only keep last 10 minutes
        emotionHistory.removeAll { now - it.first > 10 * 60 * 1000L }

        if (emotion != currentDominantEmotion) {
            if (cooldownRegistry.canSwitchEmotion()) {
                currentDominantEmotion = emotion
                Log.d("EmotionStateTracker", "Switched to new dominant emotion: $emotion")
            } else {
                Log.d("EmotionStateTracker", "Emotion switch blocked by cooldown. Keeping $currentDominantEmotion")
            }
        }
    }

    fun getCurrentEmotion(): Emotion = currentDominantEmotion
}
