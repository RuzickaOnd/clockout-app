package com.example.data.local

import android.content.Context
import android.database.Cursor
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.provider.OpenableColumns
import com.example.domain.model.CustomSound
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.util.UUID

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

/**
 * Copies user picked audio files into the app's private storage so playback no
 * longer depends on the picker's temporary content:// permission grant.
 */
class CustomSoundStorage(context: Context) {

    private val appContext = context.applicationContext

    suspend fun import(uri: Uri, existingCount: Int): SoundImportResult = withContext(Dispatchers.IO) {
        if (existingCount >= MAX_CUSTOM_SOUNDS) {
            return@withContext SoundImportResult.Failure(SoundImportError.LIMIT_REACHED)
        }

        val (displayName, declaredSize) = queryFileMetadata(uri)
        if (declaredSize != null && declaredSize > MAX_FILE_SIZE_BYTES) {
            return@withContext SoundImportResult.Failure(SoundImportError.FILE_TOO_LARGE)
        }
        val extension = displayName.substringAfterLast('.', "").lowercase()
            .takeIf { it.isNotEmpty() && it.length <= 5 }
            ?: uri.toString().substringAfterLast('.', "").lowercase().takeIf { it in SUPPORTED_EXTENSIONS }
            ?: "audio"

        val directory = directory(appContext).also { it.mkdirs() }
        val fileName = "${UUID.randomUUID()}.$extension"
        val target = File(directory, fileName)

        val copiedBytes = try {
            appContext.contentResolver.openInputStream(uri)?.use { input ->
                target.outputStream().use { output ->
                    input.copyTo(output, DEFAULT_BUFFER_SIZE)
                }
            } ?: run {
                target.delete()
                return@withContext SoundImportResult.Failure(SoundImportError.READ_FAILED)
            }
        } catch (_: Exception) {
            target.delete()
            return@withContext SoundImportResult.Failure(SoundImportError.READ_FAILED)
        }

        if (copiedBytes <= 0L) {
            target.delete()
            return@withContext SoundImportResult.Failure(SoundImportError.UNSUPPORTED_FILE)
        }
        if (copiedBytes > MAX_FILE_SIZE_BYTES) {
            target.delete()
            return@withContext SoundImportResult.Failure(SoundImportError.FILE_TOO_LARGE)
        }

        val durationMs = readAudioDuration(target)
        if (durationMs == null) {
            target.delete()
            return@withContext SoundImportResult.Failure(SoundImportError.UNSUPPORTED_FILE)
        }

        SoundImportResult.Success(
            CustomSound(
                id = CustomSound.ID_PREFIX + UUID.randomUUID().toString(),
                displayName = displayName.substringBeforeLast('.').ifBlank { displayName }.take(60),
                fileName = fileName,
                durationMs = durationMs,
                addedAt = System.currentTimeMillis()
            )
        )
    }

    suspend fun delete(sound: CustomSound): Boolean = withContext(Dispatchers.IO) {
        val file = resolveFile(appContext, sound.fileName)
        !file.exists() || file.delete()
    }

    /** Returns the picker reported display name and size (size is null when unknown). */
    private fun queryFileMetadata(uri: Uri): Pair<String, Long?> {
        var name: String? = null
        var size: Long? = null
        var cursor: Cursor? = null
        try {
            cursor = appContext.contentResolver.query(
                uri,
                arrayOf(OpenableColumns.DISPLAY_NAME, OpenableColumns.SIZE),
                null,
                null,
                null
            )
            if (cursor != null && cursor.moveToFirst()) {
                val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                if (nameIndex >= 0 && !cursor.isNull(nameIndex)) {
                    name = cursor.getString(nameIndex)?.takeIf { it.isNotBlank() }
                }
                val sizeIndex = cursor.getColumnIndex(OpenableColumns.SIZE)
                if (sizeIndex >= 0 && !cursor.isNull(sizeIndex)) {
                    size = cursor.getLong(sizeIndex).takeIf { it > 0L }
                }
            }
        } catch (_: Exception) {
            // fall through to the uri based fallback
        } finally {
            cursor?.close()
        }
        val fallbackName = uri.lastPathSegment?.substringAfterLast('/')?.takeIf { it.isNotBlank() } ?: "audio"
        return (name ?: fallbackName) to size
    }

    /** Returns the track duration, or null when the file holds no decodable audio. */
    private fun readAudioDuration(file: File): Long? {
        val retriever = MediaMetadataRetriever()
        return try {
            retriever.setDataSource(file.absolutePath)
            val hasAudio = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_HAS_AUDIO)
            val duration = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)?.toLongOrNull()
            if (hasAudio != "yes" || duration == null || duration <= 0L) null else duration
        } catch (_: Exception) {
            null
        } finally {
            try {
                retriever.release()
            } catch (_: Exception) {
            }
        }
    }

    companion object {
        const val MAX_CUSTOM_SOUNDS = 10
        const val MAX_FILE_SIZE_BYTES = 25L * 1024 * 1024

        private const val DIRECTORY_NAME = "custom_sounds"
        private val SUPPORTED_EXTENSIONS = setOf("mp3", "wav", "ogg", "m4a", "aac", "flac", "opus", "mp4")

        fun directory(context: Context): File = File(context.filesDir, DIRECTORY_NAME)

        fun resolveFile(context: Context, fileName: String): File = File(directory(context), fileName)
    }
}
