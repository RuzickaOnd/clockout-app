package org.clockout.domain.telemetry

/**
 * What the app reports, independent of who receives it.
 *
 * Call sites talk to this and never to Firebase: the ViewModels behind it are covered by Robolectric
 * tests, and a static Firebase accessor in them would force either an initialized Firebase in every
 * test or static mocking.
 */
interface AnalyticsTracker {
    /** Turns reporting on or off wholesale. Off is the state every install starts in. */
    fun setCollectionEnabled(enabled: Boolean)

    fun track(event: AnalyticsEvent)

    fun setUserProperty(property: UserProperty, value: String)
}
