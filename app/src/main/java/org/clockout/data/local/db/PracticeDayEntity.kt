package org.clockout.data.local.db

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * One day the workday was closed, kept apart from [RitualLogEntity] so the path survives
 * "Clear data and history". It deliberately holds the date alone: clearing the history has to
 * remove everything personal, and the mood, question and answer all are.
 */
@Entity(tableName = "practice_days")
data class PracticeDayEntity(
    @PrimaryKey
    @ColumnInfo(name = "date") val date: String // format "YYYY-MM-DD", the same as ritual_logs
)
