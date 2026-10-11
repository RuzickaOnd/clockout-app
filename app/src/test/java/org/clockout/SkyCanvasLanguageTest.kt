package org.clockout

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.toPixelMap
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.unit.dp
import org.clockout.domain.model.AppLanguage
import org.clockout.domain.model.SkyData
import org.clockout.presentation.sky.SkyCanvas
import org.clockout.presentation.util.AppLocalization
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.time.LocalDate
import java.time.YearMonth
import kotlin.math.abs

/**
 * SkyActivity starts in English and switches once DataStore answers. The constellation names drawn
 * on the canvas must follow that switch rather than keep what was measured first.
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = "w800dp-h900dp-mdpi", sdk = [36])
class SkyCanvasLanguageTest {

    @get:Rule val composeTestRule = createComposeRule()

    @Test
    fun `names drawn before a language change follow it`() {
        // Monday and Friday of week 41 share a line, so Knitting is lit and named.
        val monday = LocalDate.of(2026, 10, 5)
        val month = SkyData(setOf(monday, monday.plusDays(4)), emptyMap(), mapOf(monday to true), LocalDate.of(2026, 10, 9))
            .month(YearMonth.of(2026, 10))
        val switching = mutableStateOf(AppLanguage.ENGLISH)
        composeTestRule.setContent {
            Row {
                Box(Modifier.width(300.dp).testTag("switched")) {
                    AppLocalization(switching.value) { SkyCanvas(month, twinkle = false) }
                }
                Box(Modifier.width(300.dp).testTag("czech")) {
                    AppLocalization(AppLanguage.CZECH) { SkyCanvas(month, twinkle = false) }
                }
            }
        }
        // Robolectric draws only when an image is asked for: draw the English names first.
        composeTestRule.onNodeWithTag("switched").captureToImage()

        switching.value = AppLanguage.CZECH
        composeTestRule.waitForIdle()

        val switched = composeTestRule.onNodeWithTag("switched").captureToImage().toPixelMap()
        val czech = composeTestRule.onNodeWithTag("czech").captureToImage().toPixelMap()
        // A small tolerance: the background gradient is dithered by screen position, and the two
        // canvases sit at different x. A name drawn in the wrong language differs by far more.
        val differing = (0 until switched.width).sumOf { x ->
            (0 until switched.height).count { y ->
                val a = switched[x, y]
                val b = czech[x, y]
                maxOf(abs(a.red - b.red), abs(a.green - b.green), abs(a.blue - b.blue)) > 0.05f
            }
        }
        assertTrue("$differing pixels differ from a canvas drawn in Czech from the start", differing == 0)
    }
}
