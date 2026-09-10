package org.clockout.domain.model

enum class MoodTag(val id: String, val emoji: String) {
    CALM(id = "CALM", emoji = "😌"),
    TIRED(id = "TIRED", emoji = "🥱"),
    SATISFIED(id = "SATISFIED", emoji = "😊"),
    TENSE(id = "TENSE", emoji = "🤯");

    companion object {
        fun fromId(id: String?): MoodTag = entries.find { it.id == id } ?: CALM
    }
}
