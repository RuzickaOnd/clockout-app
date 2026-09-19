package org.clockout.presentation

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import com.google.firebase.FirebaseApp
import org.clockout.domain.telemetry.AnalyticsTracker
import org.clockout.domain.telemetry.ConsentGatedTracker
import org.clockout.domain.telemetry.NoOpAnalyticsTracker
import org.clockout.telemetry.FirebaseAnalyticsTracker

/**
 * The gated tracker the app reports through.
 *
 * Built from the application context, so it outlives an Activity recreation, and above
 * [org.clockout.presentation.util.AppLocalization] for the same reason [rememberRitualRepository]
 * is: neither has any use for a context whose configuration has been overridden to localize
 * strings.
 *
 * Falls back to [NoOpAnalyticsTracker] when no [FirebaseApp] has been initialized. `FirebaseCrashlytics.getInstance()`
 * throws in that case rather than returning a dormant instance, and `FirebaseInitProvider` does not
 * run under Robolectric - so without this fallback every test that renders the real app would die
 * on a reporting concern it has nothing to do with. Reporting nothing is also the honest outcome
 * for a build assembled without `google-services.json`.
 */
@Composable
fun rememberAnalyticsTracker(context: Context = LocalContext.current): AnalyticsTracker {
    val appContext = context.applicationContext
    return remember(appContext) {
        if (FirebaseApp.getApps(appContext).isEmpty()) {
            NoOpAnalyticsTracker
        } else {
            ConsentGatedTracker(FirebaseAnalyticsTracker(appContext))
        }
    }
}
