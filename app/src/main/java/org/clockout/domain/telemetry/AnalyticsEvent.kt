package org.clockout.domain.telemetry

/**
 * Everything the app may report, in one list.
 *
 * Nothing here carries ritual content - not the question, not the answer, not the note. Only that
 * a step happened, which mood closed the day, and how much of the week is already closed.
 */
sealed interface AnalyticsEvent {
    val name: String
    val params: Map<String, Any>

    data object RitualStarted : AnalyticsEvent {
        override val name: String = "ritual_started"
        override val params: Map<String, Any> = emptyMap()
    }

    data class RitualStepCompleted(val step: String) : AnalyticsEvent {
        override val name: String = "ritual_step_completed"
        override val params: Map<String, Any> get() = mapOf(PARAM_STEP to step)
    }

    data class RitualCompleted(val mood: String, val daysClosedThisWeek: Int) : AnalyticsEvent {
        override val name: String = "ritual_completed"
        override val params: Map<String, Any>
            get() = mapOf(PARAM_MOOD to mood, PARAM_DAYS_CLOSED_WEEK to daysClosedThisWeek)
    }

    /** Raised when the user leaves before the mood is logged, naming the step they left from. */
    data class RitualAbandoned(val step: String) : AnalyticsEvent {
        override val name: String = "ritual_abandoned"
        override val params: Map<String, Any> get() = mapOf(PARAM_STEP to step)
    }

    companion object {
        const val PARAM_STEP: String = "step"
        const val PARAM_MOOD: String = "mood"

        /**
         * [org.clockout.domain.model.RitualStreakInfo.completedThisWeekCount]. The repository has
         * no consecutive-day streak, so the parameter is named for what it actually holds.
         */
        const val PARAM_DAYS_CLOSED_WEEK: String = "days_closed_week"
    }
}
