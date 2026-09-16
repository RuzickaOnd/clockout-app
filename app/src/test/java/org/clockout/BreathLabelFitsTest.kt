package org.clockout

import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.unit.Density
import com.github.takahirom.roborazzi.RobolectricDeviceQualifiers
import org.clockout.domain.model.AppLanguage
import org.clockout.domain.model.BreathingStyle
import org.clockout.domain.model.RitualStep
import org.clockout.presentation.ritual.RitualScreen
import org.clockout.presentation.ritual.RitualState
import org.clockout.presentation.util.AppLocalization
import org.clockout.screenshot.Fixtures
import org.clockout.ui.theme.MyApplicationTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/**
 * The "Inhale..." label sits in a pill of fixed width while its own size is in `sp`, so it grows
 * with the system font setting and the pill does not. Left alone it runs off both sides and the
 * user reads a word with its first letters cut away — at 200% the Czech "Nádech..." rendered as
 * "ádech." with the full stop wrapped onto a line of its own. It is the one word the whole step
 * instructs with, so it has to survive every font scale Android offers.
 *
 * Measured through the text's own layout rather than its bounds: the pill constrains the `Text`,
 * so the node reports the pill's width whether the glyphs fit inside it or not, and an assertion
 * on bounds passes while the word on screen is cut in half.
 */
@RunWith(RobolectricTestRunner::class)
// Without native graphics Robolectric does not measure text at all and reports a nine-pixel
// line for every string, which makes any assertion here meaningless and green.
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = RobolectricDeviceQualifiers.Pixel8, sdk = [36])
class BreathLabelFitsTest {

    @get:Rule val composeTestRule = createComposeRule()

    private fun assertLabelFits(language: AppLanguage, fontScale: Float) {
        composeTestRule.setContent {
            val base = LocalDensity.current
            CompositionLocalProvider(LocalDensity provides Density(base.density, fontScale)) {
                AppLocalization(language) {
                    MyApplicationTheme {
                        RitualScreen(
                            state = RitualState(
                                question = Fixtures.question,
                                currentStep = RitualStep.Breathing,
                                selectedAnswer = "Finished a big task",
                                breathingStyle = BreathingStyle.CLASSIC_CIRCLE
                            ),
                            onIntent = {},
                            onFinish = {}
                        )
                    }
                }
            }
        }
        composeTestRule.mainClock.autoAdvance = false
        composeTestRule.mainClock.advanceTimeBy(500)

        val layout = composeTestRule.onNodeWithTag("breath_label").textLayout()

        // Line count, not `hasVisualOverflow`: that flag reads true even for a label with room to
        // spare, so it is green and red in the wrong places. A label that no longer fits wraps,
        // and the round pill then cuts the ends off both lines.
        assertEquals(
            "at font scale $fontScale the $language breathing label broke onto another line",
            1,
            layout.lineCount
        )
        // Against the constraint, which already has the inset taken out of it, so filling it
        // exactly still leaves the label clear of the curved edge.
        assertTrue(
            "at font scale $fontScale the $language breathing label is ${layout.size.width}px " +
                "wide in ${layout.layoutInput.constraints.maxWidth}px of usable pill",
            layout.size.width <= layout.layoutInput.constraints.maxWidth
        )
    }

    @Test fun `the english label fits at the default font scale`() =
        assertLabelFits(AppLanguage.ENGLISH, 1f)

    @Test fun `the czech label fits at the default font scale`() =
        assertLabelFits(AppLanguage.CZECH, 1f)

    @Test fun `the czech label fits at the largest font scale Android offers`() =
        assertLabelFits(AppLanguage.CZECH, 2f)

    @Test fun `the english label fits at the largest font scale Android offers`() =
        assertLabelFits(AppLanguage.ENGLISH, 2f)
}

private fun androidx.compose.ui.test.SemanticsNodeInteraction.textLayout(): TextLayoutResult {
    val results = mutableListOf<TextLayoutResult>()
    fetchSemanticsNode().config[SemanticsActions.GetTextLayoutResult].action?.invoke(results)
    return results.first()
}
