package com.example.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.example.domain.model.Thought

@Entity(tableName = "thoughts")
data class ThoughtEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val content: String,
    val timestamp: Long,
    val xPosition: Float,
    val yPosition: Float,
    val importance: Int,
    val isAiGenerated: Boolean
) {
    fun toDomain(): Thought = Thought(
        id = id,
        title = title,
        content = content,
        timestamp = timestamp,
        xPosition = xPosition,
        yPosition = yPosition,
        importance = importance,
        isAiGenerated = isAiGenerated
    )

    companion object {
        fun fromDomain(domain: Thought): ThoughtEntity = ThoughtEntity(
            id = domain.id,
            title = domain.title,
            content = domain.content,
            timestamp = domain.timestamp,
            xPosition = domain.xPosition,
            yPosition = domain.yPosition,
            importance = domain.importance,
            isAiGenerated = domain.isAiGenerated
        )
    }
}
