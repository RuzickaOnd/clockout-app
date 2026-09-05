package org.clockout.presentation.history

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import org.clockout.data.repository.RitualRepository
import org.clockout.domain.model.AppLanguage
import org.clockout.domain.model.MoodTag
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class HistoryViewModel(
    private val repository: RitualRepository
) : ViewModel() {

    private val _state = MutableStateFlow(HistoryState())
    val state: StateFlow<HistoryState> = _state.asStateFlow()

    init {
        loadHistory()
    }

    private fun loadHistory() {
        viewModelScope.launch {
            combine(
                repository.getLogs(),
                repository.getLanguage()
            ) { logs, language ->
                Pair(logs, language)
            }.collect { (logs, language) ->
                val counts = mutableMapOf<MoodTag, Int>()
                MoodTag.entries.forEach { counts[it] = 0 }
                logs.forEach { entry ->
                    counts[entry.moodTag] = (counts[entry.moodTag] ?: 0) + 1
                }

                _state.update {
                    it.copy(
                        logs = logs,
                        totalCount = logs.size,
                        moodCounts = counts,
                        language = language
                    )
                }
            }
        }
    }

    fun clearAllHistory() {
        viewModelScope.launch {
            repository.clearLogs()
        }
    }
}
