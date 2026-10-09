package org.clockout.data.local

import org.clockout.data.local.db.PracticeDayDao
import org.clockout.data.local.db.PracticeDayEntity
import org.clockout.data.local.db.RitualLogDao
import org.clockout.data.local.db.toEntity
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * Moves ritual logs that older versions kept as a JSON blob in DataStore into the Room table.
 *
 * Runs at most once per install: [LegacyRitualLogSource.markLogsMigratedToRoom] persists the flag
 * and drops the legacy blob, and the mutex keeps concurrent readers and writers from importing twice.
 */
class RitualLogMigrator(
    private val legacySource: LegacyRitualLogSource,
    private val dao: RitualLogDao,
    private val practiceDayDao: PracticeDayDao
) {

    private val mutex = Mutex()

    @Volatile
    private var migrated = false

    suspend fun ensureMigrated() {
        if (migrated) return
        mutex.withLock {
            if (migrated) return
            if (!legacySource.isLogsMigratedToRoom()) {
                val legacyLogs = legacySource.readLegacyLogs()
                if (legacyLogs.isNotEmpty()) {
                    dao.upsertAll(legacyLogs.map { it.toEntity() })
                    // The Room migration that seeds the path ran before this import, so the days
                    // that only now reach ritual_logs have to be written to the path here.
                    practiceDayDao.insertAll(legacyLogs.map { PracticeDayEntity(it.date) })
                }
                legacySource.markLogsMigratedToRoom()
            }
            migrated = true
        }
    }
}
