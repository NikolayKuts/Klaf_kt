package com.kuts.klaf.deckList.deckRenaming

import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withStyle
import com.kuts.klaf.common.EventMessage
import com.kuts.klaf.deckList.common.DeckNamingView
import com.kuts.klaf.presentation.resources.*
import com.kuts.klaf.theme.MainTheme
import org.jetbrains.compose.resources.stringResource

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
                append(text = stringResource(resource = Res.string.deck_navigation_dialog_item_rename_deck))
            }
            withStyle(style = MainTheme.typographies.accentedDialogText) {
                append(" \"${deckName}\"")
            }
        }
    )
}
