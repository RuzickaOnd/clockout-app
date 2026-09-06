package org.clockout.data.local.db

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * One closed workday. The date is the primary key, so logging the ritual twice on the same day
 * replaces the previous entry instead of creating a duplicate.
 */
@Entity(tableName = "ritual_logs")
data class RitualLogEntity(
    @PrimaryKey
    @ColumnInfo(name = "date") val date: String, // format "YYYY-MM-DD"
    @ColumnInfo(name = "question_text") val questionText: String,
    @ColumnInfo(name = "selected_answer") val selectedAnswer: String?,
    @ColumnInfo(name = "mood_tag") val moodTagId: String,
    @ColumnInfo(name = "timestamp") val timestamp: Long
)
