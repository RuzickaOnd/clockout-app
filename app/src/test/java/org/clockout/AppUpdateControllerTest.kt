package org.clockout

import android.content.Context
import android.os.Looper
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContract
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.app.ActivityOptionsCompat
import androidx.test.core.app.ApplicationProvider
import com.google.android.gms.tasks.Task
import com.google.android.gms.tasks.Tasks
import com.google.android.play.core.appupdate.AppUpdateInfo
import com.google.android.play.core.appupdate.testing.FakeAppUpdateManager
import org.clockout.update.AppUpdateController
import org.clockout.update.AppUpdateState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class AppUpdateControllerTest {

    private lateinit var context: Context
    private lateinit var playStore: FakeAppUpdateManager

    /** Stands in for the launcher the activity registers; Play only needs somewhere to send the flow. */
    private class NoOpLauncher : ActivityResultLauncher<IntentSenderRequest>() {
        override fun launch(input: IntentSenderRequest, options: ActivityOptionsCompat?) = Unit
        override fun unregister() = Unit
        override val contract: ActivityResultContract<IntentSenderRequest, *> =
            ActivityResultContracts.StartIntentSenderForResult()
    }

    private val launcher = NoOpLauncher()

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        playStore = FakeAppUpdateManager(context)
    }

    private fun settle() = shadowOf(Looper.getMainLooper()).idle()

    @Test
    fun `no update available leaves the state idle`() {
        playStore.setUpdateNotAvailable()
        val controller = AppUpdateController(playStore)

        controller.checkForUpdate(launcher)
        settle()

        assertEquals(AppUpdateState.Idle, controller.state.value)
        assertFalse(playStore.isConfirmationDialogVisible)
    }

    @Test
    fun `an available update asks play to show the update flow`() {
        playStore.setUpdateAvailable(2)
        val controller = AppUpdateController(playStore)

        controller.checkForUpdate(launcher)
        settle()

        assertTrue(playStore.isConfirmationDialogVisible)
    }

    @Test
    fun `a finished download makes the update ready to install`() {
        playStore.setUpdateAvailable(2)
        val controller = AppUpdateController(playStore)
        controller.checkForUpdate(launcher)
        settle()

        playStore.userAcceptsUpdate()
        playStore.downloadStarts()
        playStore.downloadCompletes()
        settle()

        assertEquals(AppUpdateState.ReadyToInstall, controller.state.value)
    }

    @Test
    fun `installing the downloaded update hands it back to play`() {
        playStore.setUpdateAvailable(2)
        val controller = AppUpdateController(playStore)
        controller.checkForUpdate(launcher)
        settle()
        playStore.userAcceptsUpdate()
        playStore.downloadStarts()
        playStore.downloadCompletes()
        settle()

        controller.completeUpdate()
        settle()

        assertTrue(playStore.isInstallSplashScreenVisible)
    }

    @Test
    fun `an update check that fails leaves the state idle`() {
        val unavailablePlayStore = object : FakeAppUpdateManager(context) {
            override fun getAppUpdateInfo(): Task<AppUpdateInfo> =
                Tasks.forException(IllegalStateException("app not installed from Play"))
        }
        val controller = AppUpdateController(unavailablePlayStore)

        controller.checkForUpdate(launcher)
        settle()

        assertEquals(AppUpdateState.Idle, controller.state.value)
    }

    @Test
    fun `returning to an already downloaded update makes it ready to install`() {
        playStore.setUpdateAvailable(2)
        val controller = AppUpdateController(playStore)
        controller.checkForUpdate(launcher)
        settle()
        playStore.userAcceptsUpdate()
        playStore.downloadStarts()
        playStore.downloadCompletes()

        val returningController = AppUpdateController(playStore)
        returningController.resumeIfDownloaded()
        settle()

        assertEquals(AppUpdateState.ReadyToInstall, returningController.state.value)
    }
}
