package com.example.klaf.presentation.deckList.deckDeleting

import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withStyle
import com.example.klaf.R
import com.example.klaf.presentation.common.FullBackgroundDialog
import com.example.klaf.presentation.common.RoundButton
import com.example.klaf.presentation.common.RoundedIcon
import com.example.klaf.presentation.theme.MainTheme

@Composable
fun DeckDeletionDialogView(
    deckName: String,
    onCloseDialogButtonClick: () -> Unit,
    onConfirmDeckDeletingButtonClick: () -> Unit,
) {
    FullBackgroundDialog(
        onBackgroundClick = onCloseDialogButtonClick,
        topContent = {
            RoundedIcon(
                background = MainTheme.colors.common.negativeDialogButton,
                iconId = R.drawable.ic_attention_mark_24,
            )
        },
        mainContent = {
            Text(
                style = MainTheme.typographies.dialogTextStyle,
                text = buildAnnotatedString {
                    withStyle(style = SpanStyle()) {
                        append(stringResource(R.string.deck_deleting_title))
                    }
                    withStyle(style = MainTheme.typographies.accentedDialogText) {
                        append(" \"${deckName}\"")
                    }
                    withStyle(style = SpanStyle()) { append("?") }
                },
            )
        },
        bottomContent = {
            DeckDeletingConformationButton(onClick = onConfirmDeckDeletingButtonClick)
            DialogClosingButton(onClick = onCloseDialogButtonClick)
        }
    )
}

@Composable
private fun DeckDeletingConformationButton(onClick: () -> Unit) {
    RoundButton(
        background = MainTheme.colors.common.negativeDialogButton,
        iconId = R.drawable.ic_delete_24,
        onClick = onClick
    )
}

@Composable
private fun DialogClosingButton(onClick: () -> Unit) {
    RoundButton(
        background = MainTheme.colors.common.neutralDialogButton,
        iconId = R.drawable.ic_close_24,
        onClick = onClick
    )
}