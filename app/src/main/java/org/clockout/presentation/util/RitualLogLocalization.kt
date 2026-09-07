package org.clockout.presentation.util

import org.clockout.data.local.QuestionsData
import org.clockout.domain.model.AppLanguage
import org.clockout.domain.model.RitualLogEntry

/**
 * A log was written with the question and the picked suggestion already rendered into the language
 * the app ran in that evening. Home and history therefore look both back up in the phrase catalog
 * and show them in the language the app runs in now; anything the user typed themselves, and any
 * wording the catalog no longer carries, is shown exactly as it was saved.
 */
fun RitualLogEntry.localizedQuestionText(language: AppLanguage): String =
    QuestionsData.localizeQuestionText(questionText, language)

fun RitualLogEntry.localizedAnswer(language: AppLanguage): String? =
    selectedAnswer?.let { QuestionsData.localizeAnswer(it, questionText, language) }
