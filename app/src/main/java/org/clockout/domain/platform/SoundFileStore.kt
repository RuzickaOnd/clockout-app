package org.clockout.domain.platform

import org.clockout.domain.model.CustomSound
import org.clockout.domain.model.SoundImportResult

/**
 * Keeps user-picked audio in app storage. [source] is the picked document's URI as a string, so
 * the domain stays free of android.net.Uri.
 */
interface SoundFileStore {
    suspend fun import(source: String, existingCount: Int): SoundImportResult
    suspend fun delete(sound: CustomSound)
}
