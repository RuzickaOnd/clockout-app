package org.clockout

import org.clockout.domain.model.AppLanguage
import org.clockout.domain.model.QuestionCatalog
import org.clockout.domain.model.RitualQuestion
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

/** The catalog answers from plain data alone - no resources, no Android. */
class QuestionCatalogLookupTest {

    private val calm = RitualQuestion(
        id = "q_calm",
        textEn = "What calmed you?",
        textCs = "Co tě uklidnilo?",
        suggestedAnswersEn = listOf("A walk", "Tea"),
        suggestedAnswersCs = listOf("Procházka", "Čaj")
    )

    @Before
    fun install() = QuestionCatalog.install(listOf(calm))

    @Test
    fun `a stored czech question reads in english`() {
        assertEquals("What calmed you?", QuestionCatalog.localizeQuestionText("Co tě uklidnilo?", AppLanguage.ENGLISH))
    }

    @Test
    fun `a suggestion is re-worded through its own question`() {
        assertEquals("Čaj", QuestionCatalog.localizeAnswer("Tea", "What calmed you?", AppLanguage.CZECH))
    }

    @Test
    fun `wording the catalog does not know is kept as it was saved`() {
        assertEquals("Something else", QuestionCatalog.localizeAnswer("Something else", null, AppLanguage.CZECH))
    }

    @Test
    fun `the same day of the same year always yields the same question`() {
        assertEquals(QuestionCatalog.getTodayQuestion(40, 2026), QuestionCatalog.getTodayQuestion(40, 2026))
    }
}
