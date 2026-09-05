package org.clockout.domain.model

/**
 * A user supplied audio file that was imported into the app's private storage
 * and can be used as the ambient meditation sound.
 */
data class CustomSound(
    val id: String,
    val displayName: String,
    val fileName: String,
    val durationMs: Long,
    val addedAt: Long
) {
    val formattedDuration: String
        get() {
            val totalSeconds = (durationMs / 1000).coerceAtLeast(0)
            return String.format("%d:%02d", totalSeconds / 60, totalSeconds % 60)
        }

    companion object {
        const val ID_PREFIX = "custom:"

        fun isCustomId(id: String?): Boolean = id?.startsWith(ID_PREFIX) == true
    }
}
