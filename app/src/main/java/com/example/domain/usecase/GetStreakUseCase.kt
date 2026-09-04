package com.example.domain.usecase

import com.example.data.repository.RitualRepository
import com.example.domain.model.RitualStreakInfo
import kotlinx.coroutines.flow.Flow

class GetStreakUseCase(
    private val repository: RitualRepository
) {
    operator fun invoke(): Flow<RitualStreakInfo> {
        return repository.getStreakInfo()
    }
}
