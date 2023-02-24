package com.example.presentation.deckList.deckDeleting

import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withStyle
import com.example.presentation.R
import com.example.presentation.common.FullBackgroundDialog
import com.example.presentation.common.RoundButton
import com.example.presentation.theme.MainTheme

@Composable
fun DeckDeletionDialogView(
    deckName: String,
    onCloseDialogButtonClick: () -> Unit,
    onConfirmDeckDeletingButtonClick: () -> Unit,
) {
    FullBackgroundDialog(
        onBackgroundClick = onCloseDialogButtonClick,
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
        background = MainTheme.colors.negativeDialogButton,
        iconId = R.drawable.ic_delete_24,
        onClick = onClick
    )
}

@Composable
private fun DialogClosingButton(onClick: () -> Unit) {
    RoundButton(
        background = MainTheme.colors.neutralDialogButton,
        iconId = R.drawable.ic_close_24,
        onClick = onClick
    )
}