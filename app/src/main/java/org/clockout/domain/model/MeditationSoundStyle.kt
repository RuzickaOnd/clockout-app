package org.clockout.domain.model

enum class MeditationSoundStyle(val id: String) {
    TIBETAN_BOWL("tibetan_bowl"),
    RAIN_PINK_NOISE("rain_pink_noise"),
    BREATHING_WAVES("breathing_waves"),
    SILENT_HAPTIC("silent_haptic");

    companion object {
        fun fromId(id: String?): MeditationSoundStyle {
            return entries.firstOrNull { it.id == id } ?: TIBETAN_BOWL
        }
    }
}
