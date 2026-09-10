package org.clockout

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import com.github.takahirom.roborazzi.RobolectricDeviceQualifiers
import org.clockout.domain.model.AppLanguage
import org.clockout.domain.model.RitualQuestion
import org.clockout.domain.model.RitualStreakInfo
import org.clockout.presentation.home.HomeScreen
import org.clockout.presentation.home.HomeState
import org.clockout.presentation.util.AppLocalization
import org.clockout.ui.theme.MyApplicationTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * The screen's text is decided by the provider now, not by a field on the state. The test holds
 * the state in English and the provider in Czech, so it is visible that the provider wins.
 */
@RunWith(RobolectricTestRunner::class)
@Config(qualifiers = RobolectricDeviceQualifiers.Pixel8, sdk = [36])
class HomeScreenLocalizationTest {

    @get:Rule val composeTestRule = createComposeRule()

    private val question = RitualQuestion(
        id = "q_success",
        textEn = "What went well today?",
        textCs = "Co se dnes povedlo?",
        suggestedAnswersEn = listOf("Finished an important task"),
        suggestedAnswersCs = listOf("Dokončil(a) jsem důležitý úkol")
    )

    @Test
    fun `the home screen follows the provider language`() {
        composeTestRule.setContent {
            AppLocalization(AppLanguage.CZECH) {
                MyApplicationTheme {
                    HomeScreen(
                        state = HomeState(
                            language = AppLanguage.ENGLISH,
                            todayQuestion = question
                        ),
                        onStartRitual = {},
                        onNavigateToHistory = {},
                        onNavigateToSettings = {}
                    )
                }
            }
        }

        composeTestRule.onNodeWithText("Uzavři vědomě dnešní den").assertIsDisplayed()
        composeTestRule.onNodeWithText("DNEŠNÍ OTÁZKA").assertIsDisplayed()
    }

    /**
     * home_days_closed_summary picks its plural form off targetWorkDays (the second
     * pluralStringResource argument), not off completedThisWeekCount (the first, %1$d). This
     * test deliberately sets them to counts that would select different Czech plural forms -
     * completedThisWeekCount = 1 (which on its own would pick "one") and targetWorkDays = 3
     * (which picks "few") - so that reading the wrong number as the selector would produce the
     * wrong string. Do not "simplify" this by making the two numbers agree.
     */
    @Test
    fun `the week summary declines by target days, not by closed days`() {
        composeTestRule.setContent {
            AppLocalization(AppLanguage.CZECH) {
                MyApplicationTheme {
                    HomeScreen(
                        state = HomeState(
                            language = AppLanguage.CZECH,
                            todayQuestion = question,
                            streakInfo = RitualStreakInfo(
                                completedThisWeekCount = 1,
                                targetWorkDays = 3,
                                isTodayCompleted = false,
                                todayLog = null,
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

        composeTestRule.onNodeWithText("1 z 3 dnů uzavřeno").assertIsDisplayed()
    }
}
