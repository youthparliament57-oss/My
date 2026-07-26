package com.example.brain.memory

import android.content.Context
import android.util.Log
import com.example.brain.LocalLlmLayer
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update
import org.json.JSONObject
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MemoryInterfaceImpl @Inject constructor(
    private val memoryDao: MemoryDao,
    private val omniSlm: com.example.vision.OmniSlmRuntime
) : MemoryInterface {

    private val workingMemory = MutableStateFlow<List<String>>(emptyList())

    override suspend fun storeEpisodicEvent(
        eventText: String,
        category: String,
        confidence: Float,
        latitude: Double?,
        longitude: Double?
    ): Long {
        val entity = EpisodicEventEntity(
            eventText = eventText,
            timestamp = System.currentTimeMillis(),
            category = category,
            confidence = confidence,
            latitude = latitude,
            longitude = longitude
        )
        val id = memoryDao.insertEpisodicEvent(entity)
        
        val curveState = ForgettingCurveStateEntity(
            memoryId = "episodic_$id",
            lastRecalledMs = System.currentTimeMillis(),
            halfLifeDays = 30f,
            recallCount = 1
        )
        memoryDao.insertForgettingCurveState(curveState)

        workingMemory.update { list -> (list + eventText).takeLast(10) }
        return id
    }

    override suspend fun storeSemanticFact(factText: String, category: String, confidence: Float): Long {
        val entity = SemanticFactEntity(
            factText = factText,
            timestamp = System.currentTimeMillis(),
            category = category,
            confidence = confidence
        )
        val id = memoryDao.insertSemanticFact(entity)

        val curveState = ForgettingCurveStateEntity(
            memoryId = "semantic_$id",
            lastRecalledMs = System.currentTimeMillis(),
            halfLifeDays = 365f,
            recallCount = 1
        )
        memoryDao.insertForgettingCurveState(curveState)
        return id
    }

    override suspend fun storeProceduralPattern(patternText: String, frequency: Int, successRate: Float): Long {
        val entity = ProceduralPatternEntity(
            patternText = patternText,
            frequency = frequency,
            successRate = successRate,
            lastTriggeredMs = System.currentTimeMillis()
        )
        return memoryDao.insertProceduralPattern(entity)
    }

    override suspend fun storeEmotionalAssociation(
        stimulus: String,
        emotion: String,
        intensity: Float,
        confidence: Float
    ): Long {
        val entity = EmotionalAssociationEntity(
            stimulus = stimulus,
            primaryEmotion = emotion,
            intensity = intensity,
            timestamp = System.currentTimeMillis(),
            confidence = confidence
        )
        val id = memoryDao.insertEmotionalAssociation(entity)

        val curveState = ForgettingCurveStateEntity(
            memoryId = "emotional_$id",
            lastRecalledMs = System.currentTimeMillis(),
            halfLifeDays = 90f,
            recallCount = 1
        )
        memoryDao.insertForgettingCurveState(curveState)
        return id
    }

    override suspend fun addGraphNode(
        id: String,
        type: String,
        name: String,
        properties: Map<String, String>,
        embedding: ByteArray
    ) {
        val propertiesJson = JSONObject(properties).toString()
        val quantizedJson = VectorMath.byteVectorToJson(embedding)

        val entity = KnowledgeGraphNodeEntity(
            id = id,
            type = type,
            name = name,
            propertiesJson = propertiesJson,
            quantizedEmbeddingJson = quantizedJson
        )
        memoryDao.insertGraphNode(entity)
    }

    override suspend fun addGraphEdge(
        id: String,
        sourceId: String,
        targetId: String,
        type: String,
        weight: Float
    ) {
        val entity = KnowledgeGraphEdgeEntity(
            id = id,
            sourceId = sourceId,
            targetId = targetId,
            type = type,
            weight = weight,
            startMs = System.currentTimeMillis(),
            endMs = 0L
        )
        memoryDao.insertGraphEdge(entity)
    }

    override suspend fun recall(query: String): MemoryContext {
        val startTime = System.currentTimeMillis()
        val queryEmbedding = omniSlm.generateEmbedding(query)
        
        // 1. Keyword Search via FTS5
        val ftsEpisodic = memoryDao.searchEpisodicEvents(query)
        val ftsSemantic = memoryDao.searchSemanticFacts(query)
        
        val keywordMatches = (ftsEpisodic.map { it.eventText } + ftsSemantic.map { it.factText }).distinct()

        // 2. Semantic Similarity Search
        val matchedNodes = memoryDao.searchGraphNodesByQuery(query)
        val semanticMatches = matchedNodes.map { node ->
            val nodeVector = VectorMath.jsonToByteVector(node.quantizedEmbeddingJson)
            val similarity = VectorMath.cosineSimilarity(queryEmbedding, nodeVector)
            node.name to similarity
        }.filter { it.second > 0.4f }
         .sortedByDescending { it.second }
         .map { it.first }

        // 3. Knowledge Graph Traversal
        val graphInsights = mutableListOf<String>()
        val nodeIds = matchedNodes.map { it.id }
        val allEdges = if (nodeIds.isNotEmpty()) memoryDao.getEdgesForNodes(nodeIds) else emptyList()
        for (node in matchedNodes) {
            val properties = try { JSONObject(node.propertiesJson) } catch (e: Exception) { JSONObject() }
            val edges = allEdges.filter { it.sourceId == node.id || it.targetId == node.id }
            for (edge in edges) {
                val targetId = if (edge.sourceId == node.id) edge.targetId else edge.sourceId
                val targetNode = memoryDao.getGraphNodeById(targetId)
                if (targetNode != null) {
                    graphInsights.add("Relation: ${node.name} --(${edge.type})--> ${targetNode.name}")
                }
            }
            if (properties.length() > 0) graphInsights.add("Properties of ${node.name}: $properties")
        }
        // 4. RRF Fusion
        val fusedResults = reciprocalRankFusion(listOf(keywordMatches, semanticMatches)).take(5)

        val activeEmotion = memoryDao.getAllEmotionalAssociations()
            .filter { applyForgettingCurveDecay("emotional_${it.id}", it.confidence) > 0.2f }
            .maxByOrNull { it.intensity }?.primaryEmotion ?: "CALM"

        Log.i("MemoryInterfaceImpl", "Recall in ${System.currentTimeMillis() - startTime}ms")

        return MemoryContext(
            relevantEpisodicEvents = fusedResults,
            relevantSemanticFacts = ftsSemantic.take(3).map { it.factText },
            activePatterns = memoryDao.getAllProceduralPatterns().take(2).map { it.patternText },
            emotionalState = activeEmotion,
            graphInsights = graphInsights
        )
    }

    private fun reciprocalRankFusion(lists: List<List<String>>, k: Int = 60): List<String> {
        val scores = mutableMapOf<String, Float>()
        for (list in lists) {
            list.forEachIndexed { index, item ->
                scores[item] = (scores[item] ?: 0f) + 1.0f / (k + index + 1)
            }
        }
        return scores.toList().sortedByDescending { it.second }.map { it.first }
    }

    private suspend fun applyForgettingCurveDecay(memoryId: String, initialConfidence: Float): Float {
        val state = memoryDao.getForgettingCurveState(memoryId) ?: return initialConfidence
        val currentMs = System.currentTimeMillis()
        
        // Decay the confidence based on time passed
        val decayed = EbbinghausForgettingCurve.calculateDecayedConfidence(
            initialConfidence = initialConfidence,
            lastRecalledMs = state.lastRecalledMs,
            currentMs = currentMs,
            halfLifeDays = state.halfLifeDays
        )

        // Apply 7-day half decay logic
        return EbbinghausForgettingCurve.checkAndApplySevenDayDecay(decayed, state.lastRecalledMs, currentMs)
    }

    override suspend fun consolidateMemories(localLlm: LocalLlmLayer?) {
        val startTime = System.currentTimeMillis()
        Log.i("MemoryInterfaceImpl", "Starting Dream Mode Memory Consolidation process...")

        val rawEpisodic = memoryDao.getAllEpisodicEvents()
        val dayAgo = System.currentTimeMillis() - (24 * 60 * 60 * 1000)
        val recentEpisodic = rawEpisodic.filter { it.timestamp >= dayAgo }

        var processedCount = 0
        val extractedFacts = mutableListOf<String>()

        for (event in recentEpisodic) {
            val text = event.eventText
            if (localLlm != null) {
                try {
                    val prompt = "Extract core semantic facts from this episodic memory. Return ONLY the extracted facts (no preamble). Memory: '$text'"
                    val dummyContext = com.example.brain.BrainContext() // Minimal context
                    val extracted = localLlm.processLocalQuery(prompt, dummyContext)
                    if (extracted.isNotBlank() && !extracted.startsWith("Error")) {
                        extractedFacts.add(extracted)
                        storeSemanticFact(
                            factText = extracted,
                            category = "Extracted",
                            confidence = 0.8f
                        )
                        processedCount++
                    }
                } catch (e: Exception) {
                    android.util.Log.e("MemoryInterface", "LLM fact extraction failed", e)
                }
            } else {
                if (text.contains("prefer", ignoreCase = true) || text.contains("like", ignoreCase = true) || text.contains("always", ignoreCase = true)) {
                    extractedFacts.add("Extracted Fact from event: $text")
                    storeSemanticFact(
                        factText = "User preferences/habits noticed: $text",
                        category = "Preference",
                        confidence = 0.8f
                    )
                    processedCount++
                }
            }
        }

        val allEpisodic = memoryDao.getAllEpisodicEvents()
        for (event in allEpisodic) {
            val memoryId = "episodic_${event.id}"
            val curve = memoryDao.getForgettingCurveState(memoryId)
            if (curve != null && curve.recallCount > 1) {
                val newHalfLife = EbbinghausForgettingCurve.strengthenHalfLife(curve.halfLifeDays, curve.recallCount)
                memoryDao.insertForgettingCurveState(
                    curve.copy(halfLifeDays = newHalfLife, lastRecalledMs = System.currentTimeMillis())
                )
            }
        }

        val duration = System.currentTimeMillis() - startTime
        val summaryText = "Consolidated ${recentEpisodic.size} episodic events. Extracted ${extractedFacts.size} facts."
        
        memoryDao.insertConsolidationLog(
            ConsolidationLogEntity(
                timestamp = System.currentTimeMillis(),
                durationMs = duration,
                processedCount = processedCount,
                status = "SUCCESS",
                summary = summaryText
            )
        )
        
        Log.i("MemoryInterfaceImpl", "Dream Mode Consolidation complete! Duration: ${duration}ms. Summary: $summaryText")
    }

    // 4. Privacy Pruning
    override suspend fun clearOldMemories() {
        val thirtyDaysAgo = System.currentTimeMillis() - (30L * 24 * 60 * 60 * 1000)
        val ninetyDaysAgo = System.currentTimeMillis() - (90L * 24 * 60 * 60 * 1000)

        // Purge expired episodic records (> 30 days) and emotional associations (> 90 days)
        memoryDao.purgeOldEpisodicEvents(thirtyDaysAgo)
        memoryDao.purgeOldEmotionalAssociations(ninetyDaysAgo)
        
        Log.i("MemoryInterfaceImpl", "Privacy purge complete. Older episodic and emotional events safely removed.")
    }
}
