package org.clockout

import org.clockout.data.local.QuestionsData
import org.clockout.domain.model.AppLanguage
import org.clockout.domain.model.LanguageOption
import org.clockout.presentation.util.AppStrings
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
    fun testAppStringsLocalization() {
        val enStrings = AppStrings(AppLanguage.ENGLISH)
        val csStrings = AppStrings(AppLanguage.CZECH)

        assertEquals("ClockOut", enStrings.appName)
        assertEquals("Padla", csStrings.appName)

        assertEquals("About ClockOut", enStrings.aboutTitle)
        assertEquals("O aplikaci Padla", csStrings.aboutTitle)

        assertEquals("Work → Leisure transition", enStrings.appSubtitle)
        assertEquals("Přechod práce → volno", csStrings.appSubtitle)

        assertEquals("Start ritual (2–3 min)", enStrings.startRitualButton)
        assertEquals("Zahájit rituál (2–3 min)", csStrings.startRitualButton)

        assertEquals("History", enStrings.navHistory)
        assertEquals("Historie", csStrings.navHistory)

        assertEquals("Settings", enStrings.navSettings)
        assertEquals("Nastavení", csStrings.navSettings)

        assertEquals("Inhale...", enStrings.breathingInhale)
        assertEquals("Nádech...", csStrings.breathingInhale)

        assertEquals("Exhale...", enStrings.breathingExhale)
        assertEquals("Výdech...", csStrings.breathingExhale)

        assertEquals("Disabled", enStrings.notificationsDisabled)
        assertEquals("Vypnuto", csStrings.notificationsDisabled)
        assertEquals("Enabled", enStrings.notificationsEnabled)
        assertEquals("Zapnuto", csStrings.notificationsEnabled)
    }

    @Test
    fun testQuestionsDataBilingual() {
        val questions = QuestionsData.questions
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
