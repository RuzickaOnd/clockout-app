package org.clockout.domain.model

/**
 * How the breathing step draws itself. The visual is the only thing that changes; the 4 s / 4 s
 * rhythm and the guidance text are the same whichever style is picked.
 */
enum class BreathingStyle(val id: String) {
    /** Concentric frosted circles that scale with the breath. What the app shipped with. */
    CLASSIC_CIRCLE("classic_circle"),

    /** An organic blob filling with layered colour as the user breathes in. */
    AURORA("aurora"),

    /** Rings leaving the centre as the user breathes in and drawn back into it on the way out. */
    CONCENTRIC_RIPPLE("concentric_ripple");

    companion object {
        /**
         * [CLASSIC_CIRCLE] is the fallback, so an install that predates this setting keeps the
         * visual it already had instead of changing appearance on an update.
         */
        fun fromId(id: String?): BreathingStyle = entries.firstOrNull { it.id == id } ?: CLASSIC_CIRCLE
    }
}
