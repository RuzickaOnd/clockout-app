package org.clockout.data.local.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [RitualLogEntity::class, PracticeDayEntity::class],
    version = 2,
    exportSchema = true
)
abstract class ClockoutDatabase : RoomDatabase() {

    abstract fun ritualLogDao(): RitualLogDao

    abstract fun practiceDayDao(): PracticeDayDao

    companion object {
        const val DATABASE_NAME = "clockout.db"

        /** Seeds the path from the history that already exists, so an update keeps its stage. */
        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("CREATE TABLE IF NOT EXISTS `practice_days` (`date` TEXT NOT NULL, PRIMARY KEY(`date`))")
                db.execSQL("INSERT OR IGNORE INTO practice_days(date) SELECT date FROM ritual_logs")
            }
        }

        @Volatile
        private var instance: ClockoutDatabase? = null

        fun getInstance(context: Context): ClockoutDatabase =
            instance ?: synchronized(this) {
                instance ?: build(context.applicationContext).also { instance = it }
            }

        private fun build(context: Context): ClockoutDatabase =
            Room.databaseBuilder(context, ClockoutDatabase::class.java, DATABASE_NAME)
                .addMigrations(MIGRATION_1_2)
                // TRUNCATE keeps everything in the single .db file instead of a separate WAL,
                // so Android's auto-backup captures a consistent snapshot. The app writes at
                // most one row per day, so there is nothing to gain from WAL here.
                .setJournalMode(JournalMode.TRUNCATE)
                .build()
    }
}
