package org.clockout.util

import android.content.Context
import android.content.ContextWrapper
import android.content.res.AssetManager
import android.content.res.Configuration
import android.content.res.Resources
import org.clockout.domain.model.AppLanguage
import java.util.Locale

/**
 * Resources pro jazyk vybraný v aplikaci, který se může lišit od systémového locale.
 * Jediné místo, kde se locale override vyrábí - používá ho jak Compose strom, tak
 * katalog otázek a notifikace.
 *
 * Vrací ContextWrapper nad původním contextem, ne přímo createConfigurationContext():
 * ten vrací nový Context, který přes getBaseContext() zpět k Activitě nevede, takže by
 * rozbil cokoliv, co si Activitu (nebo ActivityResultRegistryOwner apod.) dohledává
 * procházením řetězu ContextWrapperů - přesně to dělá findOwner() v activity-compose,
 * na kterém stojí LocalActivity i LocalActivityResultRegistryOwner. Wrapper naproti
 * tomu getBaseContext() zdědí a jen přebije getResources()/getAssets(), takže Activita
 * zůstane dohledatelná a resources se přitom čtou v požadovaném jazyce.
 */
fun Context.localizedFor(language: AppLanguage): Context {
    val configuration = Configuration(resources.configuration).apply {
        setLocale(Locale.forLanguageTag(language.code))
    }
    val localizedResources = createConfigurationContext(configuration).resources
    return object : ContextWrapper(this) {
        override fun getResources(): Resources = localizedResources
        override fun getAssets(): AssetManager = localizedResources.assets
    }
}
