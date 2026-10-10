package org.clockout.presentation.history

import org.clockout.domain.model.AppLanguage
import org.clockout.domain.model.MoodTag
import org.clockout.domain.model.RitualLogEntry
import org.clockout.domain.model.SkyData

data class HistoryState(
    val language: AppLanguage = AppLanguage.ENGLISH,
    val logs: List<RitualLogEntry> = emptyList(),
    val totalCount: Int = 0,
    val moodCounts: Map<MoodTag, Int> = emptyMap(),
    val isLoading: Boolean = false,
    /** Outlives the logs: after a wipe the history is empty but the sky still has its stars. */
    val sky: SkyData? = null
)
