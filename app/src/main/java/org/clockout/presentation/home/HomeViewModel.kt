package org.clockout.presentation.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import org.clockout.data.repository.RitualRepository
import org.clockout.domain.model.AppLanguage
import org.clockout.domain.model.RitualDayPolicy
import org.clockout.domain.model.RitualStreakInfo
import org.clockout.domain.usecase.GetStreakUseCase
import org.clockout.domain.usecase.GetTodayRitualUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class HomeViewModel(
    private val ritualRepository: RitualRepository,
    private val getTodayRitualUseCase: GetTodayRitualUseCase,
    private val getStreakUseCase: GetStreakUseCase
) : ViewModel() {

    /** Last value seen from settings, so a plain refresh can re-evaluate today's rest day. */
    private var includeWeekends = false

    private val _state = MutableStateFlow(
        HomeState(
            todayQuestion = getTodayRitualUseCase(),
            isWithinWindow = ritualRepository.isWithinWorkdayCloseWindow(),
            isRitualDay = RitualDayPolicy.isTodayRitualDay(includeWeekends)
        )
    )
    val state: StateFlow<HomeState> = _state.asStateFlow()

    init {
        observeData()
    }

    private fun observeData() {
        viewModelScope.launch {
            combine(
                getStreakUseCase(),
                ritualRepository.getTestMode(),
                ritualRepository.getLanguage(),
                ritualRepository.getIncludeWeekends()
            ) { streakInfo, testMode, language, includeWeekends ->
                HomeData(streakInfo, testMode, language, includeWeekends)
            }.collect { data ->
                includeWeekends = data.includeWeekends
                _state.update {
                    it.copy(
                        streakInfo = data.streakInfo,
                        isTestModeEnabled = data.testMode,
                        language = data.language,
                        isWithinWindow = ritualRepository.isWithinWorkdayCloseWindow(),
                        isRitualDay = RitualDayPolicy.isTodayRitualDay(data.includeWeekends)
                    )
                }
            }
        }
    }

    fun onIntent(intent: HomeIntent) {
        when (intent) {
            is HomeIntent.Refresh -> {
                _state.update {
                    it.copy(
                        isWithinWindow = ritualRepository.isWithinWorkdayCloseWindow(),
                        isRitualDay = RitualDayPolicy.isTodayRitualDay(includeWeekends)
                    )
                }
            }
            is HomeIntent.ToggleTestMode -> {
                viewModelScope.launch {
                    ritualRepository.setTestMode(intent.enabled)
                }
            }
        }
    }

    private data class HomeData(
        val streakInfo: RitualStreakInfo,
        val testMode: Boolean,
        val language: AppLanguage,
        val includeWeekends: Boolean
    )
}
