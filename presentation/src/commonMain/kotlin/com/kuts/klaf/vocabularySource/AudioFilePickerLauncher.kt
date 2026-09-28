package com.kuts.klaf.vocabularySource

import androidx.compose.runtime.Composable

interface AudioFilePickerLauncher {
    fun launch()
}

@Composable
expect fun rememberAudioFilePickerLauncher(
    onAudioFileSelected: (
        fileName: String,
        byteSize: Long,
        mimeType: String,
        audioSource: suspend (writeChunk: suspend (ByteArray) -> Unit) -> Unit,
    ) -> Unit,
): AudioFilePickerLauncher
