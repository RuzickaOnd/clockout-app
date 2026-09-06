package org.clockout.update

import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.IntentSenderRequest
import com.google.android.play.core.appupdate.AppUpdateManager
import com.google.android.play.core.appupdate.AppUpdateOptions
import com.google.android.play.core.install.InstallStateUpdatedListener
import com.google.android.play.core.install.model.AppUpdateType
import com.google.android.play.core.install.model.InstallStatus
import com.google.android.play.core.install.model.UpdateAvailability
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/** Where the flexible Play update currently stands, as far as the UI needs to care. */
sealed interface AppUpdateState {
    data object Idle : AppUpdateState
    data object ReadyToInstall : AppUpdateState
}

/**
 * Drives the flexible in-app update flow: the new version downloads in the background while the
 * ritual carries on, and only once it is ready does the UI offer a restart.
 *
 * Every Play call is best effort — a build that did not come from the Play Store simply stays
 * [AppUpdateState.Idle] instead of bothering the user.
 */
class AppUpdateController(private val updateManager: AppUpdateManager) {

    private val _state = MutableStateFlow<AppUpdateState>(AppUpdateState.Idle)
    val state: StateFlow<AppUpdateState> = _state.asStateFlow()

    private val installListener = InstallStateUpdatedListener { installState ->
        _state.value = when (installState.installStatus()) {
            InstallStatus.DOWNLOADED -> AppUpdateState.ReadyToInstall
            else -> AppUpdateState.Idle
        }
    }

    init {
        updateManager.registerListener(installListener)
    }

    /** Asks Play whether a flexible update is waiting and, if so, starts the download flow. */
    fun checkForUpdate(launcher: ActivityResultLauncher<IntentSenderRequest>) {
        updateManager.appUpdateInfo
            .addOnSuccessListener { info ->
                val available = info.updateAvailability() == UpdateAvailability.UPDATE_AVAILABLE
                if (available && info.isUpdateTypeAllowed(AppUpdateType.FLEXIBLE)) {
                    updateManager.startUpdateFlowForResult(
                        info,
                        launcher,
                        AppUpdateOptions.newBuilder(AppUpdateType.FLEXIBLE).build()
                    )
                }
            }
            .addOnFailureListener { _state.value = AppUpdateState.Idle }
    }

    /** Catches the case where the download finished while the app was in the background. */
    fun resumeIfDownloaded() {
        updateManager.appUpdateInfo
            .addOnSuccessListener { info ->
                if (info.installStatus() == InstallStatus.DOWNLOADED) {
                    _state.value = AppUpdateState.ReadyToInstall
                }
            }
            .addOnFailureListener { _state.value = AppUpdateState.Idle }
    }

    /** Restarts the app into the downloaded version. */
    fun completeUpdate() {
        updateManager.completeUpdate()
    }

    fun unregister() {
        updateManager.unregisterListener(installListener)
    }
}
