package com.example.domain.model

data class Thought(
    val id: Long = 0,
    val title: String,
    val content: String,
    val timestamp: Long = System.currentTimeMillis(),
    val xPosition: Float = 0.5f, // Relative position from 0.0 to 1.0
    val yPosition: Float = 0.5f, // Relative position from 0.0 to 1.0
    val importance: Int = 3, // 1 to 5 scale for node size representation
    val isAiGenerated: Boolean = false
)
