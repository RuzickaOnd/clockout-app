package org.clockout.domain.usecase

import org.clockout.domain.repository.RitualLogRepository
import org.clockout.domain.model.RitualQuestion

class GetTodayRitualUseCase(
    private val repository: RitualLogRepository
) {
    operator fun invoke(): RitualQuestion {
        return repository.getTodayQuestion()
    }
}
