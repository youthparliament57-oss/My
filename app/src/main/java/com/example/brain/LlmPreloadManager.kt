package com.example.brain

import android.content.ComponentCallbacks2
import android.content.Context
import android.content.res.Configuration
import android.util.Log
import androidx.work.*
import dagger.hilt.android.qualifiers.ApplicationContext
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class LlmPreloadManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val localLlmLayer: LocalLlmLayer
) : ComponentCallbacks2 {

    init {
        context.registerComponentCallbacks(this)
        Log.i("LlmPreloadManager", "LlmPreloadManager initialized. Dynamic low-memory callbacks registered.")
    }

    /**
     * Triggered programmatically on wake-word detection or chat screen transition to eliminate cold-start lag.
     */
    fun preloadModelOnIntent() {
        Log.i("LlmPreloadManager", "Preloading requested due to contextual trigger (wake word or chat opened).")
        localLlmLayer.preloadModel()
    }

    /**
     * Schedule a background preloading WorkManager job.
     * Triggers: Charging + Device Idle + Screen off (simulated or configured idle).
     */
    fun scheduleNightlyPreload() {
        val constraints = Constraints.Builder()
            .setRequiresCharging(true)
            .setRequiresDeviceIdle(true)
            .build()

        val workRequest = PeriodicWorkRequestBuilder<LlmPreloadWorker>(
            1, TimeUnit.DAYS
        )
            .setConstraints(constraints)
            .build()

        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            WORK_NAME,
            ExistingPeriodicWorkPolicy.KEEP,
            workRequest
        )
        Log.i("LlmPreloadManager", "Nightly predictive LLM preloading task successfully scheduled with WorkManager.")
    }

    // --- ComponentCallbacks2 Low-Memory Callback Implementation ---
    override fun onTrimMemory(level: Int) {
        if (level >= ComponentCallbacks2.TRIM_MEMORY_RUNNING_CRITICAL || 
            level >= ComponentCallbacks2.TRIM_MEMORY_COMPLETE) {
            Log.w("LlmPreloadManager", "CRITICAL SYSTEM MEMORY PRESSURE DETECTED (Level: $level). Unloading native LLM context instantly to prevent OOM termination.")
            localLlmLayer.releaseModel()
        }
    }

    override fun onConfigurationChanged(newConfig: Configuration) {}

    override fun onLowMemory() {
        Log.w("LlmPreloadManager", "Low Memory event captured. Safe-unloading native model.")
        localLlmLayer.releaseModel()
    }

    companion object {
        private const val WORK_NAME = "NousLlmNightlyPreload"
    }
}

/**
 * Worker to perform actual preloading in background when charging and idle.
 */
class LlmPreloadWorker(
    context: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(context, workerParams) {

    @dagger.hilt.EntryPoint
    @dagger.hilt.InstallIn(dagger.hilt.components.SingletonComponent::class)
    interface PreloadWorkerEntryPoint {
        fun localLlmLayer(): LocalLlmLayer
    }

    override suspend fun doWork(): Result {
        return try {
            val entryPoint = dagger.hilt.android.EntryPointAccessors.fromApplication(
                applicationContext,
                PreloadWorkerEntryPoint::class.java
            )
            val localLlm = entryPoint.localLlmLayer()
            localLlm.preloadModel()
            Result.success()
        } catch (e: Exception) {
            Log.e("LlmPreloadWorker", "Predictive preloading background execution failed: ${e.message}")
            Result.retry()
        }
    }
}
