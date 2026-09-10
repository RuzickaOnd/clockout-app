package org.clockout

import androidx.test.core.app.ApplicationProvider
import android.content.Context
import org.clockout.domain.model.AppLanguage
import org.clockout.util.localizedFor
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Notifikace vzniká mimo Compose, takže si jazyk musí vyžádat sama.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class NotificationTextTest {

    private val context: Context = ApplicationProvider.getApplicationContext()

    @Test
    fun `the reminder speaks czech when the app is czech`() {
        val resources = context.localizedFor(AppLanguage.CZECH).resources

        assertEquals(
            "Padla – čas ukončit práci",
            resources.getString(R.string.notification_workday_end_title)
        )
    }

    @Test
    fun `the reminder speaks english when the app is english`() {
        val resources = context.localizedFor(AppLanguage.ENGLISH).resources

        assertEquals(
            "ClockOut – time to wrap up work",
            resources.getString(R.string.notification_workday_end_title)
        )
    }
}
