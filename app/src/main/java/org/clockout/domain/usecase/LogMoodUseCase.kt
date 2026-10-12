package org.clockout.domain.usecase

import org.clockout.domain.repository.RitualLogRepository
import org.clockout.domain.model.MoodTag

class LogMoodUseCase(
    private val repository: RitualLogRepository
) {
    suspend operator fun invoke(selectedAnswer: String?, mood: MoodTag, questionText: String) {
        repository.logRitual(selectedAnswer, mood, questionText)
    }
}
