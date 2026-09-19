package org.clockout.domain.telemetry

/** The default a ViewModel gets, so tests and previews report nothing without arranging anything. */
object NoOpAnalyticsTracker : AnalyticsTracker {
    override fun setCollectionEnabled(enabled: Boolean) = Unit
    override fun track(event: AnalyticsEvent) = Unit
    override fun setUserProperty(property: UserProperty, value: String) = Unit
}
