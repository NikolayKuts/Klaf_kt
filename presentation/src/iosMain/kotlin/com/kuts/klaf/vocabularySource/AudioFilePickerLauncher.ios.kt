package com.kuts.klaf.vocabularySource

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember

@Composable
actual fun rememberAudioFilePickerLauncher(
    onAudioFileSelected: (
        fileName: String,
        byteSize: Long,
        mimeType: String,
        audioSource: suspend (writeChunk: suspend (ByteArray) -> Unit) -> Unit,
    ) -> Unit,
): AudioFilePickerLauncher {
    return remember {
        object : AudioFilePickerLauncher {
            override fun launch() {
                // iOS picker can be implemented when iOS target is fully enabled
            }
        }
    }
}
