package org.clockout

import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.hasAnyDescendant
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performSemanticsAction
import androidx.compose.ui.unit.dp
import com.github.takahirom.roborazzi.RobolectricDeviceQualifiers
import org.clockout.domain.model.AppLanguage
import org.clockout.domain.model.MoodTag
import org.clockout.domain.model.RitualLogEntry
import org.clockout.domain.model.SkyData
import org.clockout.presentation.sky.SkyDayCard
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

@RunWith(RobolectricTestRunner::class)
@Config(qualifiers = RobolectricDeviceQualifiers.Pixel8, sdk = [36])
class SkyScreenTest {

    @get:Rule val composeTestRule = createComposeRule()

    private val today = LocalDate.of(2026, 10, 9)
    private val sky = SkyData(
        // Monday and Friday of week 41 share a line, so Knitting is lit.
        days = setOf(LocalDate.of(2026, 9, 7), LocalDate.of(2026, 10, 5), LocalDate.of(2026, 10, 9)),
        logs = emptyMap(),
        rhythmWeeks = mapOf(LocalDate.of(2026, 10, 5) to true),
        today = today
    )

    private fun show(data: SkyData = sky) {
        composeTestRule.mainClock.autoAdvance = false
        composeTestRule.setContent {
            AppLocalization(AppLanguage.CZECH) {
                MyApplicationTheme { SkyScreen(data = data, language = AppLanguage.CZECH, onBack = {}) }
            }
        }
        composeTestRule.mainClock.advanceTimeBy(500)
    }

    @Test
    fun `the screen opens on the current month with the summary`() {
        show()
        composeTestRule.onNodeWithText("Noční obloha").assertIsDisplayed()
        composeTestRule.onNodeWithText("3 hvězdy", substring = true).assertIsDisplayed()
        composeTestRule.onNodeWithText("Říjen 2026").assertIsDisplayed()
    }

    @Test
    fun `the arrows move between months`() {
        show()
        composeTestRule.onNodeWithTag("sky_prev_month").performClick()
        composeTestRule.mainClock.advanceTimeBy(1_000)
        composeTestRule.onNodeWithText("Září 2026").assertIsDisplayed()
    }

    @Test
    fun `the atlas names a lit constellation and when an unlit one rises`() {
        show()
        // The atlas is composed in full inside the scrolling column, so its tiles exist off screen.
        // Scrolling to them would wait for the twinkle to go idle, which it never does.
        // A tile is clickable, so its texts are merged into the tile node itself.
        composeTestRule.onNode(hasTestTag("sky_atlas_tile_41") and hasText("Pletení")).assertExists()
        composeTestRule.onNode(hasTestTag("sky_atlas_tile_41") and hasText("1×")).assertExists()
        composeTestRule.onNode(hasTestTag("sky_atlas_tile_42") and hasText("vychází v týdnu 42")).assertExists()
    }

    @Test
    fun `without any day the sky explains where the first star comes from`() {
        show(SkyData.EMPTY)
        composeTestRule.onNodeWithTag("sky_empty").assertIsDisplayed()
    }

    @Test
    fun `a star with an entry shows its mood and answer`() {
        val date = LocalDate.of(2026, 10, 5)
        composeTestRule.setContent {
            AppLocalization(AppLanguage.CZECH) {
                MyApplicationTheme {
                    SkyDayCard(
                        date = date,
                        entry = RitualLogEntry(date.toString(), "Co se dnes povedlo?", "Dopsal(a) jsem report", MoodTag.CALM, 0L),
                        language = AppLanguage.CZECH
                    )
                }
            }
        }
        composeTestRule.onNodeWithText("Klidný", substring = true).assertIsDisplayed()
        composeTestRule.onNodeWithText("Dopsal(a) jsem report").assertIsDisplayed()
    }

