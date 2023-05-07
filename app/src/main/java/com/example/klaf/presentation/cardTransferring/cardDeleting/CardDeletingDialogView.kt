package com.example.klaf.presentation.cardTransferring.cardDeleting

import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.example.klaf.R
import com.example.klaf.presentation.common.ClosingButton
import com.example.klaf.presentation.common.DeletingButton
import com.example.klaf.presentation.common.FullBackgroundDialog
import com.example.klaf.presentation.common.RoundedIcon
import com.example.klaf.presentation.theme.MainTheme

@Composable
fun CardDeletingDialogView(
    cardQuantity: Int,
    onConfirmDeleting: () -> Unit,
    onCancel: () -> Unit,
) {
    FullBackgroundDialog(
        onBackgroundClick = onCancel,
        topContent = {
            RoundedIcon(
                background = MainTheme.colors.common.negativeDialogButton,
                iconId = R.drawable.ic_attention_mark_24
            )
        },
        mainContent = {
            Text(
                modifier = Modifier,
                style = MainTheme.typographies.dialogTextStyle,
                text = getDialogTitleByCardCount(quantity = cardQuantity)
            )
        },
        bottomContent = {
            DeletingButton(onClick = onConfirmDeleting)
            ClosingButton(onClick = onCancel)
        },
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