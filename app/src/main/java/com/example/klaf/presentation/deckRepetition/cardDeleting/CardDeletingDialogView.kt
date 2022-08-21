package com.example.klaf.presentation.deckRepetition.cardDeleting

import androidx.compose.foundation.layout.*
import androidx.compose.material.Card
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.example.klaf.R
import com.example.klaf.presentation.common.DIALOG_BUTTON_SIZE
import com.example.klaf.presentation.common.RoundButton
import com.example.klaf.presentation.theme.MainTheme

@Composable
fun CardDeletingDialogView(
    onConfirmDeleting: () -> Unit,
    onCancel: () -> Unit,
) {
    Box {
        Card(
            modifier = Modifier
                .defaultMinSize(minHeight = 150.dp, minWidth = 300.dp)
                .padding(bottom = (DIALOG_BUTTON_SIZE / 2).dp)
        ) {
            Box(modifier = Modifier.padding(MainTheme.dimensions.dialogContentPadding)) {
                Text(
                    style = MainTheme.typographies.dialogTextStyle,
                    text = stringResource(id = R.string.card_deleting_dialog_title)
                )
            }
        }
        Row(
            modifier = Modifier
                .fillMaxWidth(0.5F)
                .align(alignment = Alignment.BottomCenter),
            horizontalArrangement = Arrangement.SpaceAround
        ) {
            DeckDeletingConformationButton(onClick = onConfirmDeleting)
            DialogClosingButton(onClick = onCancel)
        }

    }
}

@Composable
private fun DeckDeletingConformationButton(onClick: () -> Unit) {
    RoundButton(
        background = MainTheme.colors.negativeDialogButton,
        iconId = R.drawable.ic_delete_24,
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
