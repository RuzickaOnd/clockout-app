package org.clockout.presentation.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import org.clockout.data.repository.RitualRepository
import org.clockout.domain.model.AppLanguage
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

    private val _state = MutableStateFlow(
        HomeState(
            todayQuestion = getTodayRitualUseCase(),
            isWithinWindow = ritualRepository.isWithinWorkdayCloseWindow()
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
                ritualRepository.getLanguage()
            ) { streakInfo, testMode, language ->
                Triple(streakInfo, testMode, language)
            }.collect { (streakInfo, testMode, language) ->
                _state.update {
                    it.copy(
                        streakInfo = streakInfo,
                        isTestModeEnabled = testMode,
                        language = language,
                        isWithinWindow = ritualRepository.isWithinWorkdayCloseWindow()
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
                        isWithinWindow = ritualRepository.isWithinWorkdayCloseWindow()
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
}
