package org.clockout.telemetry

import org.clockout.domain.telemetry.AnalyticsEvent
import org.clockout.domain.telemetry.AnalyticsTracker
import org.clockout.domain.telemetry.UserProperty

/** Records what it was asked to report, so a test can assert on it. */
class RecordingAnalyticsTracker : AnalyticsTracker {

    private val _events = mutableListOf<AnalyticsEvent>()
    val events: List<AnalyticsEvent> get() = _events

    private val _properties = mutableMapOf<UserProperty, String>()
    val properties: Map<UserProperty, String> get() = _properties

    var collectionEnabled: Boolean = false
        private set

    override fun setCollectionEnabled(enabled: Boolean) {
        collectionEnabled = enabled
    }

    override fun track(event: AnalyticsEvent) {
        _events += event
    }

    override fun setUserProperty(property: UserProperty, value: String) {
        _properties[property] = value
    }
}
