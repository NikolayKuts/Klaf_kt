package com.example.klaf.presentation.deckList.deckCreation

import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.example.klaf.R
import com.example.klaf.presentation.common.EventMessage
import com.example.klaf.presentation.deckList.common.DeckNamingView
import com.example.klaf.presentation.theme.MainTheme

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
        eventMassage = eventMassage
    )
}

@Composable
private fun DialogTitle() {
    Text(
        style = MainTheme.typographies.dialogTextStyle,
        text = stringResource(id = R.string.deck_creation_dialog_title),
    )
}