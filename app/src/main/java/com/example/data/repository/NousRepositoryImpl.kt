package com.example.data.repository

import com.example.BuildConfig
import com.example.data.local.ConnectionEntity
import com.example.data.local.NousDao
import com.example.data.local.ThoughtEntity
import com.example.data.remote.Content
import com.example.data.remote.GeminiApiService
import com.example.data.remote.GenerateContentRequest
import com.example.data.remote.Part
import com.example.domain.model.Thought
import com.example.domain.model.ThoughtConnection
import com.example.domain.repository.NousRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class NousRepositoryImpl @Inject constructor(
    private val nousDao: NousDao,
    private val geminiApiService: GeminiApiService
) : NousRepository {

    override fun getThoughts(): Flow<List<Thought>> {
        return nousDao.getThoughts().map { entities ->
            entities.map { it.toDomain() }
        }
    }

    override suspend fun getThoughtById(id: Long): Thought? {
        return nousDao.getThoughtById(id)?.toDomain()
    }

    override suspend fun saveThought(thought: Thought): Long {
        return nousDao.insertThought(ThoughtEntity.fromDomain(thought))
    }

    override suspend fun deleteThought(id: Long) {
        nousDao.deleteThought(id)
    }

    override fun getConnections(): Flow<List<ThoughtConnection>> {
        return nousDao.getConnections().map { entities ->
            entities.map { it.toDomain() }
        }
    }

    override suspend fun saveConnection(connection: ThoughtConnection) {
        nousDao.insertConnection(ConnectionEntity.fromDomain(connection))
    }

    override suspend fun deleteConnection(id: Long) {
        nousDao.deleteConnection(id)
    }

    override suspend fun deleteConnectionsForThought(thoughtId: Long) {
        nousDao.deleteConnectionsForThought(thoughtId)
    }

    override suspend fun askNousForInsight(prompt: String, contextThoughts: List<Thought>): String {
        val apiKey = BuildConfig.GEMINI_API_KEY
        if (apiKey.isBlank() || apiKey == "MY_GEMINI_API_KEY") {
            return "To active AI insights, please configure your GEMINI_API_KEY in the AI Studio Secrets panel. This will activate your real-time thinking companion."
        }

        val contextString = contextThoughts.joinToString(separator = "\n") { thought ->
            "- Title: ${thought.title}\n  Content: ${thought.content}"
        }

        val fullPrompt = if (contextThoughts.isNotEmpty()) {
            """
                Context from user's current mind space (NOUS nodes):
                $contextString
                
                Question / Directive:
                $prompt
                
                Provide a structured, insightful summary or expansion. Be helpful, concise, and intellectually engaging.
            """.trimIndent()
        } else {
            prompt
        }

        val request = GenerateContentRequest(
            contents = listOf(
                Content(
                    parts = listOf(
                        Part(text = "System Context: You are NOUS, an advanced AI mental-modeling assistant. You help users link ideas, find hidden connections, and expand concepts. Keep responses elegant, structured, and helpful.\n\n$fullPrompt")
                    )
                )
            )
        )

        return try {
            val response = geminiApiService.generateContent(apiKey, request)
            response.candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text
                ?: "I apologize, but I could not formulate a response at this time."
        } catch (e: Exception) {
            "An error occurred while connecting with NOUS: ${e.message}"
        }
    }
}
