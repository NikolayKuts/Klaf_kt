package com.kuts.klaf.vocabularySource

import android.content.Intent
import android.provider.OpenableColumns
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

private val AUDIO_MIME_TYPES = arrayOf("audio/*")
private const val CHUNK_BUFFER_SIZE = 64 * 1024

@Composable
actual fun rememberAudioFilePickerLauncher(
    onAudioFileSelected: (
        fileName: String,
        byteSize: Long,
        mimeType: String,
        audioSource: suspend (writeChunk: suspend (ByteArray) -> Unit) -> Unit,
    ) -> Unit,
): AudioFilePickerLauncher {
    val context = LocalContext.current
    val activityLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument(),
    ) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult

        runCatching {
            context.contentResolver.takePersistableUriPermission(
                uri,
                Intent.FLAG_GRANT_READ_URI_PERMISSION,
            )
        }

        var fileName = "recording.audio"
        var byteSize = -1L

        context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
            val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
            val sizeIndex = cursor.getColumnIndex(OpenableColumns.SIZE)
            if (cursor.moveToFirst()) {
                if (nameIndex != -1) {
                    fileName = cursor.getString(nameIndex) ?: fileName
                }
                if (sizeIndex != -1) {
                    byteSize = cursor.getLong(sizeIndex)
                }
            }
        }

        if (byteSize <= 0L) {
            runCatching {
                context.contentResolver.openAssetFileDescriptor(uri, "r")?.use { afd ->
                    byteSize = afd.length
                }
            }
        }

        val resolvedMime = context.contentResolver.getType(uri) ?: resolveMimeType(fileName)
        val audioSource: suspend (writeChunk: suspend (ByteArray) -> Unit) -> Unit = { writeChunk ->
            withContext(Dispatchers.IO) {
                val inputStream = context.contentResolver.openInputStream(uri)
                    ?: throw IllegalStateException("Cannot open input stream for audio uri: $uri")
                inputStream.use { stream ->
                    val buffer = ByteArray(CHUNK_BUFFER_SIZE)
                    var bytesRead: Int
                    while (stream.read(buffer).also { bytesRead = it } != -1) {
                        writeChunk(buffer.copyOf(bytesRead))
                    }
                }
            }
        }

        onAudioFileSelected(fileName, byteSize, resolvedMime, audioSource)
    }

    return remember(activityLauncher) {
        object : AudioFilePickerLauncher {
            override fun launch() {
                activityLauncher.launch(AUDIO_MIME_TYPES)
            }
        }
    }
}

private fun resolveMimeType(fileName: String): String {
    val ext = fileName.substringAfterLast('.', "").lowercase()
    return when (ext) {
        "wav" -> "audio/wav"
        "mp3" -> "audio/mpeg"
        "m4a", "mp4" -> "audio/mp4"
        "aac" -> "audio/aac"
        else -> "audio/*"
    }
}
