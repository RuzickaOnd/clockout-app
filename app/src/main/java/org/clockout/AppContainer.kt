package org.clockout

import android.content.Context
import kotlinx.coroutines.flow.Flow
import org.clockout.data.local.CustomSoundStorage
import org.clockout.data.local.RitualLogMigrator
import org.clockout.data.local.RitualPreferences
import org.clockout.data.local.db.ClockoutDatabase
import org.clockout.data.repository.ProgressRepositoryImpl
import org.clockout.data.repository.RitualLogRepositoryImpl
import org.clockout.data.repository.SettingsRepositoryImpl
import org.clockout.data.repository.SoundRepositoryImpl
import org.clockout.domain.platform.ReminderScheduler
import org.clockout.domain.platform.SoundFileStore
import org.clockout.domain.platform.SoundPlayer
import org.clockout.domain.repository.ProgressRepository
import org.clockout.domain.repository.RitualLogRepository
import org.clockout.domain.repository.SettingsRepository
import org.clockout.domain.repository.SoundRepository
import org.clockout.notification.AlarmReminderScheduler
import org.clockout.sound.AmbientAudioPlayer
import org.clockout.util.todayFlow
import java.time.LocalDate

/**
 * The app's object graph, built once per process from the application context. Every Activity,
 * the reminder receiver and the tests take their dependencies from here rather than wiring their
 * own, so DataStore and Room are never handed a locale-overridden context.
 */
class AppContainer(
    context: Context,
    val preferences: RitualPreferences = RitualPreferences(context.applicationContext),
    val database: ClockoutDatabase = ClockoutDatabase.getInstance(context.applicationContext),
    todayFlow: Flow<LocalDate> = context.applicationContext.todayFlow(),
    // A cloud backup restores the sound metadata but not the audio files, which travel only on a
    // device-to-device transfer, so what is on disk has the last word.
    soundFileExists: (fileName: String) -> Boolean = { fileName ->
        CustomSoundStorage.resolveFile(context.applicationContext, fileName).exists()
    }
) {
    private val appContext = context.applicationContext

    /** The one migrator: its flag and mutex are per instance, so a second would import the legacy blob twice. */
    val logMigrator = RitualLogMigrator(preferences, database.ritualLogDao(), database.practiceDayDao())

    val settings: SettingsRepository = SettingsRepositoryImpl(preferences)
    val sounds: SoundRepository = SoundRepositoryImpl(preferences, soundFileExists)
    val ritualLogs: RitualLogRepository =
        RitualLogRepositoryImpl(preferences, database.ritualLogDao(), logMigrator)
    val progress: ProgressRepository = ProgressRepositoryImpl(
        practiceDayDao = database.practiceDayDao(),
        logMigrator = logMigrator,
        logs = ritualLogs,
        includeWeekends = settings.getIncludeWeekends(),
        todayFlow = todayFlow
    )

    val reminderScheduler: ReminderScheduler = AlarmReminderScheduler(appContext)
    val soundFileStore: SoundFileStore = CustomSoundStorage(appContext)

    /** A fresh player per screen: each view model stops its own in onCleared. */
    fun newSoundPlayer(): SoundPlayer = AmbientAudioPlayer(appContext)
}

val Context.appContainer: AppContainer
    get() = (applicationContext as ClockoutApplication).container
