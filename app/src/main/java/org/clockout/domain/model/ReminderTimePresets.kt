package org.clockout.domain.model

/** The reminder times Settings offers as one-tap chips, as hour and minute. */
object ReminderTimePresets {

    /** The reminder time of a fresh install, and the one a time outside the ritual window falls back to. */
    const val DEFAULT_HOUR = 17
    const val DEFAULT_MINUTE = 30

    val ALL: List<Pair<Int, Int>> = listOf(
        16 to 0,
        16 to 30,
        17 to 0,
        17 to 30,
        18 to 0,
        18 to 30,
        19 to 0
    )

    fun contains(hour: Int, minute: Int): Boolean = (hour to minute) in ALL
}
