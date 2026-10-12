package org.clockout.domain.usecase

import org.clockout.domain.repository.RitualLogRepository
import org.clockout.domain.model.MoodTag
import org.clockout.domain.model.ReviewPromptPolicy

/**
 * Asks [ReviewPromptPolicy] whether the ritual that has just been logged is the right moment to
 * request a Play Store review, and writes down that the question was asked so it never repeats.
 */
class ShouldRequestReviewUseCase(
    private val repository: RitualLogRepository
) {
    suspend operator fun invoke(mood: MoodTag): Boolean {
        val shouldPrompt = ReviewPromptPolicy.shouldPrompt(
            completedRituals = repository.getCompletedRitualCount(),
            mood = mood,
            alreadyPrompted = repository.isReviewPromptShown()
        )
        if (shouldPrompt) repository.markReviewPromptShown()
        return shouldPrompt
    }
}
