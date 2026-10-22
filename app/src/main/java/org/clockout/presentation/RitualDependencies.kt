package org.clockout.presentation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import org.clockout.AppContainer
import org.clockout.appContainerOrStandalone

/** The process-wide [AppContainer], reachable from any composable whatever its localization. */
@Composable
fun appContainer(): AppContainer {
    val context = LocalContext.current
    return remember(context) { context.appContainerOrStandalone() }
}
