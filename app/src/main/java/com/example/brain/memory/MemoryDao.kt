package com.example.brain.memory

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface MemoryDao {

    // --- Episodic Events ---
    @Query("SELECT * FROM episodic_events ORDER BY timestamp DESC")
    fun getAllEpisodicEventsFlow(): Flow<List<EpisodicEventEntity>>

    @Query("SELECT * FROM episodic_events ORDER BY timestamp DESC")
    suspend fun getAllEpisodicEvents(): List<EpisodicEventEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertEpisodicEvent(event: EpisodicEventEntity): Long

    @Query("DELETE FROM episodic_events WHERE id = :id")
    suspend fun deleteEpisodicEventById(id: Long)

    @Query("DELETE FROM episodic_events WHERE timestamp < :expiryTime")
    suspend fun purgeOldEpisodicEvents(expiryTime: Long)

    @Query("""
        SELECT * FROM episodic_events 
        JOIN episodic_events_fts ON episodic_events.id = episodic_events_fts.docid 
        WHERE episodic_events_fts MATCH :query
    """)
    suspend fun searchEpisodicEvents(query: String): List<EpisodicEventEntity>


    // --- Semantic Facts ---
    @Query("SELECT * FROM semantic_facts ORDER BY timestamp DESC")
    fun getAllSemanticFactsFlow(): Flow<List<SemanticFactEntity>>

    @Query("SELECT * FROM semantic_facts ORDER BY timestamp DESC")
    suspend fun getAllSemanticFacts(): List<SemanticFactEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSemanticFact(fact: SemanticFactEntity): Long

    @Query("DELETE FROM semantic_facts WHERE id = :id")
    suspend fun deleteSemanticFactById(id: Long)

    @Query("""
        SELECT * FROM semantic_facts 
        JOIN semantic_facts_fts ON semantic_facts.id = semantic_facts_fts.docid 
        WHERE semantic_facts_fts MATCH :query
    """)
    suspend fun searchSemanticFacts(query: String): List<SemanticFactEntity>


    // --- Procedural Patterns ---
    @Query("SELECT * FROM procedural_patterns ORDER BY frequency DESC")
    fun getAllProceduralPatternsFlow(): Flow<List<ProceduralPatternEntity>>

    @Query("SELECT * FROM procedural_patterns ORDER BY frequency DESC")
    suspend fun getAllProceduralPatterns(): List<ProceduralPatternEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertProceduralPattern(pattern: ProceduralPatternEntity): Long


    // --- Emotional Associations ---
    @Query("SELECT * FROM emotional_associations ORDER BY timestamp DESC")
    fun getAllEmotionalAssociationsFlow(): Flow<List<EmotionalAssociationEntity>>

    @Query("SELECT * FROM emotional_associations ORDER BY timestamp DESC")
    suspend fun getAllEmotionalAssociations(): List<EmotionalAssociationEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertEmotionalAssociation(association: EmotionalAssociationEntity): Long

    @Query("DELETE FROM emotional_associations WHERE timestamp < :expiryTime")
    suspend fun purgeOldEmotionalAssociations(expiryTime: Long)


    // --- Knowledge Graph Nodes ---
    @Query("SELECT * FROM knowledge_graph_nodes")
    fun getAllGraphNodesFlow(): Flow<List<KnowledgeGraphNodeEntity>>

    @Query("SELECT * FROM knowledge_graph_nodes")
    suspend fun getAllGraphNodes(): List<KnowledgeGraphNodeEntity>

    @Query("SELECT * FROM knowledge_graph_nodes WHERE name LIKE '%' || :query || '%'")
    suspend fun searchGraphNodesByQuery(query: String): List<KnowledgeGraphNodeEntity>

    @Query("SELECT * FROM knowledge_graph_nodes WHERE id = :id")
    suspend fun getGraphNodeById(id: String): KnowledgeGraphNodeEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertGraphNode(node: KnowledgeGraphNodeEntity)

    @Query("DELETE FROM knowledge_graph_nodes WHERE id = :id")
    suspend fun deleteGraphNodeById(id: String)


    // --- Knowledge Graph Edges ---
    @Query("SELECT * FROM knowledge_graph_edges")
    fun getAllGraphEdgesFlow(): Flow<List<KnowledgeGraphEdgeEntity>>

    @Query("SELECT * FROM knowledge_graph_edges")
    suspend fun getAllGraphEdges(): List<KnowledgeGraphEdgeEntity>

    @Query("SELECT * FROM knowledge_graph_edges WHERE sourceId IN (:nodeIds) OR targetId IN (:nodeIds)")
    suspend fun getEdgesForNodes(nodeIds: List<String>): List<KnowledgeGraphEdgeEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertGraphEdge(edge: KnowledgeGraphEdgeEntity)

    @Query("DELETE FROM knowledge_graph_edges WHERE id = :id")
    suspend fun deleteGraphEdgeById(id: String)

    @Query("DELETE FROM knowledge_graph_edges WHERE sourceId = :nodeId OR targetId = :nodeId")
    suspend fun deleteEdgesForNode(nodeId: String)


    // --- Forgetting Curve States ---
    @Query("SELECT * FROM forgetting_curve_state WHERE memoryId = :memoryId")
    suspend fun getForgettingCurveState(memoryId: String): ForgettingCurveStateEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertForgettingCurveState(state: ForgettingCurveStateEntity)


    // --- Consolidation Logs ---
    @Query("SELECT * FROM consolidation_log ORDER BY timestamp DESC")
    fun getAllConsolidationLogsFlow(): Flow<List<ConsolidationLogEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertConsolidationLog(log: ConsolidationLogEntity): Long


    // --- User Preferences ---
    @Query("SELECT * FROM user_preferences WHERE `key` = :key")
    suspend fun getUserPreference(key: String): UserPreferenceEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertUserPreference(pref: UserPreferenceEntity)


    // --- Native Crash Logs ---
    @Query("SELECT * FROM native_crash_logs ORDER BY timestamp DESC")
    fun getAllNativeCrashLogsFlow(): Flow<List<NativeCrashLogEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertNativeCrashLog(log: NativeCrashLogEntity): Long

    // --- Reasoning Traces ---
    @Query("SELECT * FROM reasoning_traces ORDER BY timestamp DESC")
    fun getAllReasoningTracesFlow(): Flow<List<ReasoningTraceEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertReasoningTrace(trace: ReasoningTraceEntity): Long

    @Query("DELETE FROM reasoning_traces WHERE query = :query")
    suspend fun deleteReasoningTraceByQuery(query: String)

    @Query("UPDATE reasoning_traces SET userFeedback = :feedback WHERE query = :query")
    suspend fun updateReasoningTraceFeedback(query: String, feedback: String)
}
