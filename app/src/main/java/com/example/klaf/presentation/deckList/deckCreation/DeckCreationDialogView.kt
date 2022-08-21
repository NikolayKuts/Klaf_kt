package com.example.klaf.presentation.deckList.deckCreation

import androidx.compose.foundation.layout.*
import androidx.compose.material.Card
import androidx.compose.material.OutlinedTextField
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.example.klaf.R
import com.example.klaf.presentation.common.DIALOG_BUTTON_SIZE
import com.example.klaf.presentation.common.RoundButton
import com.example.klaf.presentation.common.rememberAsMutableStateOf
import com.example.klaf.presentation.theme.MainTheme


@Composable
fun DeckCreationDialog(
    onConfirmCreationClick: (deckName: String) -> Unit,
    onCloseDialogClick: () -> Unit,
) {
    val deckNameState = rememberAsMutableStateOf(value = "")

    Box() {
        Card(
            modifier = Modifier
                .defaultMinSize(minHeight = 150.dp, minWidth = 300.dp)
                .padding(bottom = (DIALOG_BUTTON_SIZE / 2).dp)
        ) {
            Column(modifier = Modifier.padding(MainTheme.dimensions.dialogContentPadding)) {
                DialogTitle()
                Spacer(modifier = Modifier.height(16.dp))
                DeckNameTextField(deckNameState = deckNameState)
                Spacer(modifier = Modifier.height(16.dp))
            }
        }

        Row(
            modifier = Modifier
                .fillMaxWidth(0.5F)
                .align(alignment = Alignment.BottomCenter),
            horizontalArrangement = Arrangement.SpaceAround
        ) {
            ConformationButton(
                onClick = { onConfirmCreationClick(deckNameState.value) }
            )
            DialogClosingButton(onClick = onCloseDialogClick)
        }
    }
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
    OutlinedTextField(
        value = deckNameState.value,
        onValueChange = { updatedName -> deckNameState.value = updatedName },
        label = { Text(text = stringResource(R.string.deck_name_label)) },
        placeholder = { Text(text = stringResource(id = R.string.enter_deck_name)) },
    )
}

@Composable
private fun ConformationButton(onClick: () -> Unit) {
    RoundButton(
        background = MainTheme.colors.positiveDialogButton,
        iconId = R.drawable.ic_comfirmation_24,
        onClick = onClick
    )
}

@Composable
private fun DialogClosingButton(onClick: () -> Unit) {
    RoundButton(
        background = MainTheme.colors.neutralDialogButton,
        iconId = R.drawable.ic_close_24,
        onClick = onClick
    )
}
