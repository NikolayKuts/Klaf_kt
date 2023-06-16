package com.kuts.klaf.presentation.deckList.sygningTypeChoosing

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.ButtonDefaults
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.kuts.domain.common.AuthenticationAction
import com.kuts.klaf.R
import com.kuts.klaf.presentation.common.*
import com.kuts.klaf.presentation.common.NavigationDestination.*
import com.kuts.klaf.presentation.theme.MainTheme

@Composable
fun SigningTypeChoosingView(
    fromSourceDestination: NavigationDestination,
    onSigningActionButtonClick: (action: AuthenticationAction) -> Unit,
    onCloseButtonClick: () -> Unit,
) {
    ScrollableBox(
        modifier = Modifier.noRippleClickable { onCloseButtonClick() },
        dialogMode = true,
    ) {
        FullBackgroundDialog(
            onBackgroundClick = onCloseButtonClick,
            topContent = ContentHolder(size = DIALOG_APP_LABEL_SIZE.dp) { DialogAppLabel() },
            mainContent = {
                val textId = when (fromSourceDestination) {
                    DECK_LIST_FRAGMENT -> R.string.authentication_type_choosing_message
                    DATA_SYNCHRONIZATION_DIALOG -> R.string.authentication_sync_data_message
                }
                Column {
                    Text(
                        style = MainTheme.typographies.dialogTextStyle,
                        text = stringResource(id = textId),
                    )

                    Spacer(modifier = Modifier.height(20.dp))

                    SigningButton(
                        text = stringResource(id = R.string.authentication_sign_in_label),
                        onClick = { onSigningActionButtonClick(AuthenticationAction.SIGN_IN) }
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    SigningButton(
                        text = stringResource(id = R.string.authentication_sign_up_label),
                        onClick = { onSigningActionButtonClick(AuthenticationAction.SIGN_UP) }
                    )
                }
            },
            bottomContent = {
                ClosingButton(onClick = onCloseButtonClick)
            },
        )
    }
}

@Composable
private fun SigningButton(
    text: String,
    onClick: () -> Unit,
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(shape = MaterialTheme.shapes.small)
            .background(MainTheme.colors.common.positiveDialogButton)
            .defaultMinSize(
                minWidth = ButtonDefaults.MinWidth,
                minHeight = ButtonDefaults.MinHeight
            )
            .clickable { onClick() }
            .padding(ButtonDefaults.ContentPadding),
    ) {
        Text(
            modifier = Modifier.align(Alignment.Center),
            text = text,
            textAlign = TextAlign.Center,
            style = MaterialTheme.typography.button
        )
    }
}