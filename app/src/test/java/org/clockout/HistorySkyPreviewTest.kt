package org.clockout

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.github.takahirom.roborazzi.RobolectricDeviceQualifiers
import org.clockout.domain.model.AppLanguage
import org.clockout.domain.model.MoodTag
import org.clockout.domain.model.RitualLogEntry
import org.clockout.domain.model.SkyData
import org.clockout.presentation.history.HistoryScreen
import org.clockout.presentation.history.HistoryState
import org.clockout.presentation.util.AppLocalization
import org.clockout.ui.theme.MyApplicationTheme
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.time.LocalDate

@RunWith(RobolectricTestRunner::class)
@Config(qualifiers = RobolectricDeviceQualifiers.Pixel8, sdk = [36])
class HistorySkyPreviewTest {

    @get:Rule val composeTestRule = createComposeRule()

    private val day = LocalDate.of(2026, 10, 5)
    private val sky = SkyData(setOf(day), emptyMap(), mapOf(day to false), LocalDate.of(2026, 10, 9))

    private fun show(state: HistoryState, onOpenSky: () -> Unit = {}) {
        composeTestRule.mainClock.autoAdvance = false
        composeTestRule.setContent {
            AppLocalization(AppLanguage.CZECH) {
                MyApplicationTheme {
                    HistoryScreen(state = state, onBack = {}, onClearHistory = {}, onOpenSky = onOpenSky)
                }
            }
        }
        composeTestRule.mainClock.advanceTimeBy(500)
    }

    @Test
    fun `the sky preview shows above an empty history`() {
        show(HistoryState(sky = sky))
        composeTestRule.onNodeWithTag("sky_preview_card").assertIsDisplayed()
        composeTestRule.onNodeWithText("1 hvězda", substring = true).assertIsDisplayed()
    }

    @Test
    fun `the sky preview sits under the mood summary of a full history`() {
        val entry = RitualLogEntry(day.toString(), "Q", null, MoodTag.CALM, 1L)
        show(HistoryState(logs = listOf(entry), totalCount = 1, moodCounts = mapOf(MoodTag.CALM to 1), sky = sky))
        composeTestRule.onNodeWithTag("sky_preview_card").assertIsDisplayed()
    }

    @Test
    fun `without any day there is no preview`() {
        show(HistoryState(sky = SkyData.EMPTY))
        composeTestRule.onNodeWithTag("sky_preview_card").assertDoesNotExist()
    }

    @Test
    fun `tapping the preview opens the sky`() {
        var opened = false
        show(HistoryState(sky = sky), onOpenSky = { opened = true })
        composeTestRule.onNodeWithTag("sky_preview_card").performClick()
        assertTrue(opened)
    }
}
