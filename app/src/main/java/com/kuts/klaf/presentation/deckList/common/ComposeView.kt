package com.kuts.klaf.presentation.deckList.common

import androidx.compose.foundation.layout.*
import androidx.compose.material.OutlinedTextField
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.kuts.domain.common.ifNotNull
import com.kuts.domain.entities.Deck
import com.kuts.klaf.R
import com.kuts.klaf.presentation.common.*

@Composable
fun DeckNamingView(
    title: @Composable () -> Unit,
    onConfirmCreationClick: (deckName: String) -> Unit,
    onCloseDialogClick: () -> Unit,
    modifier: Modifier = Modifier,
    initialName: String? = null,
    eventMessage: EventMessage? = null,
) {
    val deckNameState = rememberSaveable { mutableStateOf(value = initialName ?: "") }

    ScrollableBox(
        modifier = modifier.noRippleClickable { onCloseDialogClick() },
        dialogMode = true,
        topContent = {
            eventMessage.ifNotNull { EventMessageView(message = it) }
        },
    ) {
        FullBackgroundDialog(
            onBackgroundClick = onCloseDialogClick,
            topContent = ContentHolder(size = DIALOG_APP_LABEL_SIZE.dp) { DialogAppLabel() },
            mainContent = {
                Column {
                    title()
                    Spacer(modifier = Modifier.height(16.dp))
                    DeckNameTextField(
                        deckNameState = deckNameState,
                        placeholder = { Text(text = stringResource(id = R.string.enter_deck_name)) },
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                }
            },
            bottomContent = {
                ConfirmationButton(onClick = { onConfirmCreationClick(deckNameState.value) })
                ClosingButton(onClick = onCloseDialogClick)
            }
        )
    }
}

@Composable
private fun DeckNameTextField(
    deckNameState: MutableState<String>,
    placeholder: @Composable () -> Unit,
) {
    OutlinedTextField(
        modifier = Modifier.width(MinElementWidth),
        value = deckNameState.value,
        singleLine = true,
        maxLines = 1,
        onValueChange = { updatedName ->
            if (updatedName.length <= Deck.MAX_NAME_LENGTH) {
                deckNameState.value = updatedName
            }
        },
        label = { Text(text = stringResource(R.string.deck_name_label)) },
        placeholder = placeholder,
    )
}