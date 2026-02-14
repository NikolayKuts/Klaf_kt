package com.kuts.klaf.deckList.deckCreation

import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.kuts.klaf.presentation.R
import com.kuts.klaf.common.EventMessage
import com.kuts.klaf.deckList.common.DeckNamingView
import com.kuts.klaf.theme.MainTheme

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