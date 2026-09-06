package org.clockout.data.local

import org.clockout.domain.model.RitualLogEntry

/**
 * The pre-Room storage of ritual logs (a JSON blob in DataStore), narrowed down to what
 * [RitualLogMigrator] needs to import it once and then retire it.
 */
interface LegacyRitualLogSource {

    /** Ritual logs still held by the legacy blob, or an empty list once it has been retired. */
    suspend fun readLegacyLogs(): List<RitualLogEntry>

    suspend fun isLogsMigratedToRoom(): Boolean

    /** Records that the import ran and drops the legacy blob so it can never be imported again. */
    suspend fun markLogsMigratedToRoom()
}
