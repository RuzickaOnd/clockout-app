package org.clockout.data.local.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface PracticeDayDao {

    @Query("SELECT date FROM practice_days")
    fun observeAll(): Flow<List<String>>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(day: PracticeDayEntity)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertAll(days: List<PracticeDayEntity>)
}
