package org.clockout.domain.usecase

import org.clockout.data.repository.RitualRepository
import org.clockout.domain.model.MoodTag

class LogMoodUseCase(
    private val repository: RitualRepository
) {
    suspend operator fun invoke(selectedAnswer: String?, mood: MoodTag, questionText: String) {
        repository.logRitual(selectedAnswer, mood, questionText)
    }
}
