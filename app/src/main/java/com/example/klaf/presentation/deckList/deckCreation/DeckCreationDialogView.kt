package com.example.klaf.presentation.deckList.deckCreation

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.material.OutlinedTextField
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.example.klaf.R
import com.example.klaf.presentation.common.FullBackgroundDialog
import com.example.klaf.presentation.common.RoundButton
import com.example.klaf.presentation.common.rememberAsMutableStateOf
import com.example.klaf.presentation.theme.MainTheme

@Composable
fun DeckCreationDialog(
    onConfirmCreationClick: (deckName: String) -> Unit,
    onCloseDialogClick: () -> Unit,
) {
    val deckNameState = rememberAsMutableStateOf(value = "")

    FullBackgroundDialog(
        onBackgroundClick = onCloseDialogClick,
        mainContent = {
            Column {
                DialogTitle()
                Spacer(modifier = Modifier.height(16.dp))
                DeckNameTextField(deckNameState = deckNameState)
                Spacer(modifier = Modifier.height(16.dp))
            }
        },
        bottomContent = {
            ConformationButton(onClick = { onConfirmCreationClick(deckNameState.value) })
            DialogClosingButton(onClick = onCloseDialogClick)
        }
    )
}

@Composable
private fun DialogTitle() {
    Text(
        style = MainTheme.typographies.dialogTextStyle,
        text = stringResource(id = R.string.deck_creation_dialog_title),
    )
}

@Composable
private fun DeckNameTextField(deckNameState: MutableState<String>) {
    val maxNameLength = 30

    OutlinedTextField(
        value = deckNameState.value,
        singleLine = true,
        maxLines = 1,
        onValueChange = { updatedName ->
            if (updatedName.length <= maxNameLength) {
                deckNameState.value = updatedName
            }
        },
        label = { Text(text = stringResource(R.string.deck_name_label)) },
        placeholder = { Text(text = stringResource(id = R.string.enter_deck_name)) },
    )
}

@Composable
private fun ConformationButton(onClick: () -> Unit) {
    RoundButton(
        background = MainTheme.colors.commonColors.positiveDialogButton,
        iconId = R.drawable.ic_confirmation_24,
        onClick = onClick
    )
}

@Composable
private fun DialogClosingButton(onClick: () -> Unit) {
    RoundButton(
        background = MainTheme.colors.commonColors.neutralDialogButton,
        iconId = R.drawable.ic_close_24,
        onClick = onClick
    )
}