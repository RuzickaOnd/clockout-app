package org.clockout.data.local.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [RitualLogEntity::class],
    version = 1,
    exportSchema = true
)
abstract class ClockoutDatabase : RoomDatabase() {

    abstract fun ritualLogDao(): RitualLogDao

    companion object {
        const val DATABASE_NAME = "clockout.db"

        @Volatile
        private var instance: ClockoutDatabase? = null

        fun getInstance(context: Context): ClockoutDatabase =
            instance ?: synchronized(this) {
                instance ?: build(context.applicationContext).also { instance = it }
            }

        private fun build(context: Context): ClockoutDatabase =
            Room.databaseBuilder(context, ClockoutDatabase::class.java, DATABASE_NAME)
                // TRUNCATE keeps everything in the single .db file instead of a separate WAL,
                // so Android's auto-backup captures a consistent snapshot. The app writes at
                // most one row per day, so there is nothing to gain from WAL here.
                .setJournalMode(JournalMode.TRUNCATE)
                .build()
    }
}
