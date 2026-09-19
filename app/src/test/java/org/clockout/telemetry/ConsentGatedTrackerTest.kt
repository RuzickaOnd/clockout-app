package org.clockout.telemetry

import org.clockout.domain.telemetry.AnalyticsEvent
import org.clockout.domain.telemetry.ConsentGatedTracker
import org.clockout.domain.telemetry.UserProperty
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * The gate is what makes the opt-in real. A fresh instance reports nothing, because an install that
 * has not answered the dialog yet must behave exactly like one that said no.
 */
class ConsentGatedTrackerTest {

    private val delegate = RecordingAnalyticsTracker()
    private val tracker = ConsentGatedTracker(delegate)

    @Test
    fun `nothing is reported before consent is granted`() {
        tracker.track(AnalyticsEvent.RitualStarted)
        tracker.setUserProperty(UserProperty.APP_LANGUAGE, "cs")

        assertTrue(delegate.events.isEmpty())
        assertTrue(delegate.properties.isEmpty())
    }

    @Test
    fun `events flow once collection is enabled`() {
        tracker.setCollectionEnabled(true)

        tracker.track(AnalyticsEvent.RitualStarted)
        tracker.setUserProperty(UserProperty.APP_LANGUAGE, "cs")

        assertEquals(listOf(AnalyticsEvent.RitualStarted), delegate.events)
        assertEquals(mapOf(UserProperty.APP_LANGUAGE to "cs"), delegate.properties)
    }

    @Test
    fun `withdrawing consent stops the flow again`() {
        tracker.setCollectionEnabled(true)
        tracker.track(AnalyticsEvent.RitualStarted)

        tracker.setCollectionEnabled(false)
        tracker.track(AnalyticsEvent.RitualCompleted(mood = "CALM", daysClosedThisWeek = 1))

        assertEquals(listOf(AnalyticsEvent.RitualStarted), delegate.events)
    }

    @Test
    fun `the switch itself always reaches the delegate`() {
        tracker.setCollectionEnabled(true)
        assertTrue(delegate.collectionEnabled)

        tracker.setCollectionEnabled(false)
        assertFalse(delegate.collectionEnabled)
    }
}
