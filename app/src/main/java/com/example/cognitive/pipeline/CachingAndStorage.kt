package com.example.cognitive.pipeline

import com.example.cognitive.models.ReasoningTrace
import com.example.brain.memory.ReasoningTraceEntity
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

import java.security.MessageDigest
import java.util.concurrent.ConcurrentHashMap

@Singleton
class ReasoningCache @Inject constructor() {
    private val cache = ConcurrentHashMap<String, Pair<ReasoningTrace, Long>>()
    private val ttlMs = 24 * 60 * 60 * 1000L // 24 hours
    private val maxEntries = 500

    fun get(key: String): ReasoningTrace? {
        val entry = cache[key] ?: return null
        if (System.currentTimeMillis() - entry.second > ttlMs) {
            cache.remove(key)
            return null
        }
        return entry.first
    }

    fun put(key: String, trace: ReasoningTrace) {
        if (cache.size >= maxEntries) {
            cache.entries.minByOrNull { it.value.second }?.key?.let { cache.remove(it) }
        }
        cache[key] = trace to System.currentTimeMillis()
    }
    
    fun generateKey(query: String, userId: String, personaName: String): String {
        return try {
            val md = MessageDigest.getInstance("SHA-256")
            val input = "$query|$userId|$personaName"
            md.digest(input.toByteArray()).joinToString("") { "%02x".format(it) }
        } catch (e: Exception) {
            (query + userId + personaName).hashCode().toString()
        }
    }
}

@Singleton
class ReasoningTraceStore @Inject constructor(
    private val memoryDao: com.example.brain.memory.MemoryDao
) {
    val traces: kotlinx.coroutines.flow.Flow<List<ReasoningTrace>> = memoryDao.getAllReasoningTracesFlow().map { entities ->
        entities.map { it.toDomain() }
    }

    suspend fun storeTrace(trace: ReasoningTrace) {
        memoryDao.insertReasoningTrace(ReasoningTraceEntity.fromDomain(trace))
    }

    suspend fun deleteTrace(query: String) {
        memoryDao.deleteReasoningTraceByQuery(query)
    }
    
    suspend fun annotateFeedback(query: String, feedback: String) {
        memoryDao.updateReasoningTraceFeedback(query, feedback)
    }
}

// Mappers
fun ReasoningTraceEntity.toDomain() = ReasoningTrace(
    query = query,
    subTasks = subTasksJson,
    steps = stepsJson,
    finalAnswer = finalAnswer,
    confidenceScore = confidenceScore,
    timestamp = timestamp,
    userFeedback = userFeedback
)

fun ReasoningTraceEntity.Companion.fromDomain(trace: ReasoningTrace) = ReasoningTraceEntity(
    query = trace.query,
    subTasksJson = trace.subTasks,
    stepsJson = trace.steps,
    finalAnswer = trace.finalAnswer,
    confidenceScore = trace.confidenceScore,
    timestamp = trace.timestamp,
    userFeedback = trace.userFeedback
)

// Add Companion to ReasoningTraceEntity in MemoryModels.kt or just use a helper
