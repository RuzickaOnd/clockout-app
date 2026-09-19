package org.clockout.domain.telemetry

import org.clockout.domain.model.RitualStep

/**
 * An extension rather than a property on [RitualStep], so the ritual's own model stays free of a
 * concern that belongs to reporting.
 */
fun RitualStep.analyticsId(): String = when (this) {
    RitualStep.Question -> "question"
    RitualStep.Breathing -> "breathing"
    RitualStep.Closing -> "closing"
    RitualStep.Log -> "log"
}
