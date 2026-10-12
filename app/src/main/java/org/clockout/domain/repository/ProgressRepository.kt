package org.clockout.domain.repository

import kotlinx.coroutines.flow.Flow
import org.clockout.domain.model.PathProgress
import org.clockout.domain.model.SkyData

interface ProgressRepository {
    /** The path's stage and weekly rhythm; survives [RitualLogRepository.clearLogs], which only empties the history. */
    fun getPathProgress(): Flow<PathProgress>
    /** The night sky: practice days, the entries still in the history, and the rhythm per week. */
    fun getSky(): Flow<SkyData>
}
