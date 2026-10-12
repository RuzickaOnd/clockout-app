package org.clockout

import android.app.Application
import org.clockout.data.local.QuestionsData

/**
 * The ritual question catalog is read out of the string resources, and both the UI and the
 * notification receiver reach it through a singleton, so it is loaded here once per process rather
 * than by whichever entry point happens to run first.
 */
class ClockoutApplication : Application() {

    /** Built on first use, so a process started only for the reminder receiver pays for it too. */
    val container: AppContainer by lazy { AppContainer(this) }

    override fun onCreate() {
        super.onCreate()
        QuestionsData.initialize(this)
    }
}
