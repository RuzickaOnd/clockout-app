package org.clockout

import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.unit.dp
import org.clockout.domain.model.AppLanguage
import org.clockout.domain.model.SkyData
import org.clockout.presentation.sky.SkyScreen
import org.clockout.presentation.util.AppLocalization
import org.clockout.ui.theme.MyApplicationTheme
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.time.LocalDate

/** A 3:4 sky at the full width of a phone turned sideways would be taller than the window itself. */
@RunWith(RobolectricTestRunner::class)
@Config(qualifiers = "w891dp-h411dp-mdpi", sdk = [36])
class SkyLandscapeFitTest {

    @get:Rule val composeTestRule = createComposeRule()

    @Test
    fun `the whole sky fits a landscape phone`() {
        val day = LocalDate.of(2026, 10, 5)
        composeTestRule.mainClock.autoAdvance = false
        composeTestRule.setContent {
            AppLocalization(AppLanguage.ENGLISH) {
                MyApplicationTheme {
                    SkyScreen(
                        data = SkyData(setOf(day), emptyMap(), mapOf(day to true), LocalDate.of(2026, 10, 9)),
                        language = AppLanguage.ENGLISH,
                        onBack = {}
                    )
                }
            }
        }
        composeTestRule.mainClock.advanceTimeBy(500)

        val bounds = composeTestRule.onNodeWithTag("sky_canvas").getUnclippedBoundsInRoot()
        val height = bounds.bottom - bounds.top
        assertTrue("sky is $height tall in a 411dp window", height <= 411.dp * 0.75f)
    }
}
