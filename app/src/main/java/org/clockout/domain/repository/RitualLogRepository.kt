package org.clockout.domain.repository

import kotlinx.coroutines.flow.Flow
import org.clockout.domain.model.MoodTag
import org.clockout.domain.model.RitualLogEntry
import org.clockout.domain.model.RitualQuestion
import org.clockout.domain.model.RitualStreakInfo

interface RitualLogRepository {
    fun getTodayQuestion(): RitualQuestion
    fun getTodayDateString(): String
    fun getLogs(): Flow<List<RitualLogEntry>>
    fun getStreakInfo(): Flow<RitualStreakInfo>
    /** Records a finished ritual: the log entry, its path day, and the lifetime completion count. */
    suspend fun logRitual(selectedAnswer: String?, mood: MoodTag, questionText: String)
    suspend fun getCompletedRitualCount(): Int
    suspend fun isReviewPromptShown(): Boolean
    suspend fun markReviewPromptShown()
    /** Empties the history; the path, the counter and the review flag survive it. */
    suspend fun clearLogs()
}
