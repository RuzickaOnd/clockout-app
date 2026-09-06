package org.clockout.data.local.db

import org.clockout.domain.model.MoodTag
import org.clockout.domain.model.RitualLogEntry

fun RitualLogEntity.toDomain(): RitualLogEntry = RitualLogEntry(
    date = date,
    questionText = questionText,
    selectedAnswer = selectedAnswer,
    moodTag = MoodTag.fromId(moodTagId),
    timestamp = timestamp
)

fun RitualLogEntry.toEntity(): RitualLogEntity = RitualLogEntity(
    date = date,
    questionText = questionText,
    selectedAnswer = selectedAnswer,
    moodTagId = moodTag.id,
    timestamp = timestamp
)
