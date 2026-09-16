package org.clockout.presentation

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import org.clockout.data.local.CustomSoundStorage
import org.clockout.data.local.RitualPreferences
import org.clockout.data.local.db.ClockoutDatabase
import org.clockout.data.repository.RitualRepository
import org.clockout.data.repository.RitualRepositoryImpl

/**
 * Builds the repository the app's screens read and write through.
 *
 * Call this **above** [org.clockout.presentation.util.AppLocalization], so DataStore and Room are
 * handed the real context rather than one whose configuration has been overridden to localize
 * strings. Both [org.clockout.presentation.PadlaApp] and
 * [org.clockout.BreathingStyleActivity] enter the app at their own root and would otherwise each
 * repeat this wiring; the DataStore and the Room instance behind it are per-process singletons, so
 * two callers share the same storage either way.
 */
@Composable
fun rememberRitualRepository(context: Context = LocalContext.current): RitualRepository {
    val preferences = remember(context) { RitualPreferences(context) }
    return remember(context) {
        RitualRepositoryImpl(
            preferences = preferences,
            ritualLogDao = ClockoutDatabase.getInstance(context).ritualLogDao(),
            // A cloud backup restores the sound metadata but not the audio files, which travel
            // only on a device-to-device transfer, so what is on disk has the last word.
            soundFileExists = { fileName -> CustomSoundStorage.resolveFile(context, fileName).exists() }
        )
    }
}
