package org.clockout.telemetry

import org.clockout.domain.model.RitualStep
import org.clockout.domain.telemetry.AnalyticsEvent
import org.clockout.domain.telemetry.UserProperty
import org.clockout.domain.telemetry.analyticsId
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Firebase drops a malformed event name silently - no exception, no log, the event simply never
 * appears in the console. A name that breaks a limit is therefore invisible until someone wonders
 * months later why a funnel has a hole in it, which is why the rules are a test rather than a
 * review comment.
 */
class AnalyticsEventTest {

    private val allEvents = listOf(
        AnalyticsEvent.RitualStarted,
        AnalyticsEvent.RitualStepCompleted(RitualStep.Breathing.analyticsId()),
        AnalyticsEvent.RitualCompleted(mood = "CALM", daysClosedThisWeek = 3),
        AnalyticsEvent.RitualAbandoned(RitualStep.Question.analyticsId())
    )

    private val namePattern = Regex("^[a-z][a-z0-9_]*$")
    private val reservedPrefixes = listOf("firebase_", "google_", "ga_")

    @Test
    fun `event names fit Firebase's rules`() {
        allEvents.forEach { event ->
            assertTrue("${event.name} is not snake_case", namePattern.matches(event.name))
            assertTrue("${event.name} is longer than 40 characters", event.name.length <= 40)
            reservedPrefixes.forEach { prefix ->
                assertTrue(
                    "${event.name} uses the reserved prefix $prefix",
                    !event.name.startsWith(prefix)
                )
            }
        }
    }

    @Test
    fun `event parameter names fit Firebase's rules`() {
        allEvents.flatMap { it.params.keys }.forEach { key ->
            assertTrue("$key is not snake_case", namePattern.matches(key))
            assertTrue("$key is longer than 40 characters", key.length <= 40)
            reservedPrefixes.forEach { prefix ->
                assertTrue("$key uses the reserved prefix $prefix", !key.startsWith(prefix))
            }
        }
    }

    @Test
    fun `user property names fit the tighter 24-character ceiling`() {
        UserProperty.entries.forEach { property ->
            assertTrue(
                "${property.propertyName} is not snake_case",
                namePattern.matches(property.propertyName)
            )
            assertTrue(
                "${property.propertyName} is longer than 24 characters",
                property.propertyName.length <= 24
            )
        }
    }

    @Test
    fun `every ritual step has a distinct analytics id`() {
        val ids = listOf(
            RitualStep.Question, RitualStep.Breathing, RitualStep.Closing, RitualStep.Log
        ).map { it.analyticsId() }

        assertEquals(listOf("question", "breathing", "closing", "log"), ids)
        assertEquals(ids.size, ids.toSet().size)
    }

    @Test
    fun `a completed ritual carries the mood and the week count`() {
        val event = AnalyticsEvent.RitualCompleted(mood = "SATISFIED", daysClosedThisWeek = 4)

        assertEquals("ritual_completed", event.name)
        assertEquals("SATISFIED", event.params["mood"])
        assertEquals(4, event.params["days_closed_week"])
    }
}
