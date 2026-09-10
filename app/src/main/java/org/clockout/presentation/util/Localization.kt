package org.clockout.presentation.util

import androidx.annotation.StringRes
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.res.stringArrayResource
import androidx.compose.ui.res.stringResource
import org.clockout.R
import org.clockout.domain.model.AppLanguage
import org.clockout.domain.model.DayStatus
import org.clockout.domain.model.LanguageOption
import org.clockout.domain.model.MeditationSoundStyle
import org.clockout.domain.model.MoodTag
import org.clockout.domain.model.SoundChoice
import org.clockout.util.localizedFor

/**
 * Podřídí resources ve stromu jazyku vybranému v aplikaci tím, že přebije LocalResources,
 * ne LocalContext. stringResource()/stringArrayResource()/pluralStringResource() čtou
 * LocalResources.current, který je jinak jen computed-default odvozený z
 * LocalContext.current.resources - stačí přebít přímo tenhle default a LocalContext
 * (i LocalConfiguration jako dřív, kvůli kódu, který si locale čte z konfigurace) nechat
 * beze změny.
 *
 * LocalContext tím zůstává skutečnou Activitou. Na tom stojí composition locals dohledávané
 * procházením řetězu ContextWrapperů (LocalActivity, LocalActivityResultRegistryOwner) i
 * WindowInfoTracker, jehož WM Extensions v1 i Sidecar backend pro detekci posture/fold
 * vyžadují `context is Activity` (ani jeden nerozbaluje ContextWrapper.baseContext).
 * Přepsání LocalContext na createConfigurationContext() (nebo i na ContextWrapper nad ním)
 * by "context is Activity" nadobro rozbilo - proto se tu LocalContext nedotýká vůbec.
 */
@Composable
fun AppLocalization(language: AppLanguage, content: @Composable () -> Unit) {
    val context = LocalContext.current
    val configuration = LocalConfiguration.current
    val localizedResources = remember(context, configuration, language) {
        context.localizedFor(language).resources
    }
    CompositionLocalProvider(
        LocalResources provides localizedResources,
        LocalConfiguration provides localizedResources.configuration,
        content = content
    )
}

@get:StringRes
val MoodTag.labelRes: Int
    get() = when (this) {
        MoodTag.CALM -> R.string.mood_calm_label
        MoodTag.TIRED -> R.string.mood_tired_label
        MoodTag.SATISFIED -> R.string.mood_satisfied_label
        MoodTag.TENSE -> R.string.mood_tense_label
    }

@get:StringRes
val MoodTag.descriptionRes: Int
    get() = when (this) {
        MoodTag.CALM -> R.string.mood_calm_desc
        MoodTag.TIRED -> R.string.mood_tired_desc
        MoodTag.SATISFIED -> R.string.mood_satisfied_desc
        MoodTag.TENSE -> R.string.mood_tense_desc
    }

@get:StringRes
val MeditationSoundStyle.labelRes: Int
    get() = when (this) {
        MeditationSoundStyle.TIBETAN_BOWL -> R.string.sound_tibetan_bowl_label
        MeditationSoundStyle.RAIN_PINK_NOISE -> R.string.sound_rain_pink_noise_label
        MeditationSoundStyle.BREATHING_WAVES -> R.string.sound_breathing_waves_label
        MeditationSoundStyle.SILENT_HAPTIC -> R.string.sound_silent_haptic_label
    }

@get:StringRes
val MeditationSoundStyle.descriptionRes: Int
    get() = when (this) {
        MeditationSoundStyle.TIBETAN_BOWL -> R.string.sound_tibetan_bowl_desc
        MeditationSoundStyle.RAIN_PINK_NOISE -> R.string.sound_rain_pink_noise_desc
        MeditationSoundStyle.BREATHING_WAVES -> R.string.sound_breathing_waves_desc
        MeditationSoundStyle.SILENT_HAPTIC -> R.string.sound_silent_haptic_desc
    }

/** Vlastní zvuk nese jméno souboru, které uživatel zvolil, a to se nepřekládá. */
@Composable
fun SoundChoice.localizedLabel(): String = when (this) {
    is SoundChoice.BuiltIn -> stringResource(style.labelRes)
    is SoundChoice.Custom -> sound.displayName
}

@Composable
fun SoundChoice.localizedDescription(): String = when (this) {
    is SoundChoice.BuiltIn -> stringResource(style.descriptionRes)
    is SoundChoice.Custom -> stringResource(R.string.sound_custom_desc, sound.formattedDuration)
}

/** Zkrácený název dne v týdnu (Po/Mon...) pro pruh dnů na Home - pořadí odpovídá `dayIndex`. */
@Composable
fun DayStatus.localizedDayName(): String = stringArrayResource(R.array.day_names_short)[dayIndex]

/** Endonyma zůstávají v původním jazyce, překládá se jen volba "podle systému". */
@Composable
fun LanguageOption.localizedLabel(): String = when (this) {
    LanguageOption.SYSTEM -> stringResource(R.string.language_option_system)
    LanguageOption.ENGLISH -> AppLanguage.ENGLISH.displayName
    LanguageOption.CZECH -> AppLanguage.CZECH.displayName
}
