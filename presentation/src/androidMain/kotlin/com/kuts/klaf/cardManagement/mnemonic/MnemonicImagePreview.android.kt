package com.kuts.klaf.cardManagement.mnemonic

import android.graphics.BitmapFactory
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale

@Composable
internal actual fun MnemonicImagePreview(
    imagePath: String,
    modifier: Modifier,
    contentDescription: String?,
    contentScale: ContentScale,
) {
    val bitmap = remember(imagePath) {
        BitmapFactory.decodeFile(imagePath)?.asImageBitmap()
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
