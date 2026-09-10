package org.clockout.util

import android.content.Context
import android.content.res.Configuration
import org.clockout.domain.model.AppLanguage
import java.util.Locale

/**
 * Resources pro jazyk vybraný v aplikaci, který se může lišit od systémového locale.
 * Jediné místo, kde se locale override vyrábí - používá ho katalog otázek
 * (QuestionsData) i Compose strom (AppLocalization, přes výsledné .resources).
 *
 * Vrací přímo createConfigurationContext() - žádný z volajících už dnes nepotřebuje nic
 * jiného než výsledné .resources, takže není důvod obalovat vrácený Context do
 * ContextWrapperu jen kvůli zachování řetězu zpět k Activitě. (AppLocalization dřív
 * přebíjela LocalContext tímhle Contextem, což tenhle řetěz rozbíjelo - opraveno tak, že
 * AppLocalization teď přebíjí jen LocalResources/LocalConfiguration a LocalContext
 * nechává být, viz Localization.kt.)
 */
fun Context.localizedFor(language: AppLanguage): Context {
    val configuration = Configuration(resources.configuration).apply {
        setLocale(Locale.forLanguageTag(language.code))
    }
    return createConfigurationContext(configuration)
}
