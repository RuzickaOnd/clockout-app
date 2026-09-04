package com.example.domain.model

data class RitualLogEntry(
    val date: String, // format "YYYY-MM-DD"
    val questionText: String,
    val selectedAnswer: String?,
    val moodTag: MoodTag,
    val timestamp: Long = System.currentTimeMillis()
)
