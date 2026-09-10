package org.clockout.presentation.util

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import org.clockout.domain.model.AppLanguage
import org.clockout.util.localizedFor

/**
 * Podřídí resources ve stromu jazyku vybranému v aplikaci. stringResource() čte z
 * LocalResources, který je odvozený od LocalContext.resources, takže override Contextu
 * stačí; LocalConfiguration se poskytuje kvůli kódu, který si locale čte z konfigurace.
 */
@Composable
fun AppLocalization(language: AppLanguage, content: @Composable () -> Unit) {
    val context = LocalContext.current
    val localized = remember(context, language) { context.localizedFor(language) }
    CompositionLocalProvider(
        LocalContext provides localized,
        LocalConfiguration provides localized.resources.configuration,
        content = content
    )
}
