package org.clockout.presentation.home

import org.clockout.domain.model.AppLanguage
import org.clockout.domain.model.PathProgress
import org.clockout.domain.model.RitualQuestion
import org.clockout.domain.model.RitualStreakInfo

data class HomeState(
    val language: AppLanguage = AppLanguage.ENGLISH,
    val streakInfo: RitualStreakInfo? = null,
    val pathProgress: PathProgress? = null,
    val todayQuestion: RitualQuestion,
    val isWithinWindow: Boolean = false,
    /** Mirrors the DataStore default, so the frame before preferences emit does not close the ritual off. */
    val isTestModeEnabled: Boolean = true,
    val isRitualDay: Boolean = true,
    val isLoading: Boolean = false
) {
    /**
     * A weekend excluded by the settings closes the ritual off even with the anytime setting on,
     * and with that setting off the 16-21 window is what decides.
     */
    val canStartRitual: Boolean
        get() = isRitualDay && (isTestModeEnabled || isWithinWindow)
}
