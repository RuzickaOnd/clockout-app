package com.example.domain.usecase

import com.example.data.repository.RitualRepository
import com.example.domain.model.MoodTag

class LogMoodUseCase(
    private val repository: RitualRepository
) {
    suspend operator fun invoke(selectedAnswer: String?, mood: MoodTag, questionText: String) {
        repository.logRitual(selectedAnswer, mood, questionText)
    }
}
