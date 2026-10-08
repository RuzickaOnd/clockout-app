package org.clockout.domain.model

import java.util.Calendar

/**
 * The stretch of the day the closing ritual belongs to. The end hour is exclusive: "16-21", the
 * span every user-facing string names, means the window is over once the clock reads 21:00.
 */
object WorkdayCloseWindow {

    const val START_HOUR = 16
    const val END_HOUR = 21

    fun contains(hour: Int): Boolean = hour in START_HOUR until END_HOUR

    /**
     * Moves a time into the window by its hour alone, keeping the minute: 10:15 becomes 16:15 and
     * 22:30 becomes 20:30. Used to open the reminder picker somewhere it can be confirmed.
     */
    fun coerce(hour: Int, minute: Int): Pair<Int, Int> =
        hour.coerceIn(START_HOUR, END_HOUR - 1) to minute

    fun isOpenNow(): Boolean = contains(Calendar.getInstance().get(Calendar.HOUR_OF_DAY))
}
