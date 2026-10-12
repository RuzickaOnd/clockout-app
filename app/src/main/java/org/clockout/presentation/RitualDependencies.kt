package org.clockout.presentation

import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import org.clockout.AppContainer
import org.clockout.appContainer

/** The process-wide [AppContainer], reachable from any composable whatever its localization. */
@Composable
fun appContainer(): AppContainer = LocalContext.current.appContainer
