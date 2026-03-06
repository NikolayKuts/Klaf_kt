package com.kuts.klaf.deckList.deckDeleting

import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import com.kuts.domain.common.ifNotNull
import com.kuts.klaf.common.*
import com.kuts.klaf.presentation.resources.*
import com.kuts.klaf.theme.MainTheme
import org.jetbrains.compose.resources.stringResource

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
                            append(stringResource(resource = Res.string.deck_deleting_title))
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
        iconRes = Res.drawable.ic_delete_24,
        onClick = onClick
    )
}

@Composable
private fun DialogClosingButton(onClick: () -> Unit) {
    RoundButton(
        background = MainTheme.colors.common.neutralDialogButton,
        iconRes = Res.drawable.ic_close_24,
        onClick = onClick
    )
}
