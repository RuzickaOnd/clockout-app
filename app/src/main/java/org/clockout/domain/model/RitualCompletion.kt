package org.clockout.domain.model

/** How far the first constellation still is: the stars so far and the closed days still needed. */
data class SkyTeaser(val stars: Int, val daysToFirstConstellation: Int)

/** What a finished ritual changed, for the completion card and the one-off review request. */
data class RitualCompletion(
    val reachedStage: PathStage?,
    val litConstellationNumber: Int?,
    val skyTeaser: SkyTeaser?,
    val shouldRequestReview: Boolean,
    val daysClosedThisWeek: Int
)
