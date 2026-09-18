package org.clockout

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import com.github.takahirom.roborazzi.RobolectricDeviceQualifiers
import org.clockout.domain.model.AppLanguage
import org.clockout.domain.model.RitualQuestion
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
 * With "ritual available anytime" switched off the home screen only offers the ritual inside
 * the 16-21 window, and explains the wait instead of showing a button the setting has closed.
 */
@RunWith(RobolectricTestRunner::class)
@Config(qualifiers = RobolectricDeviceQualifiers.Pixel8, sdk = [36])
class HomeScreenWindowTest {

    @get:Rule val composeTestRule = createComposeRule()

    private val question = RitualQuestion(
        id = "q1",
        textEn = "What went well today?",
        textCs = "Co se dnes povedlo?",
        suggestedAnswersEn = emptyList(),
        suggestedAnswersCs = emptyList()
    )

    private fun setContent(isTestModeEnabled: Boolean, isWithinWindow: Boolean) {
        composeTestRule.setContent {
            AppLocalization(AppLanguage.ENGLISH) {
                MyApplicationTheme {
                    HomeScreen(
                        state = HomeState(
                            language = AppLanguage.ENGLISH,
                            todayQuestion = question,
                            isRitualDay = true,
                            isTestModeEnabled = isTestModeEnabled,
                            isWithinWindow = isWithinWindow
                        ),
                        onStartRitual = {},
                        onNavigateToHistory = {},
                        onNavigateToSettings = {}
                    )
                }
            }
        }
    }

    @Test
    fun `outside the window the switched off setting replaces the button with an explanation`() {
        setContent(isTestModeEnabled = false, isWithinWindow = false)

        composeTestRule.onNodeWithTag("start_ritual_button").assertIsNotDisplayed()
        composeTestRule.onNodeWithText("The ritual opens at the end of the day").assertIsDisplayed()
    }

    @Test
    fun `inside the window the ritual starts even with the setting switched off`() {
        setContent(isTestModeEnabled = false, isWithinWindow = true)

        composeTestRule.onNodeWithTag("start_ritual_button").assertIsDisplayed()
    }

    @Test
    fun `the setting unlocks the ritual outside the window`() {
        setContent(isTestModeEnabled = true, isWithinWindow = false)

        composeTestRule.onNodeWithTag("start_ritual_button").assertIsDisplayed()
    }
}
