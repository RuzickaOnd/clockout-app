package org.clockout.data.local.db

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

@Dao
interface RitualLogDao {

    /** Newest day first, matching the order the history screen renders. */
    @Query("SELECT * FROM ritual_logs ORDER BY date DESC")
    fun observeAll(): Flow<List<RitualLogEntity>>

    @Query("SELECT * FROM ritual_logs WHERE date = :date LIMIT 1")
    suspend fun getByDate(date: String): RitualLogEntity?

    @Query("SELECT COUNT(*) FROM ritual_logs")
    suspend fun count(): Int

    @Upsert
    suspend fun upsert(entry: RitualLogEntity)

    @Upsert
    suspend fun upsertAll(entries: List<RitualLogEntity>)

    @Query("DELETE FROM ritual_logs")
    suspend fun deleteAll()
}
