package com.example.domain.usecase

import com.example.domain.model.ThoughtConnection
import com.example.domain.repository.NousRepository
import javax.inject.Inject

class AddConnectionUseCase @Inject constructor(
    private val repository: NousRepository
) {
    suspend operator fun invoke(sourceId: Long, targetId: Long, relationType: String = "associated") {
        if (sourceId == targetId) {
            throw IllegalArgumentException("A node cannot be connected to itself")
        }
        val connection = ThoughtConnection(
            sourceId = sourceId,
            targetId = targetId,
            relationshipType = relationType
        )
        repository.saveConnection(connection)
    }
}
