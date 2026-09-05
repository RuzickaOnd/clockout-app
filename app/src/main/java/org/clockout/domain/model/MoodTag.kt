package org.clockout.domain.model

enum class MoodTag(
    val id: String,
    val labelCs: String,
    val labelEn: String,
    val emoji: String,
    val descriptionCs: String,
    val descriptionEn: String
) {
    CALM(
        id = "CALM",
        labelCs = "Klidný",
        labelEn = "Calm",
        emoji = "😌",
        descriptionCs = "Pocit lehkosti a vnitřního klidu",
        descriptionEn = "A sense of lightness and inner calm"
    ),
    TIRED(
        id = "TIRED",
        labelCs = "Unavený",
        labelEn = "Tired",
        emoji = "🥱",
        descriptionCs = "Zasloužený odpočinek po náročném dni",
        descriptionEn = "Well-deserved rest after a demanding day"
    ),
    SATISFIED(
        id = "SATISFIED",
        labelCs = "Spokojený",
        labelEn = "Satisfied",
        emoji = "😊",
        descriptionCs = "Pocit dobře odvedené práce a posunu",
        descriptionEn = "A sense of accomplishment and forward progress"
    ),
    TENSE(
        id = "TENSE",
        labelCs = "Napjatý",
        labelEn = "Tense",
        emoji = "🤯",
        descriptionCs = "Čas nechat napětí za zavřenými dveřmi",
        descriptionEn = "Time to leave tension behind closed doors"
    );

    fun label(lang: AppLanguage): String = when (lang) {
        AppLanguage.CZECH -> labelCs
        AppLanguage.ENGLISH -> labelEn
    }

    fun description(lang: AppLanguage): String = when (lang) {
        AppLanguage.CZECH -> descriptionCs
        AppLanguage.ENGLISH -> descriptionEn
    }

    companion object {
        fun fromId(id: String?): MoodTag = entries.find { it.id == id } ?: CALM
    }
}
