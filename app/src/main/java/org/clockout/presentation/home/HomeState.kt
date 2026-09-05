package org.clockout.presentation.home

import org.clockout.domain.model.AppLanguage
import org.clockout.domain.model.RitualQuestion
import org.clockout.domain.model.RitualStreakInfo

data class HomeState(
    val language: AppLanguage = AppLanguage.ENGLISH,
    val streakInfo: RitualStreakInfo? = null,
    val todayQuestion: RitualQuestion,
    val isWithinWindow: Boolean = false,
    val isTestModeEnabled: Boolean = false,
    val isLoading: Boolean = false
) {
    val canStartRitual: Boolean
        get() = isTestModeEnabled || isWithinWindow || (streakInfo?.isTodayCompleted != true)
}
