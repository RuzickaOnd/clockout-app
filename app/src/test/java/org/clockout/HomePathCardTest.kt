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
import org.clockout.domain.model.RitualQuestion
import org.clockout.presentation.home.HomeScreen
import org.clockout.presentation.home.HomeState
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
class HomePathCardTest {

    @get:Rule val composeTestRule = createComposeRule()

    private val question = RitualQuestion(
        id = "q_success",
        textEn = "What went well today?",
        textCs = "Co se dnes povedlo?",
        suggestedAnswersEn = listOf("Finished an important task"),
        suggestedAnswersCs = listOf("Dokončil(a) jsem důležitý úkol")
    )

    private fun show(progress: PathProgress, onOpenPath: () -> Unit = {}) {
        composeTestRule.setContent {
            AppLocalization(AppLanguage.CZECH) {
                MyApplicationTheme {
                    HomeScreen(
                        state = HomeState(todayQuestion = question, pathProgress = progress),
                        onStartRitual = {},
                        onNavigateToHistory = {},
                        onNavigateToSettings = {},
                        onOpenPath = onOpenPath
                    )
                }
            }
        }
        composeTestRule.onNodeWithTag("path_card").performScrollTo()
    }

    @Test
    fun `a newcomer is told where the path begins`() {
        show(PathProgress.EMPTY)

        composeTestRule.onNodeWithText("Ještě u stolu").assertIsDisplayed()
        composeTestRule.onNodeWithText("Cesta začíná prvním uzavřeným dnem").assertIsDisplayed()
    }

    @Test
    fun `a stage in progress shows the days left and the rhythm`() {
        show(
            PathProgress(
                closedDays = 12, stage = PathStage.KEY_IN_LOCK, nextStage = PathStage.DOWN_THE_STAIRS,
                daysToNextStage = 8, rhythmWeeks = 2, longestRhythmWeeks = 5, isCurrentWeekInRhythm = false
            )
        )

        composeTestRule.onNodeWithText("Klíč v zámku").assertIsDisplayed()
        composeTestRule.onNodeWithText("Ještě 8 dní do dalšího stupně").assertIsDisplayed()
        composeTestRule.onNodeWithText("Další: Schody dolů").assertIsDisplayed()
        composeTestRule.onNodeWithText("Rytmus: 2 týdny · nejdelší 5").assertIsDisplayed()
    }

    @Test
    fun `the last stage says the path goes on`() {
        show(
            PathProgress(
                closedDays = 300, stage = PathStage.HOME, nextStage = null,
                daysToNextStage = null, rhythmWeeks = 0, longestRhythmWeeks = 12, isCurrentWeekInRhythm = false
            )
        )

        composeTestRule.onNodeWithText("Jsi doma. Cestu teď znáš nazpaměť.").assertIsDisplayed()
        composeTestRule.onNodeWithText("Nejdelší 12").assertIsDisplayed()
    }

    @Test
    fun `tapping the card opens the path`() {
        var opened = false
        show(PathProgress.EMPTY, onOpenPath = { opened = true })

        composeTestRule.onNodeWithTag("path_card").performClick()

        assertTrue(opened)
    }
}
