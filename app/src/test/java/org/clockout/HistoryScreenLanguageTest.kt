package org.clockout

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import com.github.takahirom.roborazzi.RobolectricDeviceQualifiers
import org.clockout.domain.model.AppLanguage
import org.clockout.domain.model.MoodTag
import org.clockout.domain.model.RitualLogEntry
import org.clockout.presentation.history.HistoryScreen
import org.clockout.presentation.history.HistoryState
import org.clockout.ui.theme.MyApplicationTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * A day closed in Czech has to read back in English once the app switches language, so the
 * history never mixes the two.
 */
@RunWith(RobolectricTestRunner::class)
@Config(qualifiers = RobolectricDeviceQualifiers.Pixel8, sdk = [36])
class HistoryScreenLanguageTest {

    @get:Rule val composeTestRule = createComposeRule()

    private val czechLog = RitualLogEntry(
        date = "2026-09-06",
        questionText = "Co se dnes povedlo?",
        selectedAnswer = "Dokončil(a) jsem důležitý úkol",
        moodTag = MoodTag.CALM,
        timestamp = 0L
    )

    private fun setContent(language: AppLanguage, log: RitualLogEntry) {
        composeTestRule.setContent {
            MyApplicationTheme {
                HistoryScreen(
                    state = HistoryState(language = language, logs = listOf(log), totalCount = 1),
                    onBack = {},
                    onClearHistory = {}
                )
            }
        }
    }

    @Test
    fun `a day logged in czech reads in english`() {
        setContent(AppLanguage.ENGLISH, czechLog)

        composeTestRule.onNodeWithText("What went well today?").assertIsDisplayed()
        composeTestRule.onNodeWithText("“Finished an important task”").assertIsDisplayed()
    }

    @Test
    fun `the same day still reads in czech`() {
        setContent(AppLanguage.CZECH, czechLog)

        composeTestRule.onNodeWithText("Co se dnes povedlo?").assertIsDisplayed()
        composeTestRule.onNodeWithText("„Dokončil(a) jsem důležitý úkol“").assertIsDisplayed()
    }

    @Test
    fun `an answer the user typed is shown as they wrote it`() {
        setContent(AppLanguage.ENGLISH, czechLog.copy(selectedAnswer = "Konečně dopsaný dopis"))

        composeTestRule.onNodeWithText("“Konečně dopsaný dopis”").assertIsDisplayed()
    }
}
