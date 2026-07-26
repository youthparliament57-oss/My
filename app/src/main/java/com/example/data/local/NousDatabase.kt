package com.example.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import com.example.brain.memory.*

@Database(
    entities = [
        ThoughtEntity::class,
        ConnectionEntity::class,
        EpisodicEventEntity::class,
        SemanticFactEntity::class,
        SemanticFactFtsEntity::class,
        EpisodicEventFtsEntity::class,
        ProceduralPatternEntity::class,
        EmotionalAssociationEntity::class,
        KnowledgeGraphNodeEntity::class,
        KnowledgeGraphEdgeEntity::class,
        ForgettingCurveStateEntity::class,
        ConsolidationLogEntity::class,
        UserPreferenceEntity::class,
        NativeCrashLogEntity::class,
        ReasoningTraceEntity::class
    ],
    version = 4,
    exportSchema = false
)
@androidx.room.TypeConverters(RoomTypeConverters::class)
abstract class NousDatabase : RoomDatabase() {
    abstract fun nousDao(): NousDao
    abstract fun memoryDao(): MemoryDao
}
