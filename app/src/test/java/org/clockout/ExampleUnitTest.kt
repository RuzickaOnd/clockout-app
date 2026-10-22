package org.clockout

import org.clockout.domain.model.QuestionCatalog
import org.clockout.domain.model.AppLanguage
import org.clockout.domain.model.LanguageOption
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleUnitTest {

    @Test
    fun testAppLanguageDefaultAndFallback() {
        assertEquals(AppLanguage.ENGLISH, AppLanguage.fromCode(null))
        assertEquals(AppLanguage.ENGLISH, AppLanguage.fromCode("en"))
        assertEquals(AppLanguage.CZECH, AppLanguage.fromCode("cs"))
        assertEquals(AppLanguage.ENGLISH, AppLanguage.fromCode("unknown"))
    }

    @Test
    fun testLanguageOptionBehavior() {
        assertEquals(LanguageOption.SYSTEM, LanguageOption.fromCode(null))
        assertEquals(LanguageOption.SYSTEM, LanguageOption.fromCode("system"))
        assertEquals(LanguageOption.SYSTEM, LanguageOption.fromCode("default"))
        assertEquals(LanguageOption.ENGLISH, LanguageOption.fromCode("en"))
        assertEquals(LanguageOption.CZECH, LanguageOption.fromCode("cs"))

        assertEquals(AppLanguage.ENGLISH, LanguageOption.ENGLISH.resolveEffectiveLanguage())
        assertEquals(AppLanguage.CZECH, LanguageOption.CZECH.resolveEffectiveLanguage())
    }

    @Test
    fun testQuestionsDataBilingual() {
        val questions = QuestionCatalog.questions
        assertTrue(questions.isNotEmpty())
        for (q in questions) {
            assertTrue(q.textEn.isNotBlank())
            assertTrue(q.textCs.isNotBlank())
            assertTrue(q.suggestedAnswersEn.isNotEmpty())
            assertTrue(q.suggestedAnswersCs.isNotEmpty())
            assertEquals(q.suggestedAnswersEn.size, q.suggestedAnswersCs.size)
        }
    }
}
