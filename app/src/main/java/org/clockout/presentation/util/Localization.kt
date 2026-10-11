package org.clockout.presentation.util

import androidx.annotation.DrawableRes
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
import org.clockout.domain.model.PathStage
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
        BreathingStyle.LOTUS -> R.string.breathing_style_lotus_label
    }

@get:StringRes
val BreathingStyle.descriptionRes: Int
    get() = when (this) {
        BreathingStyle.CLASSIC_CIRCLE -> R.string.breathing_style_classic_circle_desc
        BreathingStyle.AURORA -> R.string.breathing_style_aurora_desc
        BreathingStyle.CONCENTRIC_RIPPLE -> R.string.breathing_style_concentric_ripple_desc
        BreathingStyle.LOTUS -> R.string.breathing_style_lotus_desc
    }

@get:StringRes
val PathStage.titleRes: Int
    get() = when (this) {
        PathStage.AT_DESK -> R.string.path_stage_0_title
        PathStage.LAPTOP_CLOSED -> R.string.path_stage_1_title
        PathStage.COAT_ON -> R.string.path_stage_2_title
        PathStage.KEY_IN_LOCK -> R.string.path_stage_3_title
        PathStage.DOWN_THE_STAIRS -> R.string.path_stage_4_title
        PathStage.STREET_AT_DUSK -> R.string.path_stage_5_title
        PathStage.THROUGH_THE_PARK -> R.string.path_stage_6_title
        PathStage.OVER_THE_BRIDGE -> R.string.path_stage_7_title
        PathStage.GARDEN_GATE -> R.string.path_stage_8_title
        PathStage.THRESHOLD -> R.string.path_stage_9_title
        PathStage.HOME -> R.string.path_stage_10_title
    }

@get:StringRes
val PathStage.verseRes: Int
    get() = when (this) {
        PathStage.AT_DESK -> R.string.path_stage_0_verse
        PathStage.LAPTOP_CLOSED -> R.string.path_stage_1_verse
        PathStage.COAT_ON -> R.string.path_stage_2_verse
        PathStage.KEY_IN_LOCK -> R.string.path_stage_3_verse
        PathStage.DOWN_THE_STAIRS -> R.string.path_stage_4_verse
        PathStage.STREET_AT_DUSK -> R.string.path_stage_5_verse
        PathStage.THROUGH_THE_PARK -> R.string.path_stage_6_verse
        PathStage.OVER_THE_BRIDGE -> R.string.path_stage_7_verse
        PathStage.GARDEN_GATE -> R.string.path_stage_8_verse
        PathStage.THRESHOLD -> R.string.path_stage_9_verse
        PathStage.HOME -> R.string.path_stage_10_verse
    }

@get:StringRes
val PathStage.descriptionRes: Int
    get() = when (this) {
        PathStage.AT_DESK -> R.string.path_stage_0_desc
        PathStage.LAPTOP_CLOSED -> R.string.path_stage_1_desc
        PathStage.COAT_ON -> R.string.path_stage_2_desc
        PathStage.KEY_IN_LOCK -> R.string.path_stage_3_desc
        PathStage.DOWN_THE_STAIRS -> R.string.path_stage_4_desc
        PathStage.STREET_AT_DUSK -> R.string.path_stage_5_desc
        PathStage.THROUGH_THE_PARK -> R.string.path_stage_6_desc
        PathStage.OVER_THE_BRIDGE -> R.string.path_stage_7_desc
        PathStage.GARDEN_GATE -> R.string.path_stage_8_desc
        PathStage.THRESHOLD -> R.string.path_stage_9_desc
        PathStage.HOME -> R.string.path_stage_10_desc
    }

@get:DrawableRes
val PathStage.iconRes: Int
    get() = when (this) {
        PathStage.AT_DESK -> R.drawable.ic_path_stage_0
        PathStage.LAPTOP_CLOSED -> R.drawable.ic_path_stage_1
        PathStage.COAT_ON -> R.drawable.ic_path_stage_2
        PathStage.KEY_IN_LOCK -> R.drawable.ic_path_stage_3
        PathStage.DOWN_THE_STAIRS -> R.drawable.ic_path_stage_4
        PathStage.STREET_AT_DUSK -> R.drawable.ic_path_stage_5
        PathStage.THROUGH_THE_PARK -> R.drawable.ic_path_stage_6
        PathStage.OVER_THE_BRIDGE -> R.drawable.ic_path_stage_7
        PathStage.GARDEN_GATE -> R.drawable.ic_path_stage_8
        PathStage.THRESHOLD -> R.drawable.ic_path_stage_9
        PathStage.HOME -> R.drawable.ic_path_stage_10
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
