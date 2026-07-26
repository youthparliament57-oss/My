package com.example.domain.repository

import com.example.domain.model.Thought
import com.example.domain.model.ThoughtConnection
import kotlinx.coroutines.flow.Flow

interface NousRepository {
    fun getThoughts(): Flow<List<Thought>>
    suspend fun getThoughtById(id: Long): Thought?
    suspend fun saveThought(thought: Thought): Long
    suspend fun deleteThought(id: Long)
    
    fun getConnections(): Flow<List<ThoughtConnection>>
    suspend fun saveConnection(connection: ThoughtConnection)
    suspend fun deleteConnection(id: Long)
    suspend fun deleteConnectionsForThought(thoughtId: Long)
    
    suspend fun askNousForInsight(prompt: String, contextThoughts: List<Thought>): String
}
