package com.kuts.klaf.presentation.deckList.deckDeleting

import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import com.kuts.domain.common.ifNotNull
import com.kuts.klaf.R
import com.kuts.klaf.presentation.common.*
import com.kuts.klaf.presentation.theme.MainTheme

@Composable
fun DeckDeletionDialogView(
    deckName: String,
    eventMessage: EventMessage?,
    onCloseDialogClick: () -> Unit,
    onConfirmDeckDeletingButtonClick: () -> Unit,
) {
    ScrollableBox(
        modifier = Modifier.noRippleClickable { onCloseDialogClick() },
        dialogMode = true,
        eventContent = {
            eventMessage.ifNotNull { EventMessageView(message = it) }
        }
    ) {
        FullBackgroundDialog(
            onBackgroundClick = onCloseDialogClick,
            topContent = ContentHolder(size = DIALOG_APP_LABEL_SIZE.dp) { DialogAppLabel() },
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
                DialogClosingButton(onClick = onCloseDialogClick)
            }
        )
    }
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