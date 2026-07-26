package com.example.brain

interface BrainInterface {
    suspend fun processQuery(rawQuery: String, history: List<ConversationTurn> = emptyList()): BrainResponse
}
