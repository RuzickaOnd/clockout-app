package org.clockout

import org.clockout.domain.model.MoodTag
import org.clockout.domain.model.ReviewPromptPolicy
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ReviewPromptPolicyTest {

    @Test
    fun `stays quiet before the tenth ritual even in a good mood`() {
        for (completed in 0 until ReviewPromptPolicy.MIN_COMPLETED_RITUALS) {
            assertFalse(
                "ritual $completed should not prompt",
                ReviewPromptPolicy.shouldPrompt(completed, MoodTag.SATISFIED, alreadyPrompted = false)
            )
        }
    }

    @Test
    fun `prompts on the tenth ritual when the mood is good`() {
        assertTrue(ReviewPromptPolicy.shouldPrompt(10, MoodTag.SATISFIED, alreadyPrompted = false))
        assertTrue(ReviewPromptPolicy.shouldPrompt(10, MoodTag.CALM, alreadyPrompted = false))
    }

    @Test
    fun `stays quiet on a bad mood no matter how many rituals`() {
        assertFalse(ReviewPromptPolicy.shouldPrompt(10, MoodTag.TIRED, alreadyPrompted = false))
        assertFalse(ReviewPromptPolicy.shouldPrompt(99, MoodTag.TENSE, alreadyPrompted = false))
    }

    /** A bad tenth day only postpones the question; it must not disqualify the user for good. */
    @Test
    fun `catches the first good mood after a bad tenth ritual`() {
        assertFalse(ReviewPromptPolicy.shouldPrompt(10, MoodTag.TENSE, alreadyPrompted = false))
        assertFalse(ReviewPromptPolicy.shouldPrompt(11, MoodTag.TIRED, alreadyPrompted = false))
        assertTrue(ReviewPromptPolicy.shouldPrompt(12, MoodTag.CALM, alreadyPrompted = false))
    }

    @Test
    fun `never asks twice`() {
        assertFalse(ReviewPromptPolicy.shouldPrompt(42, MoodTag.SATISFIED, alreadyPrompted = true))
    }
}
