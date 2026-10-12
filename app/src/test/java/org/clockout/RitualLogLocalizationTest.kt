package org.clockout

import org.clockout.domain.model.QuestionCatalog
import org.clockout.domain.model.AppLanguage
import org.clockout.domain.model.MoodTag
import org.clockout.domain.model.RitualLogEntry
import org.clockout.presentation.util.localizedAnswer
import org.clockout.presentation.util.localizedQuestionText
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Logs store the question and the picked suggestion as plain text in whatever language was active
 * that evening, so history has to translate them back into the language the app runs in now.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class RitualLogLocalizationTest {

    private fun entry(question: String, answer: String?) = RitualLogEntry(
        date = "2026-09-06",
        questionText = question,
        selectedAnswer = answer,
        moodTag = MoodTag.CALM
    )

    @Test
    fun `a czech log reads in english`() {
        val log = entry("Co se dnes povedlo?", "Dokončil(a) jsem důležitý úkol")

        assertEquals("What went well today?", log.localizedQuestionText(AppLanguage.ENGLISH))
        assertEquals("Finished an important task", log.localizedAnswer(AppLanguage.ENGLISH))
    }

    @Test
    fun `an english log reads in czech`() {
        val log = entry("What are you proud of today?", "My patience and steady energy")

        assertEquals("Na co jsi dnes hrdý/á?", log.localizedQuestionText(AppLanguage.CZECH))
        assertEquals("Na svou trpělivost a energii", log.localizedAnswer(AppLanguage.CZECH))
    }

    @Test
    fun `every catalog phrase survives a round trip through both languages`() {
        QuestionCatalog.questions.forEach { question ->
            AppLanguage.entries.forEach { stored ->
                AppLanguage.entries.forEach { shown ->
                    val log = entry(question.text(stored), question.suggestedAnswers(stored).first())

                    assertEquals(
                        question.text(shown),
                        log.localizedQuestionText(shown)
                    )
                    assertEquals(
                        question.suggestedAnswers(shown).first(),
                        log.localizedAnswer(shown)
                    )
                }
            }
        }
    }

    @Test
    fun `an answer keeps its own question's wording rather than the first match`() {
        val letGo = QuestionCatalog.questions.first { it.id == "q_let_go" }
        val log = entry(letGo.textCs, letGo.suggestedAnswersCs[2])

        assertEquals(letGo.suggestedAnswersEn[2], log.localizedAnswer(AppLanguage.ENGLISH))
    }

    @Test
    fun `an answer the user typed is left untouched`() {
        val log = entry("Co se dnes povedlo?", "Konečně jsem dopsal ten dopis")

        assertEquals("Konečně jsem dopsal ten dopis", log.localizedAnswer(AppLanguage.ENGLISH))
    }

    @Test
    fun `wording from a future or older catalog is left untouched`() {
        val log = entry("What did the day teach you?", "Something else entirely")

        assertEquals("What did the day teach you?", log.localizedQuestionText(AppLanguage.CZECH))
        assertEquals("Something else entirely", log.localizedAnswer(AppLanguage.CZECH))
    }

    @Test
    fun `a missing answer stays missing`() {
        val log = entry("Co se dnes povedlo?", null)

        assertNull(log.localizedAnswer(AppLanguage.CZECH))
    }

    @Test
    fun `surrounding whitespace does not defeat the lookup`() {
        val log = entry("  Co se dnes povedlo?  ", "  Dokončil(a) jsem důležitý úkol ")

        assertEquals("What went well today?", log.localizedQuestionText(AppLanguage.ENGLISH))
        assertEquals("Finished an important task", log.localizedAnswer(AppLanguage.ENGLISH))
    }
}
