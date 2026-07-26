package com.example.brain.memory

import android.content.Context
import androidx.work.*
import dagger.hilt.android.EntryPointAccessors
import java.util.concurrent.TimeUnit

class MemoryConsolidationWorker(
    context: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(context, workerParams) {

    @dagger.hilt.EntryPoint
    @dagger.hilt.InstallIn(dagger.hilt.components.SingletonComponent::class)
    interface MemoryWorkerEntryPoint {
        fun memoryInterface(): MemoryInterface
        fun localLlmLayer(): com.example.brain.LocalLlmLayer
    }

    override suspend fun doWork(): Result {
        return try {
            val entryPoint = EntryPointAccessors.fromApplication(
                applicationContext,
                MemoryWorkerEntryPoint::class.java
            )
            val memory = entryPoint.memoryInterface()
            val localLlm = entryPoint.localLlmLayer()
            
            // Perform consolidation (Dream Mode)
            memory.consolidateMemories(localLlm)
            
            // Purge old memories to preserve storage budget
            memory.clearOldMemories()
            
            Result.success()
        } catch (e: Exception) {
            Result.retry()
        }
    }

    companion object {
        private const val WORK_NAME = "NousMemoryConsolidationWork"

        fun scheduleDaily(context: Context) {
            val constraints = Constraints.Builder()
                .setRequiresCharging(true)
                .setRequiresDeviceIdle(true)
                .build()

            val workRequest = PeriodicWorkRequestBuilder<MemoryConsolidationWorker>(
                1, TimeUnit.DAYS
            )
                .setConstraints(constraints)
                .build()

            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                WORK_NAME,
                ExistingPeriodicWorkPolicy.KEEP,
                workRequest
            )
        }
    }
}
