package org.clockout.domain.telemetry

/**
 * Settings reported as user properties rather than as events, so every metric in the console can be
 * broken down by them - something events cannot do.
 *
 * Firebase allows 24 characters for a user property name, well below the 40 an event name gets.
 */
enum class UserProperty(val propertyName: String) {
    BREATHING_STYLE("breathing_style"),
    SOUND_ENABLED("sound_enabled"),
    APP_LANGUAGE("app_language"),
    INCLUDE_WEEKENDS("include_weekends")
}
