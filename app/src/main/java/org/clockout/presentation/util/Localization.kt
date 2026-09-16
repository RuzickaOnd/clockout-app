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
import org.clockout.domain.model.BreathingStyle
import org.clockout.domain.model.MeditationSoundStyle
import org.clockout.domain.model.MoodTag
import org.clockout.domain.model.SoundChoice
import org.clockout.util.localizedFor

/**
 * Subordinates the resources in the tree to the language chosen inside the app by overriding
 * LocalResources rather than LocalContext. stringResource(), stringArrayResource() and
 * pluralStringResource() all read LocalResources.current, which is otherwise just a computed
 * default derived from LocalContext.current.resources - overriding that default directly is
 * enough, and LocalContext (plus LocalConfiguration, for code that reads the locale off the
 * configuration) can be left untouched.
 *
 * LocalContext therefore stays the real Activity. Composition locals resolved by walking the
 * ContextWrapper chain depend on that (LocalActivity, LocalActivityResultRegistryOwner), and so
 * does WindowInfoTracker: both its WM Extensions v1 and Sidecar backends require
 * `context is Activity` for posture/fold detection, and neither unwraps ContextWrapper.baseContext.
 * Overriding LocalContext with createConfigurationContext() - or even with a ContextWrapper around
 * it - breaks `context is Activity` for good, which is why LocalContext is not touched here.
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
        MoodTag.SATISFIED -> R.string.mood_satisfied_label
        MoodTag.TIRED -> R.string.mood_tired_label
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

@get:StringRes
val BreathingStyle.labelRes: Int
    get() = when (this) {
        BreathingStyle.CLASSIC_CIRCLE -> R.string.breathing_style_classic_circle_label
        BreathingStyle.AURORA -> R.string.breathing_style_aurora_label
        BreathingStyle.CONCENTRIC_RIPPLE -> R.string.breathing_style_concentric_ripple_label
    }

@get:StringRes
val BreathingStyle.descriptionRes: Int
    get() = when (this) {
        BreathingStyle.CLASSIC_CIRCLE -> R.string.breathing_style_classic_circle_desc
        BreathingStyle.AURORA -> R.string.breathing_style_aurora_desc
        BreathingStyle.CONCENTRIC_RIPPLE -> R.string.breathing_style_concentric_ripple_desc
    }

/** A custom sound carries the file name the user picked, which is never translated. */
@Composable
fun SoundChoice.localizedLabel(): String = when (this) {
    is SoundChoice.BuiltIn -> stringResource(style.labelRes)
    is SoundChoice.Custom -> sound.displayName
}

@Composable
fun SoundChoice.localizedDescription(): String = when (this) {
    is SoundChoice.BuiltIn -> stringResource(style.descriptionRes)
    is SoundChoice.Custom -> if (sound.isAvailable) {
        stringResource(R.string.sound_custom_desc, sound.formattedDuration)
    } else {
        // A restored cloud backup brings the metadata back without the audio file
        stringResource(R.string.settings_custom_sound_missing_desc)
    }
}

/** Short weekday name (Po/Mon...) for the Home week strip - the order matches `dayIndex`. */
@Composable
fun DayStatus.localizedDayName(): String = stringArrayResource(R.array.day_names_short)[dayIndex]

/** Endonyms stay in their own language; only the "system default" option is translated. */
@Composable
fun LanguageOption.localizedLabel(): String = when (this) {
    LanguageOption.SYSTEM -> stringResource(R.string.language_option_system)
    LanguageOption.ENGLISH -> AppLanguage.ENGLISH.displayName
    LanguageOption.CZECH -> AppLanguage.CZECH.displayName
}
