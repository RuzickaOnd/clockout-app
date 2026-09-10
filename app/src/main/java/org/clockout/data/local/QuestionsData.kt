package org.clockout.data.local

import android.content.Context
import androidx.annotation.ArrayRes
import androidx.annotation.StringRes
import org.clockout.R
import org.clockout.domain.model.AppLanguage
import org.clockout.domain.model.RitualQuestion
import org.clockout.util.localizedFor
import kotlin.random.Random

object QuestionsData {

    /** Which string resources a question is built from; the wording itself lives in strings.xml. */
    private data class QuestionResources(
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
        QuestionResources("q_success", R.string.ritual_question_success, R.array.ritual_answers_success),
        QuestionResources("q_let_go", R.string.ritual_question_let_go, R.array.ritual_answers_let_go),
        QuestionResources("q_proud", R.string.ritual_question_proud, R.array.ritual_answers_proud),
        QuestionResources("q_tomorrow", R.string.ritual_question_tomorrow, R.array.ritual_answers_tomorrow),
        QuestionResources("q_signal", R.string.ritual_question_signal, R.array.ritual_answers_signal),
        QuestionResources("q_evening", R.string.ritual_question_evening, R.array.ritual_answers_evening),
        QuestionResources("q_enough", R.string.ritual_question_enough, R.array.ritual_answers_enough),
        QuestionResources("q_hardest", R.string.ritual_question_hardest, R.array.ritual_answers_hardest),
        QuestionResources("q_boundary", R.string.ritual_question_boundary, R.array.ritual_answers_boundary),
        QuestionResources("q_body", R.string.ritual_question_body, R.array.ritual_answers_body),
        QuestionResources("q_learned", R.string.ritual_question_learned, R.array.ritual_answers_learned),
        QuestionResources("q_worry", R.string.ritual_question_worry, R.array.ritual_answers_worry),
        QuestionResources("q_grateful", R.string.ritual_question_grateful, R.array.ritual_answers_grateful),
        QuestionResources("q_people", R.string.ritual_question_people, R.array.ritual_answers_people),
        QuestionResources("q_unfinished", R.string.ritual_question_unfinished, R.array.ritual_answers_unfinished),
        QuestionResources("q_pace", R.string.ritual_question_pace, R.array.ritual_answers_pace),
        QuestionResources("q_attention", R.string.ritual_question_attention, R.array.ritual_answers_attention),
        QuestionResources("q_small_joy", R.string.ritual_question_small_joy, R.array.ritual_answers_small_joy),
        QuestionResources("q_permission", R.string.ritual_question_permission, R.array.ritual_answers_permission),
        QuestionResources("q_energy", R.string.ritual_question_energy, R.array.ritual_answers_energy),
        QuestionResources("q_kindness", R.string.ritual_question_kindness, R.array.ritual_answers_kindness),
        QuestionResources("q_noise", R.string.ritual_question_noise, R.array.ritual_answers_noise),
        QuestionResources("q_next_hour", R.string.ritual_question_next_hour, R.array.ritual_answers_next_hour),
        QuestionResources("q_place", R.string.ritual_question_place, R.array.ritual_answers_place),
        QuestionResources("q_role", R.string.ritual_question_role, R.array.ritual_answers_role),
        QuestionResources("q_slow_down", R.string.ritual_question_slow_down, R.array.ritual_answers_slow_down),
        QuestionResources("q_week", R.string.ritual_question_week, R.array.ritual_answers_week),
        QuestionResources("q_ask", R.string.ritual_question_ask, R.array.ritual_answers_ask),
        QuestionResources("q_repeat", R.string.ritual_question_repeat, R.array.ritual_answers_repeat),
        QuestionResources("q_tomorrow_self", R.string.ritual_question_tomorrow_self, R.array.ritual_answers_tomorrow_self),
    )

    /**
     * The catalog together with the reverse lookups built from it, so a rebuild swaps all three at
     * once and a reader never sees questions from one build and lookups from another.
     */
    private class Catalog(val questions: List<RitualQuestion>) {

        val questionsByText: Map<String, RitualQuestion> = buildMap {
            questions.forEach { question ->
                AppLanguage.entries.forEach { language ->
                    put(question.text(language).lookupKey(), question)
                }
            }
        }

        val answersByText: Map<String, Pair<RitualQuestion, Int>> = buildMap {
            questions.forEach { question ->
                AppLanguage.entries.forEach { language ->
                    question.suggestedAnswers(language).forEachIndexed { index, answer ->
                        put(answer.lookupKey(), question to index)
                    }
                }
            }
        }
    }

    @Volatile
    private var catalog: Catalog? = null

    private val loaded: Catalog
        get() = catalog ?: error(
            "QuestionsData has not been initialized; call QuestionsData.initialize(context) first."
        )

    /**
     * Reads the catalog out of the string resources. Every language is read at once, because a log
     * written in one language has to be found again when the app runs in another, so the phrases
     * cannot come from the configuration the app happens to run under.
     */
    fun initialize(context: Context) {
        val resourcesByLanguage =
            AppLanguage.entries.associateWith { context.localizedFor(it).resources }
        catalog = Catalog(
            catalogResources.map { question ->
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
        )
    }

    val questions: List<RitualQuestion> get() = loaded.questions

    /**
     * The order the questions come round in, reshuffled once a year. Walking the catalog in
     * declaration order made tomorrow's question the next one down the list; a permutation keeps
     * the day's question stable and still hands out the whole pool before repeating any of it.
     */
    private fun rotationFor(year: Int): List<RitualQuestion> = questions.shuffled(Random(year))

    fun getTodayQuestion(dayOfYear: Int, year: Int): RitualQuestion {
        val rotation = rotationFor(year)
        return rotation[dayOfYear.coerceAtLeast(0) % rotation.size]
    }

    private fun String.lookupKey(): String = trim()

    /**
     * A ritual log keeps the question as the plain text shown that evening, so reading it back in
     * another language means finding it in the catalog again. Wording this catalog no longer
     * carries - a custom entry, or a phrase an older app version worded differently - is returned
     * unchanged rather than dropped.
     */
    fun localizeQuestionText(storedText: String, language: AppLanguage): String =
        loaded.questionsByText[storedText.lookupKey()]?.text(language) ?: storedText

    /**
     * The same for the suggestion the user picked. The question it belongs to decides which
     * suggestion list to index into, so an answer is never re-worded through another question that
     * happens to offer the same phrase; answers the user typed themselves are left as they are.
     */
    fun localizeAnswer(
        storedAnswer: String,
        storedQuestionText: String?,
        language: AppLanguage
    ): String {
        val catalog = loaded
        val key = storedAnswer.lookupKey()
        val question = storedQuestionText?.let { catalog.questionsByText[it.lookupKey()] }
        val match = question?.let { it to it.indexOfAnswer(key) }?.takeIf { it.second >= 0 }
            ?: catalog.answersByText[key]
            ?: return storedAnswer
        return match.first.suggestedAnswers(language)[match.second]
    }

    private fun RitualQuestion.indexOfAnswer(key: String): Int {
        AppLanguage.entries.forEach { language ->
            val index = suggestedAnswers(language).indexOfFirst { it.lookupKey() == key }
            if (index >= 0) return index
        }
        return -1
    }
}
