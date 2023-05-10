package com.example.klaf.presentation.deckList.deckRenaming

import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withStyle
import com.example.klaf.R
import com.example.klaf.presentation.common.EventMessage
import com.example.klaf.presentation.deckList.common.DeckNamingView
import com.example.klaf.presentation.theme.MainTheme

@Composable
fun DeckRenamingDialog(
    deckName: String,
    onConfirmRenamingClick: (newName: String) -> Unit,
    onCloseDialogClick: () -> Unit,
    eventMessage: EventMessage?,
) {
    DeckNamingView(
        title = { DialogTitle(deckName = deckName) },
        onConfirmCreationClick = onConfirmRenamingClick,
        onCloseDialogClick = onCloseDialogClick,
        initialName = deckName,
        eventMessage = eventMessage,
    )
}

@Composable
private fun DialogTitle(deckName: String) {
    Text(
        style = MainTheme.typographies.dialogTextStyle,
        text = buildAnnotatedString {
            withStyle(style = SpanStyle()) {
                append(text = stringResource(id = R.string.deck_navigation_dialog_item_rename_deck))
            }
            withStyle(style = MainTheme.typographies.accentedDialogText) {
                append(" \"${deckName}\"")
            }
        }
    )
}