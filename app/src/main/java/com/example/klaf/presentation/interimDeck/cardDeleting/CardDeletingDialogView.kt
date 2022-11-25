package com.example.klaf.presentation.interimDeck.cardDeleting

import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.example.klaf.R
import com.example.klaf.presentation.common.DialogView
import com.example.klaf.presentation.common.RoundButton
import com.example.klaf.presentation.theme.MainTheme

@Composable
fun CardDeletingDialogView(
    cardQuantity: Int,
    onConfirmDeleting: () -> Unit,
    onCancel: () -> Unit,
) {
    DialogView(
        mainContent = {
            Text(
                style = MainTheme.typographies.dialogTextStyle,
                text = getDialogTitleByCardCount(quantity = cardQuantity)
            )
        },
        buttonContent = {
            DeckDeletingConformationButton(onClick = onConfirmDeleting)
            DialogClosingButton(onClick = onCancel)
        }
    )
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

@Composable
private fun getDialogTitleByCardCount(quantity: Int): String {
    return if (quantity == 1) {
        stringResource(id = R.string.single_cards_deleting_dialog_title, quantity)
    } else {
        stringResource(id = R.string.multiple_cards_deleting_dialog_title, quantity)
    }
}