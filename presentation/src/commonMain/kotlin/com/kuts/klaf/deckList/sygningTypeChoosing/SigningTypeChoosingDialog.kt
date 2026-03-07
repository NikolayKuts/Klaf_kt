package com.kuts.klaf.deckList.sygningTypeChoosing

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.kuts.domain.common.AuthenticationAction
import com.kuts.klaf.common.*
import com.kuts.klaf.common.NavigationDestination
import com.kuts.klaf.common.NavigationDestination.*
import com.kuts.klaf.navigation.AppDestination
import com.kuts.klaf.presentation.resources.*
import com.kuts.klaf.theme.MainTheme
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource

@Composable
internal fun SigningTypeChoosingDialog(
    navController: NavHostController,
    fromSourceDestination: NavigationDestination,
) {
    SigningTypeChoosingDialogContent(
        fromSourceDestination = fromSourceDestination,
        onSigningActionButtonClick = { action ->
            val currentDestinationId = navController.currentDestination?.id

            navController.navigate(
                route = AppDestination.Authentication(
                    authenticationAction = action,
                    fromSourceDestination = fromSourceDestination,
                )
            ) {
                currentDestinationId?.let { destinationId ->
                    popUpTo(id = destinationId) { inclusive = true }
                }
            }
        },
        onCloseButtonClick = { navController.popBackStack() },
    )
}

@Composable
private fun SigningTypeChoosingDialogContent(
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
                val textRes: StringResource = when (fromSourceDestination) {
                    DECK_LIST_FRAGMENT -> Res.string.authentication_type_choosing_message
                    DATA_SYNCHRONIZATION_DIALOG -> Res.string.authentication_sync_data_message
                }
                Column {
                    Text(
                        style = MainTheme.typographies.dialogTextStyle,
                        text = stringResource(resource = textRes),
                    )

                    Spacer(modifier = Modifier.height(20.dp))

                    SigningButton(
                        text = stringResource(resource = Res.string.authentication_sign_in_label),
                        onClick = { onSigningActionButtonClick(AuthenticationAction.SIGN_IN) }
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    SigningButton(
                        text = stringResource(resource = Res.string.authentication_sign_up_label),
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
            style = MaterialTheme.typography.labelLarge
        )
    }
}
