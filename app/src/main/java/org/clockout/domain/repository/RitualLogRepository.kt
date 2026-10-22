package org.clockout.domain.repository

import kotlinx.coroutines.flow.Flow
import org.clockout.domain.model.MoodTag
import org.clockout.domain.model.RitualLogEntry
import org.clockout.domain.model.RitualStreakInfo
import java.time.LocalDate

interface RitualLogRepository {
    fun getTodayDateString(): String
    fun getLogs(): Flow<List<RitualLogEntry>>
    fun getStreakInfo(): Flow<RitualStreakInfo>
    /**
     * Records a finished ritual: the log entry, its path day, and the lifetime completion count.
     * Returns the day it was recorded under - the clock read for the write, which is not
     * necessarily today by the time the caller looks, as a ritual saved at midnight shows.
     */
    suspend fun logRitual(selectedAnswer: String?, mood: MoodTag, questionText: String): LocalDate
    suspend fun getCompletedRitualCount(): Int
    suspend fun isReviewPromptShown(): Boolean
    suspend fun markReviewPromptShown()
    /** Empties the history; the path, the counter and the review flag survive it. */
    suspend fun clearLogs()
}
