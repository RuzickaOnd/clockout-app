package org.clockout

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import com.github.takahirom.roborazzi.RobolectricDeviceQualifiers
import org.clockout.domain.model.AppLanguage
import org.clockout.domain.model.MoodTag
import org.clockout.domain.model.RitualLogEntry
import org.clockout.domain.model.RitualQuestion
import org.clockout.domain.model.RitualStreakInfo
import org.clockout.presentation.home.HomeScreen
import org.clockout.presentation.home.HomeState
import org.clockout.ui.theme.MyApplicationTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * On a day already closed, the answer alone does not say what it answered, so the card
 * repeats the evening's question above it.
 */
@RunWith(RobolectricTestRunner::class)
@Config(qualifiers = RobolectricDeviceQualifiers.Pixel8, sdk = [36])
class HomeScreenClosedDayAnswerTest {

    @get:Rule val composeTestRule = createComposeRule()

    private val question = RitualQuestion(
        id = "q1",
        textEn = "What went well today?",
        textCs = "Co se dnes povedlo?",
        suggestedAnswersEn = emptyList(),
        suggestedAnswersCs = emptyList()
    )

    private fun setContent(language: AppLanguage) {
        composeTestRule.setContent {
            MyApplicationTheme {
                HomeScreen(
                    state = HomeState(
                        language = language,
                        todayQuestion = question,
                        streakInfo = RitualStreakInfo(
                            completedThisWeekCount = 1,
                            isTodayCompleted = true,
                            todayLog = RitualLogEntry(
                                date = "2026-09-09",
                                questionText = question.textEn,
                                selectedAnswer = "Finished the report",
                                moodTag = MoodTag.CALM
                            ),
                            weekDays = emptyList()
                        )
                    ),
                    onStartRitual = {},
                    onNavigateToHistory = {},
                    onNavigateToSettings = {}
                )
            }
        }
    }

    @Test
    fun `the closed day card shows the question above the answer`() {
        setContent(AppLanguage.ENGLISH)

        composeTestRule.onNodeWithText(question.textEn).assertIsDisplayed()
        composeTestRule.onNodeWithText("Finished the report", substring = true).assertIsDisplayed()
    }
}
