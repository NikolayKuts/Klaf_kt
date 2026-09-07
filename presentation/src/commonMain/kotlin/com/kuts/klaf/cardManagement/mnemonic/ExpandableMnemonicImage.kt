package com.kuts.klaf.cardManagement.mnemonic

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.kuts.klaf.presentation.resources.Res
import com.kuts.klaf.presentation.resources.ic_close_24
import com.kuts.klaf.presentation.resources.mnemonic_image_preview_close_action
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource

@OptIn(ExperimentalFoundationApi::class)
@Composable
internal fun ExpandableMnemonicImage(
    imagePath: String,
    modifier: Modifier = Modifier,
    imageModifier: Modifier = Modifier.fillMaxSize(),
    contentDescription: String? = null,
    contentScale: ContentScale = ContentScale.Crop,
    onClick: () -> Unit = {},
) {
    var isPreviewVisible by remember(imagePath) { mutableStateOf(false) }

    Box(
        modifier = modifier.combinedClickable(
            onClick = onClick,
            onLongClick = { isPreviewVisible = true },
        ),
        contentAlignment = Alignment.Center,
    ) {
        MnemonicImagePreview(
            imagePath = imagePath,
            modifier = imageModifier,
            contentDescription = contentDescription,
            contentScale = contentScale,
        )
    }

    if (isPreviewVisible) {
        MnemonicImageFullscreenDialog(
            imagePath = imagePath,
            contentDescription = contentDescription,
            onDismiss = { isPreviewVisible = false },
        )
    }
}

@Composable
private fun MnemonicImageFullscreenDialog(
    imagePath: String,
    contentDescription: String?,
    onDismiss: () -> Unit,
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.94f)),
            contentAlignment = Alignment.Center,
        ) {
            MnemonicImagePreview(
                imagePath = imagePath,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 12.dp, vertical = 56.dp),
                contentDescription = contentDescription,
                contentScale = ContentScale.Fit,
            )

            IconButton(
                onClick = onDismiss,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(16.dp)
                    .clip(CircleShape)
                    .background(Color.White.copy(alpha = 0.16f)),
            ) {
                Icon(
                    painter = painterResource(Res.drawable.ic_close_24),
                    contentDescription = stringResource(
                        Res.string.mnemonic_image_preview_close_action,
                    ),
                    tint = Color.White,
                )
            }
        }
    }
}
