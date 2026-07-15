package com.kuts.klaf.cardManagement.mnemonic

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toComposeImageBitmap
import androidx.compose.ui.layout.ContentScale
import java.io.File
import org.jetbrains.skia.Image as SkiaImage

@Composable
internal actual fun MnemonicImagePreview(
    imagePath: String,
    modifier: Modifier,
    contentDescription: String?,
    contentScale: ContentScale,
) {
    val bitmap = remember(imagePath) {
        runCatching {
            SkiaImage.makeFromEncoded(File(imagePath).readBytes()).toComposeImageBitmap()
        }.getOrNull()
    }

    if (bitmap == null) {
        Box(
            modifier = modifier.background(Color(0x14000000)),
            contentAlignment = Alignment.Center,
        ) {
            Text(text = "Image unavailable")
        }
    } else {
        Image(
            bitmap = bitmap,
            contentDescription = contentDescription,
            modifier = modifier,
            contentScale = contentScale,
        )
    }
}
