package com.kuts.klaf.deckList.deckCreation

import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import com.kuts.klaf.common.EventMessage
import com.kuts.klaf.deckList.common.DeckNamingView
import com.kuts.klaf.presentation.resources.*
import com.kuts.klaf.theme.MainTheme
import org.jetbrains.compose.resources.stringResource

@Composable
fun DeckCreationDialog(
    onConfirmCreationClick: (deckName: String) -> Unit,
    onCloseDialogClick: () -> Unit,
    eventMassage: EventMessage? = null,
) {
    DeckNamingView(
        title = { DialogTitle() },
        onConfirmCreationClick = onConfirmCreationClick,
        onCloseDialogClick = onCloseDialogClick,
        eventMessage = eventMassage
    )
}

@Composable
private fun DialogTitle() {
    Text(
        style = MainTheme.typographies.dialogTextStyle,
        text = stringResource(resource = Res.string.deck_creation_dialog_title),
    )
}
