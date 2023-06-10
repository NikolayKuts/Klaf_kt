package com.kuts.klaf.presentation.deckList.drawer

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.kuts.klaf.R
import com.kuts.klaf.presentation.common.*
import com.kuts.klaf.presentation.theme.MainTheme

@Composable
fun DrawerActionView(
    action: DrawerAction,
    onCloseDialog: () -> Unit,
    onConfirmationClick: () -> Unit,
) {
    ScrollableBox(
        modifier = Modifier.noRippleClickable(onClick = onCloseDialog),
        dialogMode = true,
        eventContent = {
//            eventMessage.ifNotNull { EventMessageView(message = it) }
        },
    ) {
        FullBackgroundDialog(
            onBackgroundClick = { onCloseDialog() },
            topContent = ContentHolder(size = DIALOG_APP_LABEL_SIZE.dp) { DialogAppLabel() },
            mainContent = {
                when (action) {
                    DrawerAction.LOG_OUT -> LogOutView()
                    DrawerAction.DELETE_ACCOUNT -> DeletingView()
                }
            },
            bottomContent = {
                ConfirmationButton(onClick = onConfirmationClick)
                ClosingButton(onClick = onCloseDialog)
            }
        )
    }
}

@Composable
private fun LogOutView() {
    Text(text = stringResource(R.string.log_out_confirmation_question))
}

@Composable
private fun DeletingView() {
    Column {
        WarningMessage(textId = R.string.account_deleting_warning_message)
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            modifier = Modifier.padding(6.dp),
            text = stringResource(R.string.account_deleting_confirmation_question),
            style = MainTheme.typographies.dialogTextStyle
        )
    }
}