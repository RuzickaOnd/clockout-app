package com.example.presentation.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.repository.RitualRepository
import com.example.domain.model.AppLanguage
import com.example.domain.usecase.GetStreakUseCase
import com.example.domain.usecase.GetTodayRitualUseCase
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
