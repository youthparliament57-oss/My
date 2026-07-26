package com.example.brain.memory

interface MemoryInterface {
    suspend fun storeEpisodicEvent(
        eventText: String,
        category: String,
        confidence: Float = 1.0f,
        latitude: Double? = null,
        longitude: Double? = null
    ): Long
    suspend fun storeSemanticFact(factText: String, category: String, confidence: Float = 1.0f): Long
    suspend fun storeProceduralPattern(patternText: String, frequency: Int, successRate: Float = 1.0f): Long
    suspend fun storeEmotionalAssociation(stimulus: String, emotion: String, intensity: Float, confidence: Float = 1.0f): Long
    
    suspend fun addGraphNode(id: String, type: String, name: String, properties: Map<String, String>, embedding: ByteArray)
    suspend fun addGraphEdge(id: String, sourceId: String, targetId: String, type: String, weight: Float)
    
    suspend fun recall(query: String): MemoryContext
    suspend fun consolidateMemories(localLlm: com.example.brain.LocalLlmLayer?)
    
    suspend fun clearOldMemories()
}
