package org.clockout

import org.clockout.domain.model.BreathingStyle
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * The stored id is what an install carries across updates, so the fallback matters more than the
 * happy path: an app that predates this setting has nothing written down, and a user who once
 * picked a style that has since been removed must still land on something.
 */
class BreathingStyleTest {

    @Test
    fun `every style survives a round trip through its id`() {
        BreathingStyle.entries.forEach { style ->
            assertEquals(style, BreathingStyle.fromId(style.id))
        }
    }

    @Test
    fun `an install that never chose a style keeps the original circle`() {
        assertEquals(BreathingStyle.CLASSIC_CIRCLE, BreathingStyle.fromId(null))
    }

    @Test
    fun `an id that no longer exists falls back instead of throwing`() {
        assertEquals(BreathingStyle.CLASSIC_CIRCLE, BreathingStyle.fromId("nebula_from_a_future_release"))
        assertEquals(BreathingStyle.CLASSIC_CIRCLE, BreathingStyle.fromId(""))
    }

    @Test
    fun `ids are distinct and stable`() {
        val ids = BreathingStyle.entries.map { it.id }
        assertEquals(ids.size, ids.toSet().size)
        assertEquals(listOf("classic_circle", "aurora"), ids)
    }
}
