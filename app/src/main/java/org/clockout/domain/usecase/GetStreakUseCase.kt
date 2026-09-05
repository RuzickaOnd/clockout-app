package org.clockout.domain.usecase

import org.clockout.data.repository.RitualRepository
import org.clockout.domain.model.RitualStreakInfo
import kotlinx.coroutines.flow.Flow

class GetStreakUseCase(
    private val repository: RitualRepository
) {
    operator fun invoke(): Flow<RitualStreakInfo> {
        return repository.getStreakInfo()
    }
}
