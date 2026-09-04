package com.example.presentation.history

import com.example.domain.model.AppLanguage
import com.example.domain.model.MoodTag
import com.example.domain.model.RitualLogEntry

data class HistoryState(
    val language: AppLanguage = AppLanguage.ENGLISH,
    val logs: List<RitualLogEntry> = emptyList(),
    val totalCount: Int = 0,
    val moodCounts: Map<MoodTag, Int> = emptyMap(),
    val isLoading: Boolean = false
)
