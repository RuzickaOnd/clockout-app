package org.clockout.util

import android.content.Context
import android.content.res.Configuration
import org.clockout.domain.model.AppLanguage
import java.util.Locale

/**
 * Resources pro jazyk vybraný v aplikaci, který se může lišit od systémového locale.
 * Jediné místo, kde se locale override vyrábí - používá ho jak Compose strom, tak
 * katalog otázek a notifikace.
 */
fun Context.localizedFor(language: AppLanguage): Context {
    val configuration = Configuration(resources.configuration).apply {
        setLocale(Locale.forLanguageTag(language.code))
    }
    return createConfigurationContext(configuration)
}
