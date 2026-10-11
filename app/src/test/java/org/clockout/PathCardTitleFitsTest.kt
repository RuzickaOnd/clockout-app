package org.clockout

import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.text.TextLayoutResult
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
import org.robolectric.annotation.GraphicsMode

/** Without NATIVE graphics Robolectric lays no text out, and any width assertion passes. */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = RobolectricDeviceQualifiers.Pixel8, sdk = [36])
class PathCardTitleFitsTest {

    @get:Rule val composeTestRule = createComposeRule()

    @Test
    fun `the longest Czech stage name fits in two lines`() {
        composeTestRule.setContent {
            AppLocalization(AppLanguage.CZECH) {
                MyApplicationTheme {
                    HomeScreen(
                        state = HomeState(
                            todayQuestion = RitualQuestion(
                                id = "q", textEn = "Q", textCs = "Q",
                                suggestedAnswersEn = emptyList(), suggestedAnswersCs = emptyList()
                            ),
                            pathProgress = PathProgress(1, PathStage.LAPTOP_CLOSED, PathStage.COAT_ON, 4, 0, 0, false)
                        ),
                        onStartRitual = {},
                        onNavigateToHistory = {},
                        onNavigateToSettings = {}
                    )
                }
            }
        }

        val layouts = mutableListOf<TextLayoutResult>()
        composeTestRule.onNodeWithTag("path_card_stage_title", useUnmergedTree = true)
            .fetchSemanticsNode().config[SemanticsActions.GetTextLayoutResult].action?.invoke(layouts)
        assertTrue("lineCount = ${layouts.single().lineCount}", layouts.single().lineCount <= 2)
    }
}
