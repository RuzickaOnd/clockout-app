package org.clockout.telemetry

import android.content.Context
import android.os.Bundle
import com.google.firebase.analytics.FirebaseAnalytics
import com.google.firebase.crashlytics.FirebaseCrashlytics
import org.clockout.domain.telemetry.AnalyticsEvent
import org.clockout.domain.telemetry.AnalyticsTracker
import org.clockout.domain.telemetry.UserProperty

/**
 * The only class in the app that knows Firebase exists.
 *
 * Collection starts disabled in the manifest, so this switch is the only thing that ever turns it
 * on. Both SDKs remember the value across launches once set, which is why consent is written to
 * DataStore as well rather than relied on from the SDKs.
 */
class FirebaseAnalyticsTracker(context: Context) : AnalyticsTracker {

    private val analytics: FirebaseAnalytics = FirebaseAnalytics.getInstance(context)
    private val crashlytics: FirebaseCrashlytics = FirebaseCrashlytics.getInstance()

    override fun setCollectionEnabled(enabled: Boolean) {
        analytics.setAnalyticsCollectionEnabled(enabled)
        crashlytics.isCrashlyticsCollectionEnabled = enabled
    }

    override fun track(event: AnalyticsEvent) {
        val bundle = Bundle().apply {
            event.params.forEach { (key, value) ->
                when (value) {
                    is Int -> putLong(key, value.toLong())
                    is Long -> putLong(key, value)
                    is Double -> putDouble(key, value)
                    else -> putString(key, value.toString())
                }
            }
        }
        analytics.logEvent(event.name, bundle)
    }

    override fun setUserProperty(property: UserProperty, value: String) {
        analytics.setUserProperty(property.propertyName, value)
    }
}
