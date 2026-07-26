package com.example.brain.memory


import androidx.room.Entity
import androidx.room.Fts4
import androidx.room.PrimaryKey

@Entity(tableName = "episodic_events")
data class EpisodicEventEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val eventText: String,
    val timestamp: Long,
    val category: String,
    val confidence: Float,
    val latitude: Double? = null,
    val longitude: Double? = null
)

@Fts4(contentEntity = EpisodicEventEntity::class)
@Entity(tableName = "episodic_events_fts")
data class EpisodicEventFtsEntity(
    val eventText: String
)

@Entity(tableName = "semantic_facts")
data class SemanticFactEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val factText: String,
    val timestamp: Long,
    val category: String,
    val confidence: Float
)

@Fts4(contentEntity = SemanticFactEntity::class)
@Entity(tableName = "semantic_facts_fts")
data class SemanticFactFtsEntity(
    val factText: String
)

@Entity(tableName = "procedural_patterns")
data class ProceduralPatternEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val patternText: String,
    val frequency: Int,
    val successRate: Float,
    val lastTriggeredMs: Long
)

@Entity(tableName = "emotional_associations")
data class EmotionalAssociationEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val stimulus: String,
    val primaryEmotion: String,
    val intensity: Float,
    val timestamp: Long,
    val confidence: Float
)

@Entity(tableName = "knowledge_graph_nodes")
data class KnowledgeGraphNodeEntity(
    @PrimaryKey val id: String,
    val type: String, // Person, Place, App, Concept, Event, Object
    val name: String,
    val propertiesJson: String,
    val quantizedEmbeddingJson: String // INT8 vector representation [127, -50, ...]
)

@Entity(tableName = "knowledge_graph_edges")
data class KnowledgeGraphEdgeEntity(
    @PrimaryKey val id: String,
    val sourceId: String,
    val targetId: String,
    val type: String, // CALLS, KNOWS, VISITED, USES, LIKES, DISLIKES, OWNS, WORKS_AT
    val weight: Float, // Confidence / edge weight 0.0-1.0
    val startMs: Long,
    val endMs: Long
)

@Entity(tableName = "forgetting_curve_state")
data class ForgettingCurveStateEntity(
    @PrimaryKey val memoryId: String, // "episodic_123" or "semantic_456"
    val lastRecalledMs: Long,
    val halfLifeDays: Float,
    val recallCount: Int
)

@Entity(tableName = "consolidation_log")
data class ConsolidationLogEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val timestamp: Long,
    val durationMs: Long,
    val processedCount: Int,
    val status: String,
    val summary: String
)

@Entity(tableName = "user_preferences")
data class UserPreferenceEntity(
    @PrimaryKey val key: String,
    val encryptedValue: String,
    val timestamp: Long
)

@Entity(tableName = "native_crash_logs")
data class NativeCrashLogEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val timestamp: Long,
    val signalType: String,
    val stackTrace: String,
    val modelName: String,
    val vramUsage: Long,
    val tps: Float?
)

@Entity(tableName = "reasoning_traces")
data class ReasoningTraceEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val query: String,
    val subTasksJson: List<String>,
    val stepsJson: List<com.example.cognitive.models.ReasoningStep>,
    val finalAnswer: String?,
    val confidenceScore: Float,
    val timestamp: Long,
    val userFeedback: String?
) {
    companion object
}

data class MemoryContext(
    val relevantEpisodicEvents: List<String> = emptyList(),
    val relevantSemanticFacts: List<String> = emptyList(),
    val activePatterns: List<String> = emptyList(),
    val emotionalState: String = "CALM",
    val graphInsights: List<String> = emptyList()
)
