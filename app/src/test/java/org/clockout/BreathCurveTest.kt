package org.clockout

import org.clockout.presentation.components.breathCurve
import org.clockout.presentation.components.isInhalePhase
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.abs

/**
 * The breathing visual is only as calming as the curve behind it. A raised cosine is the shape
 * a slow breath actually has: it comes to a full stop at both turning points and moves fastest
 * halfway through, and it is symmetric, so the inhale and the exhale feel like the same gesture
 * played in two directions. The asymmetric easing this replaced made the exhale end in a jolt.
 */
class BreathCurveTest {

    @Test
    fun `the cycle empties, fills and empties again`() {
        assertEquals(0f, breathCurve(0f), 1e-4f)
        assertEquals(1f, breathCurve(0.5f), 1e-4f)
        assertEquals(0f, breathCurve(1f), 1e-4f)
    }

    @Test
    fun `the exhale mirrors the inhale`() {
        for (step in 1..49) {
            val t = step / 100f
            assertEquals(
                "cycle $t and its mirror should sit at the same depth",
                breathCurve(t),
                breathCurve(1f - t),
                1e-4f
            )
        }
    }

    @Test
    fun `the breath rises for half the cycle and falls for the other half`() {
        for (step in 0..48) {
            val here = breathCurve(step / 100f)
            val next = breathCurve((step + 1) / 100f)
            assertTrue("the inhale should keep rising at ${step / 100f}", next > here)
        }
        for (step in 50..98) {
            val here = breathCurve(step / 100f)
            val next = breathCurve((step + 1) / 100f)
            assertTrue("the exhale should keep falling at ${step / 100f}", next < here)
        }
    }

    @Test
    fun `the breath stalls at the turning points and hurries through the middle`() {
        val atBottom = abs(breathCurve(0.01f) - breathCurve(0f))
        val atTop = abs(breathCurve(0.51f) - breathCurve(0.5f))
        val midInhale = abs(breathCurve(0.26f) - breathCurve(0.25f))

        assertTrue("the turn at the bottom should be the slowest part", midInhale > atBottom * 5f)
        assertTrue("the turn at the top should be the slowest part", midInhale > atTop * 5f)
    }

    @Test
    fun `the loop closes without a seam`() {
        // The cycle driving this restarts, jumping from 1f straight back to 0f. Both ends have to
        // sit at the same depth and leave it at the same rate, otherwise the blob would twitch
        // once every eight seconds.
        assertEquals(breathCurve(1f), breathCurve(0f), 1e-4f)

        val approaching = breathCurve(0.99f) - breathCurve(1f)
        val leaving = breathCurve(0.01f) - breathCurve(0f)
        assertEquals("the turn should be as steep leaving as entering", approaching, leaving, 1e-4f)
    }

    @Test
    fun `the phase flips at the turning point, not somewhere along the way`() {
        assertTrue(isInhalePhase(0f))
        assertTrue(isInhalePhase(0.49f))
        assertFalse(isInhalePhase(0.5f))
        assertFalse(isInhalePhase(0.99f))
    }
}
