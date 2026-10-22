package org.clockout

import android.content.Context
import kotlinx.coroutines.flow.Flow
import org.clockout.data.local.CustomSoundStorage
import org.clockout.data.local.QuestionResources
import org.clockout.data.local.RitualLogMigrator
import org.clockout.data.local.RitualPreferences
import org.clockout.data.local.db.ClockoutDatabase
import org.clockout.data.repository.ProgressRepositoryImpl
import org.clockout.data.repository.RitualLogRepositoryImpl
import org.clockout.data.repository.SettingsRepositoryImpl
import org.clockout.data.repository.SoundRepositoryImpl
import org.clockout.domain.model.QuestionCatalog
import org.clockout.domain.platform.ReminderScheduler
import org.clockout.domain.platform.SoundFileStore
import org.clockout.domain.platform.SoundPlayer
import org.clockout.domain.repository.ProgressRepository
import org.clockout.domain.repository.RitualLogRepository
import org.clockout.domain.repository.SettingsRepository
import org.clockout.domain.repository.SoundRepository
import org.clockout.domain.usecase.CompleteRitualUseCase
import org.clockout.domain.usecase.ShouldRequestReviewUseCase
import org.clockout.notification.AlarmReminderScheduler
import org.clockout.sound.AmbientAudioPlayer
import org.clockout.util.todayFlow
import java.time.LocalDate

/**
 * The app's object graph as its callers see it: repositories, use cases and platform ports only.
 * The stores behind them stay inside [DefaultAppContainer], so a screen cannot reach DataStore or
 * Room past the repositories.
 */
interface AppContainer {
    val settings: SettingsRepository
    val sounds: SoundRepository
    val ritualLogs: RitualLogRepository
    val progress: ProgressRepository
    val completeRitual: CompleteRitualUseCase
    val reminderScheduler: ReminderScheduler
    val soundFileStore: SoundFileStore

    /** A fresh player per screen: each view model stops its own in onCleared. */
    fun newSoundPlayer(): SoundPlayer
}

/**
 * Built once per process from the application context. Every Activity, the reminder receiver and
 * the tests take their dependencies from here rather than wiring their own, so DataStore and Room
 * are never handed a locale-overridden context.
 */
class DefaultAppContainer(
    context: Context,
    preferences: RitualPreferences = RitualPreferences(context.applicationContext),
    database: ClockoutDatabase = ClockoutDatabase.getInstance(context.applicationContext),
    todayFlow: Flow<LocalDate> = context.applicationContext.todayFlow(),
    // A cloud backup restores the sound metadata but not the audio files, which travel only on a
    // device-to-device transfer, so what is on disk has the last word.
    soundFileExists: (fileName: String) -> Boolean = { fileName ->
        CustomSoundStorage.resolveFile(context.applicationContext, fileName).exists()
    }
) : AppContainer {
    private val appContext = context.applicationContext

    /**
     * The one migrator: its flag and mutex are per instance. A second instance would still read the
     * persisted flag, but a concurrent import by both could let a late one overwrite an entry written
     * in between, or bring back a blob meant to be dropped (after `clearLogs`, say).
     */
    private val logMigrator = RitualLogMigrator(preferences, database.ritualLogDao(), database.practiceDayDao())

    override val settings: SettingsRepository = SettingsRepositoryImpl(preferences)
    override val sounds: SoundRepository = SoundRepositoryImpl(preferences, soundFileExists)
    override val ritualLogs: RitualLogRepository = RitualLogRepositoryImpl(
        preferences = preferences,
        ritualLogDao = database.ritualLogDao(),
        logMigrator = logMigrator,
        includeWeekends = settings.getIncludeWeekends()
    )
    override val progress: ProgressRepository = ProgressRepositoryImpl(
        practiceDayDao = database.practiceDayDao(),
        logMigrator = logMigrator,
        logs = ritualLogs,
        includeWeekends = settings.getIncludeWeekends(),
        todayFlow = todayFlow
    )

    override val completeRitual = CompleteRitualUseCase(ritualLogs, progress, ShouldRequestReviewUseCase(ritualLogs))

    override val reminderScheduler: ReminderScheduler = AlarmReminderScheduler(appContext)
    override val soundFileStore: SoundFileStore = CustomSoundStorage(appContext)

    override fun newSoundPlayer(): SoundPlayer = AmbientAudioPlayer(appContext)
}

val Context.appContainer: AppContainer
    get() = (applicationContext as ClockoutApplication).container

/**
 * [appContainer], or - where the app is rendered without [ClockoutApplication], as the Compose
 * preview is, whose application context is LayoutLib's own - a container of its own, together with
 * the question catalog the Application would otherwise have installed.
 */
fun Context.appContainerOrStandalone(): AppContainer =
    (applicationContext as? ClockoutApplication)?.container ?: run {
        if (!QuestionCatalog.isInstalled) QuestionCatalog.install(QuestionResources.load(this))
        DefaultAppContainer(this)
    }
