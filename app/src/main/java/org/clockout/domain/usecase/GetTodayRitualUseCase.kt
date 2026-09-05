package org.clockout.domain.usecase

import org.clockout.data.repository.RitualRepository
import org.clockout.domain.model.RitualQuestion

class GetTodayRitualUseCase(
    private val repository: RitualRepository
) {
    operator fun invoke(): RitualQuestion {
        return repository.getTodayQuestion()
    }
}
