package org.clockout

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import com.github.takahirom.roborazzi.RobolectricDeviceQualifiers
import org.clockout.domain.model.AppLanguage
import org.clockout.domain.model.PathProgress
import org.clockout.domain.model.PathStage
import org.clockout.presentation.path.PathScreen
import org.clockout.presentation.settings.SettingsScreen
import org.clockout.presentation.settings.SettingsState
import org.clockout.presentation.util.AppLocalization
import org.clockout.ui.theme.MyApplicationTheme
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(qualifiers = RobolectricDeviceQualifiers.Pixel8, sdk = [36])
class PathScreenTest {

    @get:Rule val composeTestRule = createComposeRule()

    private val progress = PathProgress(
        closedDays = 22, stage = PathStage.DOWN_THE_STAIRS, nextStage = PathStage.STREET_AT_DUSK,
        daysToNextStage = 13, rhythmWeeks = 3, longestRhythmWeeks = 4, isCurrentWeekInRhythm = true
    )

    private fun show(onBack: () -> Unit = {}) {
        composeTestRule.setContent {
            AppLocalization(AppLanguage.CZECH) {
                MyApplicationTheme { PathScreen(progress = progress, onBack = onBack) }
            }
        }
    }

    @Test
    fun `the screen explains the origin and lists every stage in Czech`() {
        show()

        composeTestRule.onNodeWithText("Cesta domů").assertIsDisplayed()
        composeTestRule.onNodeWithText("22 uzavřených dní", substring = true).assertIsDisplayed()
        composeTestRule.onNodeWithText("O čem je cesta").performScrollTo().assertIsDisplayed()
        PathStage.entries.drop(1).forEach { stage ->
            composeTestRule.onNodeWithTag("path_stage_row_${stage.id}").performScrollTo().assertIsDisplayed()
        }
        composeTestRule.onNodeWithText("od 250 dní").performScrollTo().assertIsDisplayed()
        composeTestRule.onNodeWithText("Jak funguje rytmus").performScrollTo().assertIsDisplayed()
    }

    @Test
    fun `the back button closes the screen`() {
        var closed = false
        show(onBack = { closed = true })

        composeTestRule.onNodeWithTag("path_back_button").performClick()

        assertTrue(closed)
    }

    @Test
    fun `the about card opens the path`() {
        var opened = false
        composeTestRule.setContent {
            AppLocalization(AppLanguage.CZECH) {
                MyApplicationTheme {
                    SettingsScreen(
                        state = SettingsState(),
                        onBack = {},
                        onTimeSelected = { _, _ -> },
                        onTestModeToggled = {},
                        onSoundToggled = {},
                        onClearData = {},
                        onOpenPath = { opened = true }
                    )
                }
            }
        }

        composeTestRule.onNodeWithTag("about_path_button").performScrollTo().performClick()

        assertTrue(opened)
    }

    @Test
    fun `a broken rhythm reads the same as on the home card`() {
        composeTestRule.setContent {
            AppLocalization(AppLanguage.CZECH) {
                MyApplicationTheme {
                    PathScreen(
                        progress = progress.copy(rhythmWeeks = 0, longestRhythmWeeks = 4),
                        onBack = {}
                    )
                }
            }
        }
        composeTestRule.onNodeWithText("Nejdelší 4", substring = true).assertIsDisplayed()
        composeTestRule.onNodeWithText("Rytmus: 0", substring = true).assertDoesNotExist()
    }
}
