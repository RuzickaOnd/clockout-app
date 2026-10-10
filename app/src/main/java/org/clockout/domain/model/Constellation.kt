package org.clockout.domain.model

/** A point on the unit square, y growing downwards like on screen. */
data class SkyPoint(val x: Float, val y: Float)

/**
 * One constellation of the night sky, owned by an ISO week number. [stars] holds exactly seven
 * points, Monday first: the five weekdays draw the recognisable outline, Saturday and Sunday only
 * ornament it, so the figure still reads for someone who never closes a weekend.
 */
data class Constellation(
    val number: Int,
    val stars: List<SkyPoint>,
    val edges: List<Pair<Int, Int>>
)
