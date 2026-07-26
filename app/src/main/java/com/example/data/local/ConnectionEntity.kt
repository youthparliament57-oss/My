package com.example.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.example.domain.model.ThoughtConnection

@Entity(tableName = "connections")
data class ConnectionEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val sourceId: Long,
    val targetId: Long,
    val relationshipType: String
) {
    fun toDomain(): ThoughtConnection = ThoughtConnection(
        id = id,
        sourceId = sourceId,
        targetId = targetId,
        relationshipType = relationshipType
    )

    companion object {
        fun fromDomain(domain: ThoughtConnection): ConnectionEntity = ConnectionEntity(
            id = domain.id,
            sourceId = domain.sourceId,
            targetId = domain.targetId,
            relationshipType = domain.relationshipType
        )
    }
}
