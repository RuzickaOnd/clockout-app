package org.clockout

import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertSame
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class AppContainerTest {

    @Test
    fun `the application hands out one container per process`() {
        val app = ApplicationProvider.getApplicationContext<ClockoutApplication>()

        assertSame(app.container, app.container)
        assertSame(app.container.settings, app.container.settings)
    }

    @Test
    fun `an activity context reaches the same container as the application`() {
        val app = ApplicationProvider.getApplicationContext<ClockoutApplication>()
        val activity = Robolectric.buildActivity(PathActivity::class.java).create().get()

        assertSame(app.container, activity.appContainer)
    }
}
