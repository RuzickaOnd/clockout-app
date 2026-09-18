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

    fun isOpenNow(): Boolean = contains(Calendar.getInstance().get(Calendar.HOUR_OF_DAY))
}
