package org.clockout.domain.model

/**
 * The stages of the way home: the commute that working from home took away, rebuilt one closed
 * day at a time. [AT_DESK] is the state before the first closed day, so a new install has an
 * emblem of its own. Names, verses and icons
 * live in `presentation/util/Localization.kt`, the same way [MoodTag] keeps its strings out.
 */
enum class PathStage(val id: String, val minDays: Int) {
    AT_DESK("AT_DESK", 0),
    LAPTOP_CLOSED("LAPTOP_CLOSED", 1),
    COAT_ON("COAT_ON", 5),
    KEY_IN_LOCK("KEY_IN_LOCK", 10),
    DOWN_THE_STAIRS("DOWN_THE_STAIRS", 20),
    STREET_AT_DUSK("STREET_AT_DUSK", 35),
    THROUGH_THE_PARK("THROUGH_THE_PARK", 50),
    OVER_THE_BRIDGE("OVER_THE_BRIDGE", 75),
    GARDEN_GATE("GARDEN_GATE", 100),
    THRESHOLD("THRESHOLD", 150),
    HOME("HOME", 250);

    val next: PathStage?
        get() = entries.getOrNull(ordinal + 1)

    companion object {
        fun forDays(days: Int): PathStage = entries.last { it.minDays <= days.coerceAtLeast(0) }
    }
}
