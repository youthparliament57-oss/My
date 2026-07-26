package com.example.domain.model

data class ThoughtConnection(
    val id: Long = 0,
    val sourceId: Long,
    val targetId: Long,
    val relationshipType: String = "associated" // e.g. "supports", "challenges", "clarifies"
)
