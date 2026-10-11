package org.clockout.presentation.history

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import org.clockout.data.repository.RitualRepository
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

    /**
     * The entries and the sky come from one read of ritual_logs: the sky already carries every
     * entry by date, so collecting the logs as well would query the table twice on every change.
     */
    private fun loadHistory() {
        viewModelScope.launch {
            combine(repository.getSky(), repository.getLanguage()) { sky, language -> sky to language }
                .collect { (sky, language) ->
                    // Newest day first, the order the history has always been listed in.
                    val logs = sky.logs.entries.sortedByDescending { it.key }.map { it.value }
                    val counts = MoodTag.entries.associateWith { mood -> logs.count { it.moodTag == mood } }
                    _state.update {
                        it.copy(
                            logs = logs,
                            totalCount = logs.size,
                            moodCounts = counts,
                            language = language,
                            sky = sky
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
