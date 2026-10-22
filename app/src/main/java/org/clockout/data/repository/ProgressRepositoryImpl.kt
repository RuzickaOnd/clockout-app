package org.clockout.data.repository

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import org.clockout.data.local.RitualLogMigrator
import org.clockout.data.local.db.PracticeDayDao
import org.clockout.domain.model.PathPolicy
import org.clockout.domain.model.PathProgress
import org.clockout.domain.model.PathRhythm
import org.clockout.domain.model.SkyData
import org.clockout.domain.repository.ProgressRepository
import org.clockout.domain.repository.RitualLogRepository
import java.time.LocalDate

class ProgressRepositoryImpl(
    private val practiceDayDao: PracticeDayDao,
    private val logMigrator: RitualLogMigrator,
    private val logs: RitualLogRepository,
    private val includeWeekends: Flow<Boolean>,
    /**
     * Today's date, emitted again whenever it changes. The path's rhythm and the sky's current
     * month depend on the date, and nothing else would emit when it changes under an open screen.
     * The app passes `Context.todayFlow()`, which follows the system's date broadcasts; without a
     * context (tests) it is just today, read once.
     */
    private val todayFlow: Flow<LocalDate>
) : ProgressRepository {

    private val practiceDaysFlow: Flow<Set<LocalDate>> = flow {
        logMigrator.ensureMigrated()
        emitAll(practiceDayDao.observeAll().map { dates -> dates.mapNotNull(::parsePracticeDay).toSet() })
    }

    override fun getPathProgress(): Flow<PathProgress> =
        practiceDaysFlow.combine(todayFlow) { days, today -> PathPolicy.progress(days = days, today = today) }
            // The rhythm walks the whole history; keep that off the main thread.
            .flowOn(Dispatchers.Default)

    override fun getSky(): Flow<SkyData> =
        combine(practiceDaysFlow, logs.getLogs(), todayFlow, includeWeekends) { days, entries, today, weekends ->
            SkyData(
                days = days,
                logs = entries.mapNotNull { entry -> parsePracticeDay(entry.date)?.let { it to entry } }.toMap(),
                rhythmWeeks = PathRhythm.evaluate(days, today).weeks,
                today = today,
                includeWeekends = weekends
            )
        }
            .flowOn(Dispatchers.Default)
}
