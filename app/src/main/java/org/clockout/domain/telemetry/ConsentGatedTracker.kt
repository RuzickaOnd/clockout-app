package org.clockout.domain.telemetry

/**
 * Drops every event until consent has been granted.
 *
 * A decorator rather than a flag inside the Firebase tracker, so the gate is pure Kotlin and can be
 * tested without a Firebase instance. [setCollectionEnabled] is still passed through, because the
 * SDK's own automatic events never travel through [AnalyticsTracker] and only the SDK's flag can
 * stop those.
 *
 * The flag is volatile: the ritual reports from a coroutine while consent is written from another.
 */
class ConsentGatedTracker(private val delegate: AnalyticsTracker) : AnalyticsTracker {

    @Volatile
    private var enabled: Boolean = false

    override fun setCollectionEnabled(enabled: Boolean) {
        this.enabled = enabled
        delegate.setCollectionEnabled(enabled)
    }

    override fun track(event: AnalyticsEvent) {
        if (enabled) delegate.track(event)
    }

    override fun setUserProperty(property: UserProperty, value: String) {
        if (enabled) delegate.setUserProperty(property, value)
    }
}
