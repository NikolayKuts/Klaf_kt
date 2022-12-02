package com.example.klaf.presentation.deckList.deckRenaming

import androidx.compose.foundation.layout.*
import androidx.compose.material.Card
import androidx.compose.material.OutlinedTextField
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import com.example.klaf.R
import com.example.klaf.presentation.common.DIALOG_BUTTON_SIZE
import com.example.klaf.presentation.common.DialogBox
import com.example.klaf.presentation.common.RoundButton
import com.example.klaf.presentation.common.rememberAsMutableStateOf
import com.example.klaf.presentation.theme.MainTheme

@Composable
fun DeckRenamingDialog(
    deckName: String,
    onConfirmRenamingClick: (newName: String) -> Unit,
    onCloseDialogClick: () -> Unit,
) {
    var fieldDeckName by rememberAsMutableStateOf(value = deckName)

    DialogBox(onClick = onCloseDialogClick) {
        Box(modifier = Modifier.align(Alignment.Center)) {
            Card(
                modifier = Modifier
                    .defaultMinSize(minHeight = 150.dp, minWidth = 300.dp)
                    .padding(bottom = (DIALOG_BUTTON_SIZE / 2).dp)
            ) {
                Column(modifier = Modifier.padding(MainTheme.dimensions.dialogContentPadding)) {
                    DialogTitle(deckName = deckName)
                    Spacer(modifier = Modifier.height(16.dp))
                    RenamingTextField(
                        deckName = fieldDeckName,
                        onValueChange = { updatedName -> fieldDeckName = updatedName }
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                }
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth(0.5F)
                    .align(alignment = Alignment.BottomCenter),
                horizontalArrangement = Arrangement.SpaceAround
            ) {
                RoundButton(
                    background = MainTheme.colors.positiveDialogButton,
                    iconId = R.drawable.ic_confirmation_24,
                    onClick = { onConfirmRenamingClick(fieldDeckName) }
                )
                RoundButton(
                    background = MainTheme.colors.neutralDialogButton,
                    iconId = R.drawable.ic_close_24,
                    onClick = onCloseDialogClick
                )
            }
        }
    }
}

@Composable
private fun DialogTitle(deckName: String) {
    Text(
        style = MainTheme.typographies.dialogTextStyle,
        text = buildAnnotatedString {
            withStyle(style = SpanStyle()) {
                append(text = stringResource(id = R.string.rename_deck))
            }
            withStyle(style = MainTheme.typographies.accentedDialogText) {
                append(" \"${deckName}\"")
            }
        }
    )
}

@Composable
private fun RenamingTextField(deckName: String, onValueChange: (String) -> Unit) {
    OutlinedTextField(
        value = deckName,
        onValueChange = onValueChange,
        label = { Text(text = stringResource(R.string.deck_name_label)) },
        placeholder = { Text(text = stringResource(id = R.string.type_new_deck_name)) }
    )
}