package com.example.cognitive.pipeline

import com.example.domain.model.Thought
import com.example.domain.model.ThoughtConnection
import com.example.domain.repository.NousRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf

class FakeNousRepository : NousRepository {
    override fun getThoughts(): Flow<List<Thought>> = flowOf(emptyList())
    override suspend fun getThoughtById(id: Long): Thought? = null
    override suspend fun saveThought(thought: Thought): Long = 1L
    override suspend fun deleteThought(id: Long) {}

    override fun getConnections(): Flow<List<ThoughtConnection>> = flowOf(emptyList())
    override suspend fun saveConnection(connection: ThoughtConnection) {}
    override suspend fun deleteConnection(id: Long) {}
    override suspend fun deleteConnectionsForThought(thoughtId: Long) {}

    override suspend fun askNousForInsight(prompt: String, contextThoughts: List<Thought>): String {
        return "Fake insight"
    }
}
