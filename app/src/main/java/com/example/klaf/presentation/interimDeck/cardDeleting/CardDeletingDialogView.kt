package com.example.klaf.presentation.interimDeck.cardDeleting

import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.example.klaf.R
import com.example.klaf.presentation.common.DeletingButton
import com.example.klaf.presentation.common.ClosingButton
import com.example.klaf.presentation.common.DialogView
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
            DeletingButton(onClick = onConfirmDeleting)
            ClosingButton(onClick = onCancel)
        }
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