    @Test
    fun `a star without an entry says the entry was deleted`() {
        composeTestRule.setContent {
            AppLocalization(AppLanguage.CZECH) {
                MyApplicationTheme { SkyDayCard(date = LocalDate.of(2026, 10, 5), entry = null, language = AppLanguage.CZECH) }
            }
        }
        composeTestRule.onNodeWithText("Záznam byl smazán, hvězda zůstává.").assertIsDisplayed()
    }

    @Test
    fun `the screen explains what the sky is about`() {
        show()
        // Composed in full inside the scrolling column; scrolling would wait for the twinkle forever.
        composeTestRule.onNodeWithText("O čem je obloha").assertExists()
        composeTestRule.onNodeWithText("Obloha je tichá sbírka tvých večerů", substring = true).assertExists()
        composeTestRule.onNodeWithText("Souhvězdí, která jsi už rozsvítil(a), a ta, která teprve vyjdou.").assertExists()
    }

    @Test
    fun `the summary recalls last year`() {
        val lastYear = LocalDate.of(2025, 10, 6)
        val thisYear = LocalDate.of(2026, 10, 5)
        show(
            SkyData(
                days = setOf(lastYear, lastYear.plusDays(4), thisYear, thisYear.plusDays(4)),
                logs = emptyMap(),
                rhythmWeeks = mapOf(lastYear to true, thisYear to true),
                today = today
            )
        )
        composeTestRule.onNodeWithText("4 hvězdy · 2 souhvězdí · letos 1 z 53 · loni 1").assertIsDisplayed()
    }

    @Test
    fun `tapping an atlas tile tells the years it was lit in`() {
        show()
        composeTestRule.onNodeWithTag("sky_atlas_tile_41").performSemanticsAction(SemanticsActions.OnClick)
        composeTestRule.mainClock.advanceTimeBy(500)
        composeTestRule.onNode(hasTestTag("sky_atlas_detail") and hasAnyDescendant(hasText("Pletení"))).assertExists()
        composeTestRule.onNode(hasTestTag("sky_atlas_detail") and hasAnyDescendant(hasText("Rozsvíceno: 2026")))
            .assertExists()

        composeTestRule.onNodeWithTag("sky_atlas_tile_42").performSemanticsAction(SemanticsActions.OnClick)
        composeTestRule.mainClock.advanceTimeBy(500)
        composeTestRule.onNode(
            hasTestTag("sky_atlas_detail") and hasAnyDescendant(hasText("Zatím nerozsvíceno – vychází v týdnu 42."))
        ).assertExists()

        composeTestRule.onNodeWithTag("sky_atlas_tile_42").performSemanticsAction(SemanticsActions.OnClick)
        composeTestRule.mainClock.advanceTimeBy(500)
        composeTestRule.onNodeWithTag("sky_atlas_detail").assertDoesNotExist()
    }

    @Test
    fun `the years card opens right under the tapped tile`() {
        show()
        composeTestRule.onNodeWithTag("sky_atlas_tile_41").performSemanticsAction(SemanticsActions.OnClick)
        composeTestRule.mainClock.advanceTimeBy(500)

        val tile = composeTestRule.onNodeWithTag("sky_atlas_tile_41").getUnclippedBoundsInRoot()
        val card = composeTestRule.onNodeWithTag("sky_atlas_detail").getUnclippedBoundsInRoot()
        // Directly below the tile's row: not above the atlas, where a tap far down would never see it.
        assertTrue("card top ${card.top} vs tile bottom ${tile.bottom}", card.top >= tile.bottom)
        assertTrue("card top ${card.top} vs tile bottom ${tile.bottom}", card.top - tile.bottom < 40.dp)
    }

    @Test
    fun `the years card scrolls itself into view`() {
        show()
        composeTestRule.onNodeWithTag("sky_atlas_tile_41").performSemanticsAction(SemanticsActions.OnClick)
        composeTestRule.mainClock.advanceTimeBy(2_000)

        composeTestRule.onNodeWithTag("sky_atlas_detail").assertIsDisplayed()
    }
}
