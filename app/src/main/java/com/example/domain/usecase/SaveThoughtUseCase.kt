package com.example.domain.usecase

import com.example.domain.model.Thought
import com.example.domain.repository.NousRepository
import javax.inject.Inject

class SaveThoughtUseCase @Inject constructor(
    private val repository: NousRepository
) {
    suspend operator fun invoke(thought: Thought): Long {
        if (thought.title.isBlank()) {
            throw IllegalArgumentException("Thought title cannot be empty")
        }
        return repository.saveThought(thought)
    }
}
