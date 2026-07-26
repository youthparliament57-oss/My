package com.example

import android.app.Application
import com.example.brain.LlmPreloadManager
import com.example.brain.memory.MemoryConsolidationWorker
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject

@HiltAndroidApp
class NousApplication : Application() {
    @Inject lateinit var llmPreloadManager: LlmPreloadManager
    @Inject lateinit var agentFacade: com.example.agent.AgentFacade

    companion object {
        lateinit var instance: NousApplication
            private set
    }

    override fun onCreate() {
        super.onCreate()
        instance = this
        try {
            MemoryConsolidationWorker.scheduleDaily(this)
            llmPreloadManager.scheduleNightlyPreload()
        } catch (e: Exception) {
            // Safe fallback if WorkManager initialization is deferred
        }
    }
}
