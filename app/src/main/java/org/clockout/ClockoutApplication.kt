package org.clockout

import android.app.Application
import org.clockout.data.local.QuestionResources
import org.clockout.domain.model.QuestionCatalog

/**
 * The ritual question catalog is read out of the string resources by `QuestionResources` and
 * installed into the domain's `QuestionCatalog`. Both the UI and the notification receiver reach it
 * through that singleton, so it is installed here once per process rather than by whichever entry
 * point happens to run first.
 */
class ClockoutApplication : Application() {

    /** Built on first use, by whichever entry point — an Activity or the reminder receiver — gets there first. */
    val container: AppContainer by lazy { AppContainer(this) }

    override fun onCreate() {
        super.onCreate()
        QuestionCatalog.install(QuestionResources.load(this))
    }
}
