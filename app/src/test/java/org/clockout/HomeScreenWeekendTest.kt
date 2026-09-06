package org.clockout

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.assertIsNotDisplayed
import org.clockout.domain.model.AppLanguage
import org.clockout.domain.model.RitualQuestion
import org.clockout.presentation.home.HomeScreen
import org.clockout.presentation.home.HomeState
import org.clockout.ui.theme.MyApplicationTheme
import com.github.takahirom.roborazzi.RobolectricDeviceQualifiers
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * On a weekend the user excluded, the ritual card explains the rest day instead of
 * offering a button that would refuse to work.
 */
@RunWith(RobolectricTestRunner::class)
@Config(qualifiers = RobolectricDeviceQualifiers.Pixel8, sdk = [36])
class HomeScreenWeekendTest {

    @get:Rule val composeTestRule = createComposeRule()

    private val question = RitualQuestion(
        id = "q1",
        textEn = "What went well today?",
        textCs = "Co se dnes povedlo?",
        suggestedAnswersEn = emptyList(),
        suggestedAnswersCs = emptyList()
    )

    private fun setContent(isRitualDay: Boolean) {
        composeTestRule.setContent {
            MyApplicationTheme {
                HomeScreen(
                    state = HomeState(
                        language = AppLanguage.ENGLISH,
                        todayQuestion = question,
                        isRitualDay = isRitualDay,
                        isTestModeEnabled = true
                    ),
                    onStartRitual = {},
                    onNavigateToHistory = {},
                    onNavigateToSettings = {}
                )
            }
        }
    }

    @Test
    fun `a rest day replaces the start button with an explanation`() {
        setContent(isRitualDay = false)

        composeTestRule.onNodeWithTag("start_ritual_button").assertIsNotDisplayed()
        composeTestRule.onNodeWithText("Today is yours").assertIsDisplayed()
    }

    @Test
    fun `a ritual day still offers the start button`() {
        setContent(isRitualDay = true)

        composeTestRule.onNodeWithTag("start_ritual_button").assertIsDisplayed()
    }
}
