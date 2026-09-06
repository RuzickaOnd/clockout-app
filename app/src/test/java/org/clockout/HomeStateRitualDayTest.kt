package org.clockout

import org.clockout.domain.model.RitualQuestion
import org.clockout.presentation.home.HomeState
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * A non-ritual day (a weekend while weekends are excluded) closes the ritual off entirely,
 * including the "anytime" test mode that otherwise bypasses the 16-21 window.
 */
class HomeStateRitualDayTest {

    private val question = RitualQuestion(
        id = "q1",
        textEn = "What went well today?",
        textCs = "Co se dnes povedlo?",
        suggestedAnswersEn = emptyList(),
        suggestedAnswersCs = emptyList()
    )

    private fun state(isRitualDay: Boolean, isTestModeEnabled: Boolean, isWithinWindow: Boolean) =
        HomeState(
            todayQuestion = question,
            isRitualDay = isRitualDay,
            isTestModeEnabled = isTestModeEnabled,
            isWithinWindow = isWithinWindow
        )

    @Test
    fun `test mode does not unlock the ritual on a non-ritual day`() {
        val state = state(isRitualDay = false, isTestModeEnabled = true, isWithinWindow = true)

        assertFalse(state.canStartRitual)
    }

    @Test
    fun `test mode unlocks the ritual outside the window on a ritual day`() {
        val state = state(isRitualDay = true, isTestModeEnabled = true, isWithinWindow = false)

        assertTrue(state.canStartRitual)
    }
}
