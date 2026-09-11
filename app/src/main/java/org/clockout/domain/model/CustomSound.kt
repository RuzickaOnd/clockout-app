package org.clockout.domain.model

/**
 * A user supplied audio file that was imported into the app's private storage
 * and can be used as the ambient meditation sound.
 *
 * [isAvailable] is not persisted: it is resolved against the file system every time the
 * metadata is read. A cloud backup restores the metadata but not the audio file, so a
 * restored entry describes a sound whose file is no longer there.
 */
data class CustomSound(
    val id: String,
    val displayName: String,
    val fileName: String,
    val durationMs: Long,
    val addedAt: Long,
    val isAvailable: Boolean = true
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
