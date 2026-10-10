package org.clockout.domain.model

import java.util.Random
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.sin

data class SkySlot(val centerX: Float, val centerY: Float, val size: Float, val rotationDegrees: Float)

data class FieldStar(val x: Float, val y: Float, val radius: Float, val phase: Float)

/**
 * Where a month's constellations sit on a canvas of width 1 and height 1 / [ASPECT]. Every value is
 * derived from the dates through java.util.Random, whose algorithm is specified, so a month always
 * looks the same - for the user and for the screenshot tests.
 */
object SkyLayout {

    const val ASPECT = 0.75f
    private const val FIELD_STARS = 60
    private const val MAX_ROTATION = 12f

    // Zig-zag columns; sized so a rotated figure's bounding circle stays inside and apart.
    private val FOUR = listOf(0.26f to 0.20f, 0.74f to 0.40f, 0.26f to 0.60f, 0.74f to 0.80f)
    private const val FOUR_SIZE = 0.35f
    private val FIVE = listOf(0.26f to 0.17f, 0.74f to 0.335f, 0.26f to 0.50f, 0.74f to 0.665f, 0.26f to 0.83f)
    private const val FIVE_SIZE = 0.31f

    fun slots(year: Int, weekNumbers: List<Int>): List<SkySlot> {
        val (positions, size) = if (weekNumbers.size <= 4) FOUR to FOUR_SIZE else FIVE to FIVE_SIZE
        return weekNumbers.take(positions.size).mapIndexed { i, week ->
            val rotation = Random(year * 100L + week).nextFloat() * 2 * MAX_ROTATION - MAX_ROTATION
            SkySlot(positions[i].first, positions[i].second, size, rotation)
        }
    }

    fun project(slot: SkySlot, point: SkyPoint): SkyPoint {
        val angle = Math.toRadians(slot.rotationDegrees.toDouble())
        val dx = (point.x - 0.5f) * slot.size
        val dy = (point.y - 0.5f) * slot.size
        val rx = (dx * cos(angle) - dy * sin(angle)).toFloat()
        val ry = (dx * sin(angle) + dy * cos(angle)).toFloat()
        return SkyPoint(slot.centerX + rx, slot.centerY + ry * ASPECT)
    }

    fun fieldStars(year: Int, month: Int): List<FieldStar> {
        val random = Random(year * 1_000L + month)
        return List(FIELD_STARS) {
            FieldStar(random.nextFloat(), random.nextFloat(), 0.4f + random.nextFloat() * 0.8f, random.nextFloat())
        }
    }

    /** Distances are measured on the canvas, where a unit of y is 1 / ASPECT units of x. */
    fun <T> nearest(candidates: List<Pair<T, SkyPoint>>, tap: SkyPoint, maxDistance: Float): T? =
        candidates
            .map { (item, p) -> item to hypot(p.x - tap.x, (p.y - tap.y) / ASPECT) }
            .filter { it.second <= maxDistance }
            .minByOrNull { it.second }
            ?.first
}
