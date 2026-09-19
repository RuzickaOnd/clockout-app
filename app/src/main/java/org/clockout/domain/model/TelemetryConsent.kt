package org.clockout.domain.model

/**
 * Whether the user has agreed to anonymous diagnostics.
 *
 * Three states rather than a boolean: "not asked yet" and "said no" have to be distinguishable, or
 * the opt-in dialog would either reappear forever or never appear at all. Stored by [id] rather
 * than by ordinal, so reordering the entries cannot change the meaning of a stored value.
 */
enum class TelemetryConsent(val id: String) {
    UNKNOWN("unknown"),
    GRANTED("granted"),
    DENIED("denied");

    val isGranted: Boolean get() = this == GRANTED

    companion object {
        /** Anything unrecognised means the question still has to be asked. */
        fun fromId(id: String?): TelemetryConsent = entries.firstOrNull { it.id == id } ?: UNKNOWN
    }
}
