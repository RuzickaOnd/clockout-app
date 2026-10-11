package org.clockout.data.local.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
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

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertPracticeDay(day: PracticeDayEntity)

    /**
     * A closed day's entry and its practice day in one transaction, so a crash in between can
     * never leave an entry in the history whose day the path does not count.
     */
    @Transaction
    suspend fun logDay(entry: RitualLogEntity, day: PracticeDayEntity) {
        upsert(entry)
        insertPracticeDay(day)
    }

    @Query("DELETE FROM ritual_logs")
    suspend fun deleteAll()
}
