package com.example.presentation.home

import com.example.domain.model.AppLanguage
import com.example.domain.model.RitualQuestion
import com.example.domain.model.RitualStreakInfo

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
