package org.clockout

import org.clockout.data.local.QuestionsData
import org.clockout.domain.model.AppLanguage
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * The catalog has to stay large enough that the question of the day does not come round often,
 * and unambiguous enough that a logged phrase can be found in it again when the app language
 * changes.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class QuestionsCatalogTest {

    private val questions = QuestionsData.questions

    @Test
    fun `the pool is large enough to last weeks of workdays`() {
        assertTrue(
            "expected at least 30 questions, found ${questions.size}",
            questions.size >= 30
        )
    }

    @Test
    fun `every question is worded in both languages`() {
        questions.forEach { question ->
            assertTrue(question.id, question.textEn.isNotBlank())
            assertTrue(question.id, question.textCs.isNotBlank())
        }
    }

    @Test
    fun `every question offers exactly four suggestions in both languages`() {
        questions.forEach { question ->
            AppLanguage.entries.forEach { language ->
                val answers = question.suggestedAnswers(language)
                assertEquals(question.id, 4, answers.size)
                answers.forEach { assertTrue(question.id, it.isNotBlank()) }
            }
        }
    }

    @Test
    fun `ids are unique`() {
        assertEquals(questions.size, questions.map { it.id }.distinct().size)
    }

    @Test
    fun `no wording is shared between questions`() {
        val texts = questions.flatMap { question ->
            AppLanguage.entries.map { question.text(it) }
        }
        assertEquals("duplicate question wording", texts.size, texts.distinct().size)

        val answers = questions.flatMap { question ->
            AppLanguage.entries.flatMap { question.suggestedAnswers(it) }
        }
        assertEquals("duplicate suggestion wording", answers.size, answers.distinct().size)
    }

    @Test
    fun `the same day always resolves to the same question`() {
        assertEquals(
            QuestionsData.getTodayQuestion(dayOfYear = 61, year = 2026).id,
            QuestionsData.getTodayQuestion(dayOfYear = 61, year = 2026).id
        )
    }

    @Test
    fun `a full cycle uses every question exactly once`() {
        val cycle = (1..questions.size).map {
            QuestionsData.getTodayQuestion(dayOfYear = it, year = 2026).id
        }

        assertEquals(questions.size, cycle.distinct().size)
    }

    @Test
    fun `consecutive days do not simply walk down the declared list`() {
        val cycle = (1..questions.size).map {
            QuestionsData.getTodayQuestion(dayOfYear = it, year = 2026).id
        }

        assertNotEquals(questions.map { it.id }, cycle)
    }

    @Test
    fun `each year shuffles the pool differently`() {
        fun cycleOf(year: Int) = (1..questions.size).map {
            QuestionsData.getTodayQuestion(dayOfYear = it, year = year).id
        }

        assertNotEquals(cycleOf(2026), cycleOf(2027))
        assertNotEquals(cycleOf(2027), cycleOf(2028))
    }

    @Test
    fun `a day of year outside the calendar still resolves`() {
        QuestionsData.getTodayQuestion(dayOfYear = 0, year = 2026)
        QuestionsData.getTodayQuestion(dayOfYear = -5, year = 2026)
        QuestionsData.getTodayQuestion(dayOfYear = 366, year = 2026)
    }
}
