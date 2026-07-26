package com.example.domain.usecase

import com.example.domain.repository.NousRepository
import javax.inject.Inject

class DeleteThoughtUseCase @Inject constructor(
    private val repository: NousRepository
) {
    suspend operator fun invoke(thoughtId: Long) {
        repository.deleteConnectionsForThought(thoughtId)
        repository.deleteThought(thoughtId)
    }
}
