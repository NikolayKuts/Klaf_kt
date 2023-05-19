package com.kuts.klaf.presentation.deckList.deckCreation

import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.kuts.klaf.R
import com.kuts.klaf.presentation.common.EventMessage
import com.kuts.klaf.presentation.deckList.common.DeckNamingView
import com.kuts.klaf.presentation.theme.MainTheme

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
        text = stringResource(id = R.string.deck_creation_dialog_title),
    )
}