package org.clockout.data.local

import android.content.Context
import androidx.annotation.ArrayRes
import androidx.annotation.StringRes
import org.clockout.R
import org.clockout.domain.model.AppLanguage
import org.clockout.domain.model.RitualQuestion
import org.clockout.util.localizedFor

object QuestionResources {

    /** Which string resources a question is built from; the wording itself lives in strings.xml. */
    private data class QuestionResourceIds(
        val id: String,
        @param:StringRes val text: Int,
        @param:ArrayRes val answers: Int
    )

    /**
     * The catalog in declaration order, which is what the yearly rotation is drawn from. Only the
     * ids and the resource handles live here - adding a language means adding a values-xx file,
     * not touching this list.
     */
    private val catalogResources = listOf(
        QuestionResourceIds("q_success", R.string.ritual_question_success, R.array.ritual_answers_success),
        QuestionResourceIds("q_let_go", R.string.ritual_question_let_go, R.array.ritual_answers_let_go),
        QuestionResourceIds("q_proud", R.string.ritual_question_proud, R.array.ritual_answers_proud),
        QuestionResourceIds("q_tomorrow", R.string.ritual_question_tomorrow, R.array.ritual_answers_tomorrow),
        QuestionResourceIds("q_signal", R.string.ritual_question_signal, R.array.ritual_answers_signal),
        QuestionResourceIds("q_evening", R.string.ritual_question_evening, R.array.ritual_answers_evening),
        QuestionResourceIds("q_enough", R.string.ritual_question_enough, R.array.ritual_answers_enough),
        QuestionResourceIds("q_hardest", R.string.ritual_question_hardest, R.array.ritual_answers_hardest),
        QuestionResourceIds("q_boundary", R.string.ritual_question_boundary, R.array.ritual_answers_boundary),
        QuestionResourceIds("q_body", R.string.ritual_question_body, R.array.ritual_answers_body),
        QuestionResourceIds("q_learned", R.string.ritual_question_learned, R.array.ritual_answers_learned),
        QuestionResourceIds("q_worry", R.string.ritual_question_worry, R.array.ritual_answers_worry),
        QuestionResourceIds("q_grateful", R.string.ritual_question_grateful, R.array.ritual_answers_grateful),
        QuestionResourceIds("q_people", R.string.ritual_question_people, R.array.ritual_answers_people),
        QuestionResourceIds("q_unfinished", R.string.ritual_question_unfinished, R.array.ritual_answers_unfinished),
        QuestionResourceIds("q_pace", R.string.ritual_question_pace, R.array.ritual_answers_pace),
        QuestionResourceIds("q_attention", R.string.ritual_question_attention, R.array.ritual_answers_attention),
        QuestionResourceIds("q_small_joy", R.string.ritual_question_small_joy, R.array.ritual_answers_small_joy),
        QuestionResourceIds("q_permission", R.string.ritual_question_permission, R.array.ritual_answers_permission),
        QuestionResourceIds("q_energy", R.string.ritual_question_energy, R.array.ritual_answers_energy),
        QuestionResourceIds("q_kindness", R.string.ritual_question_kindness, R.array.ritual_answers_kindness),
        QuestionResourceIds("q_noise", R.string.ritual_question_noise, R.array.ritual_answers_noise),
        QuestionResourceIds("q_next_hour", R.string.ritual_question_next_hour, R.array.ritual_answers_next_hour),
        QuestionResourceIds("q_place", R.string.ritual_question_place, R.array.ritual_answers_place),
        QuestionResourceIds("q_role", R.string.ritual_question_role, R.array.ritual_answers_role),
        QuestionResourceIds("q_slow_down", R.string.ritual_question_slow_down, R.array.ritual_answers_slow_down),
        QuestionResourceIds("q_week", R.string.ritual_question_week, R.array.ritual_answers_week),
        QuestionResourceIds("q_ask", R.string.ritual_question_ask, R.array.ritual_answers_ask),
        QuestionResourceIds("q_repeat", R.string.ritual_question_repeat, R.array.ritual_answers_repeat),
        QuestionResourceIds("q_tomorrow_self", R.string.ritual_question_tomorrow_self, R.array.ritual_answers_tomorrow_self),
    )

    /**
     * Reads the questions out of the string resources. Every language is read at once, because a
     * log written in one language has to be found again when the app runs in another, so the
     * phrases cannot come from the configuration the app happens to run under.
     */
    fun load(context: Context): List<RitualQuestion> {
        val resourcesByLanguage =
            AppLanguage.entries.associateWith { context.localizedFor(it).resources }
        return catalogResources.map { question ->
            fun text(language: AppLanguage) =
                resourcesByLanguage.getValue(language).getString(question.text)

            fun answers(language: AppLanguage) =
                resourcesByLanguage.getValue(language).getStringArray(question.answers).toList()

            RitualQuestion(
                id = question.id,
                textEn = text(AppLanguage.ENGLISH),
                textCs = text(AppLanguage.CZECH),
                suggestedAnswersEn = answers(AppLanguage.ENGLISH),
                suggestedAnswersCs = answers(AppLanguage.CZECH)
            )
        }
    }
}
