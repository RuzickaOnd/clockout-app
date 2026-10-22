package org.clockout.util

import android.content.Context
import android.content.res.Configuration
import org.clockout.domain.model.AppLanguage
import java.util.Locale

/**
 * Resources for the language chosen inside the app, which may differ from the system locale.
 * The single place the locale override is built - used by the question catalog (QuestionResources)
 * and by the Compose tree (AppLocalization, through the resulting .resources).
 *
 * Returns createConfigurationContext() directly: no caller needs anything but the resulting
 * .resources, so there is no reason to wrap it in a ContextWrapper merely to keep the chain
 * back to the Activity. AppLocalization used to override LocalContext with this Context, which
 * broke that chain; it now overrides only LocalResources/LocalConfiguration and leaves
 * LocalContext alone - see Localization.kt.
 */
fun Context.localizedFor(language: AppLanguage): Context {
    val configuration = Configuration(resources.configuration).apply {
        setLocale(Locale.forLanguageTag(language.code))
    }
    return createConfigurationContext(configuration)
}
