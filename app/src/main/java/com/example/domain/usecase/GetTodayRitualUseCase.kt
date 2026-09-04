package com.example.domain.usecase

import com.example.data.repository.RitualRepository
import com.example.domain.model.RitualQuestion

class GetTodayRitualUseCase(
    private val repository: RitualRepository
) {
    operator fun invoke(): RitualQuestion {
        return repository.getTodayQuestion()
    }
}
