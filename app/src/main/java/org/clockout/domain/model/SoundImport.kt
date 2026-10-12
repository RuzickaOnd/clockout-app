package org.clockout.domain.model

/** Reason an imported file could not be used. */
enum class SoundImportError {
    UNSUPPORTED_FILE,
    FILE_TOO_LARGE,
    LIMIT_REACHED,
    READ_FAILED
}

sealed interface SoundImportResult {
    data class Success(val sound: CustomSound) : SoundImportResult
    data class Failure(val error: SoundImportError) : SoundImportResult
}
