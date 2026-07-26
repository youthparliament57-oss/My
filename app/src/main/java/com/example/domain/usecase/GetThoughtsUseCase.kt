package com.example.domain.usecase

import com.example.domain.model.Thought
import com.example.domain.repository.NousRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class GetThoughtsUseCase @Inject constructor(
    private val repository: NousRepository
) {
    operator fun invoke(): Flow<List<Thought>> {
        return repository.getThoughts()
    }
}
