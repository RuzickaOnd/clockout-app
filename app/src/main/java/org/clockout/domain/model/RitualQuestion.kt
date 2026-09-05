package org.clockout.domain.model

data class RitualQuestion(
    val id: String,
    val textEn: String,
    val textCs: String,
    val suggestedAnswersEn: List<String>,
    val suggestedAnswersCs: List<String>
) {
    val text: String get() = textEn
    val suggestedAnswers: List<String> get() = suggestedAnswersEn

    fun text(lang: AppLanguage): String = when (lang) {
        AppLanguage.CZECH -> textCs
        AppLanguage.ENGLISH -> textEn
    }

    fun suggestedAnswers(lang: AppLanguage): List<String> = when (lang) {
        AppLanguage.CZECH -> suggestedAnswersCs
        AppLanguage.ENGLISH -> suggestedAnswersEn
    }
}
