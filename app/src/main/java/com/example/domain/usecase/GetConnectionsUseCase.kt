package com.example.domain.usecase

import com.example.domain.model.ThoughtConnection
import com.example.domain.repository.NousRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class GetConnectionsUseCase @Inject constructor(
    private val repository: NousRepository
) {
    operator fun invoke(): Flow<List<ThoughtConnection>> {
        return repository.getConnections()
    }
}
