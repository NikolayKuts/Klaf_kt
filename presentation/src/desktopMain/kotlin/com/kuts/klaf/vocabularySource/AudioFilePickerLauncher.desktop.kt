package com.kuts.klaf.vocabularySource

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.awt.FileDialog
import java.awt.Frame
import java.io.File
import java.io.FilenameFilter

private const val CHUNK_BUFFER_SIZE = 64 * 1024
private val AUDIO_EXTENSIONS = setOf("wav", "mp3", "m4a")

@Composable
actual fun rememberAudioFilePickerLauncher(
    onAudioFileSelected: (
        fileName: String,
        byteSize: Long,
        mimeType: String,
        audioSource: suspend (writeChunk: suspend (ByteArray) -> Unit) -> Unit,
    ) -> Unit,
): AudioFilePickerLauncher {
    val currentOnSelected = rememberUpdatedState(onAudioFileSelected)
    return remember {
        object : AudioFilePickerLauncher {
            override fun launch() {
                val dialog = FileDialog(null as Frame?, "Select Audio File", FileDialog.LOAD)
                dialog.filenameFilter = FilenameFilter { _, name ->
                    val ext = name.substringAfterLast('.', "").lowercase()
                    ext in AUDIO_EXTENSIONS
                }
                val selectedFile = try {
                    dialog.isVisible = true
                    dialog.file?.let { fileName ->
                        dialog.directory?.let { dir -> File(dir, fileName) }
                    }
                } finally {
                    dialog.dispose()
                }
                if (selectedFile != null && selectedFile.exists()) {
                    val fileName = selectedFile.name
                    val byteSize = selectedFile.length()
                    val mimeType = resolveDesktopMimeType(fileName)
                    val audioSource: suspend (writeChunk: suspend (ByteArray) -> Unit) -> Unit = { writeChunk ->
                        withContext(Dispatchers.IO) {
                            selectedFile.inputStream().use { stream ->
                                val buffer = ByteArray(CHUNK_BUFFER_SIZE)
                                var bytesRead: Int
                                while (stream.read(buffer).also { bytesRead = it } != -1) {
                                    writeChunk(buffer.copyOf(bytesRead))
                                }
                            }
                        }
                    }
                    currentOnSelected.value(fileName, byteSize, mimeType, audioSource)
                }
            }
        }
    }
}

private fun resolveDesktopMimeType(fileName: String): String {
    val ext = fileName.substringAfterLast('.', "").lowercase()
    return when (ext) {
        "wav" -> "audio/wav"
        "mp3" -> "audio/mpeg"
        "m4a", "mp4" -> "audio/mp4"
        "aac" -> "audio/aac"
        else -> "audio/mpeg"
    }
}
