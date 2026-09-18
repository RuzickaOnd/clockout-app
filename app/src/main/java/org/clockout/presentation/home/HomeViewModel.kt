package org.clockout.presentation.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import org.clockout.data.repository.RitualRepository
import org.clockout.domain.model.AppLanguage
import org.clockout.domain.model.RitualDayPolicy
import org.clockout.domain.model.RitualStreakInfo
import org.clockout.domain.usecase.GetStreakUseCase
import org.clockout.domain.usecase.GetTodayRitualUseCase
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.Duration
import java.time.LocalDateTime
import java.time.temporal.ChronoUnit

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
                ritualRepository.getIncludeWeekends(),
                withinWindowFlow()
            ) { streakInfo, testMode, language, includeWeekends, isWithinWindow ->
                HomeData(streakInfo, testMode, language, includeWeekends, isWithinWindow)
            }.collect { data ->
                includeWeekends = data.includeWeekends
                _state.update {
                    it.copy(
                        streakInfo = data.streakInfo,
                        isTestModeEnabled = data.testMode,
                        language = data.language,
                        isWithinWindow = data.isWithinWindow,
                        isRitualDay = RitualDayPolicy.isTodayRitualDay(data.includeWeekends)
                    )
                }
            }
        }
    }

    /**
     * Nothing else emits when the clock crosses the window boundary, so with the anytime setting
     * off a user waiting for 16:00 would keep looking at a card that never opens. The answer can
     * only change on the hour, so waking up there is enough.
     */
    private fun withinWindowFlow(): Flow<Boolean> = flow {
        while (true) {
            emit(ritualRepository.isWithinWorkdayCloseWindow())
            delay(millisUntilNextHour())
        }
    }.distinctUntilChanged()

    private fun millisUntilNextHour(): Long {
        val now = LocalDateTime.now()
        return Duration.between(now, now.truncatedTo(ChronoUnit.HOURS).plusHours(1)).toMillis()
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
        val includeWeekends: Boolean,
        val isWithinWindow: Boolean
    )
}
