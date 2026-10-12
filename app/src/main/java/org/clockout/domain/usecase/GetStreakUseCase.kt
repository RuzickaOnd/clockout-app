package org.clockout.domain.usecase

import org.clockout.domain.repository.RitualLogRepository
import org.clockout.domain.model.RitualStreakInfo
import kotlinx.coroutines.flow.Flow

class GetStreakUseCase(
    private val repository: RitualLogRepository
) {
    operator fun invoke(): Flow<RitualStreakInfo> {
        return repository.getStreakInfo()
    }
}
