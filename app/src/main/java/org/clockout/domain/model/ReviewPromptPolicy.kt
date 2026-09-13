package org.clockout.domain.model

/**
 * Decides when the app may ask for a Play Store review.
 *
 * Two rules keep the question from becoming a nuisance: it waits until the ritual has had time to
 * prove itself, and it only ever arrives on a day the user left in a good mood. A bad day merely
 * postpones the question - the next good one picks it up - and once asked, the app never asks again.
 */
object ReviewPromptPolicy {

    /** How many completed rituals have to be behind the user before the question is fair. */
    const val MIN_COMPLETED_RITUALS = 10

    private val GOOD_MOODS = setOf(MoodTag.SATISFIED, MoodTag.CALM)

    fun shouldPrompt(completedRituals: Int, mood: MoodTag, alreadyPrompted: Boolean): Boolean =
        !alreadyPrompted &&
            completedRituals >= MIN_COMPLETED_RITUALS &&
            mood in GOOD_MOODS
}
