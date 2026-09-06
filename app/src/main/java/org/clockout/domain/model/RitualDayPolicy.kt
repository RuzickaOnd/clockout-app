package org.clockout.domain.model

import java.util.Calendar

/**
 * Decides which weekdays offer the closing ritual. The home screen and the reminder
 * notification both consult this rule, so they cannot drift apart.
 */
object RitualDayPolicy {

    fun isRitualDay(dayOfWeek: Int, includeWeekends: Boolean): Boolean {
        if (includeWeekends) return true
        return dayOfWeek != Calendar.SATURDAY && dayOfWeek != Calendar.SUNDAY
    }

    fun isTodayRitualDay(includeWeekends: Boolean): Boolean =
        isRitualDay(Calendar.getInstance().get(Calendar.DAY_OF_WEEK), includeWeekends)
}
