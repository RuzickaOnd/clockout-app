package org.clockout

import org.clockout.domain.model.SkyLayout
import org.clockout.domain.model.SkyPoint
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.hypot

class SkyLayoutTest {

    private val height = 1f / SkyLayout.ASPECT

    @Test
    fun `slots for four and five weeks stay inside the canvas and never overlap`() {
        listOf(listOf(40, 41, 42, 43), listOf(40, 41, 42, 43, 44)).forEach { weeks ->
            val slots = SkyLayout.slots(2026, weeks)
            assertEquals(weeks.size, slots.size)
            // A figure rotated any way fits a circle of radius size * √2 / 2.
            val circles = slots.map { Triple(it.centerX, it.centerY * height, it.size * 0.7072f) }
            circles.forEach { (x, y, r) ->
                assertTrue("x $x r $r", x - r >= 0f && x + r <= 1f)
                assertTrue("y $y r $r", y - r >= 0f && y + r <= height)
            }
            for (i in circles.indices) for (j in i + 1 until circles.size) {
                val (x1, y1, r1) = circles[i]
                val (x2, y2, r2) = circles[j]
                assertTrue("slots $i and $j overlap", hypot(x1 - x2, y1 - y2) >= r1 + r2)
            }
        }
    }

    @Test
    fun `rotation stays within twelve degrees and is the same every time`() {
        val a = SkyLayout.slots(2026, listOf(1, 2, 3, 4, 5))
        assertEquals(a, SkyLayout.slots(2026, listOf(1, 2, 3, 4, 5)))
        assertTrue(a.all { it.rotationDegrees in -12f..12f })
    }

    @Test
    fun `the centre of a figure lands on the centre of its slot`() {
        val slot = SkyLayout.slots(2026, listOf(40, 41, 42, 43)).first()
        val p = SkyLayout.project(slot, SkyPoint(0.5f, 0.5f))
        assertEquals(slot.centerX, p.x, 1e-4f)
        assertEquals(slot.centerY, p.y, 1e-4f)
    }

    @Test
    fun `the star field is fixed by the month`() {
        assertEquals(60, SkyLayout.fieldStars(2026, 10).size)
        assertEquals(SkyLayout.fieldStars(2026, 10), SkyLayout.fieldStars(2026, 10))
        assertTrue(SkyLayout.fieldStars(2026, 10) != SkyLayout.fieldStars(2026, 11))
    }

    @Test
    fun `a tap picks the nearest star within reach and nothing beyond it`() {
        val stars = listOf("a" to SkyPoint(0.2f, 0.2f), "b" to SkyPoint(0.25f, 0.2f))
        assertEquals("b", SkyLayout.nearest(stars, SkyPoint(0.24f, 0.2f), maxDistance = 0.05f))
        assertNull(SkyLayout.nearest(stars, SkyPoint(0.8f, 0.8f), maxDistance = 0.05f))
    }
}
