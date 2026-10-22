package org.clockout.domain.model

import kotlin.random.Random

/**
 * Today's question and the reverse lookups that let a log written in one language be read in the
 * other. Installed once per process from whatever loaded the questions; the domain never knows
 * they came out of string resources.
 */
object QuestionCatalog {

    /**
     * The catalog together with the reverse lookups built from it, so a rebuild swaps all three at
     * once and a reader never sees questions from one build and lookups from another.
     */
    private class Index(val questions: List<RitualQuestion>) {

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
    private var index: Index? = null

    private val loaded: Index
        get() = index ?: error(
            "QuestionCatalog has not been installed; call QuestionCatalog.install(questions) first."
        )

    val isInstalled: Boolean get() = index != null

    fun install(questions: List<RitualQuestion>) {
        index = Index(questions)
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

private fun String.lookupKey(): String = trim()
