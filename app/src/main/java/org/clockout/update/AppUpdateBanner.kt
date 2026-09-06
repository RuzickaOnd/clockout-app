package org.clockout.update

import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import org.clockout.R

/**
 * Quiet invitation to restart once a flexible update has finished downloading. It never interrupts
 * the ritual: until the update is ready, this draws nothing at all.
 */
@Composable
fun AppUpdateBanner(
    state: AppUpdateState,
    onInstall: () -> Unit,
    modifier: Modifier = Modifier
) {
    val hostState = remember { SnackbarHostState() }
    val message = stringResource(R.string.update_ready)
    val actionLabel = stringResource(R.string.update_restart)

    LaunchedEffect(state) {
        if (state is AppUpdateState.ReadyToInstall) {
            val result = hostState.showSnackbar(
                message = message,
                actionLabel = actionLabel,
                duration = SnackbarDuration.Indefinite
            )
            if (result == SnackbarResult.ActionPerformed) {
                onInstall()
            }
        }
    }

    SnackbarHost(hostState = hostState, modifier = modifier)
}
