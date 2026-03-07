package com.kuts.klaf.common.permissions

import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.kuts.klaf.common.ClosingButton
import com.kuts.klaf.common.ConfirmationButton
import com.kuts.klaf.common.ContentHolder
import com.kuts.klaf.common.DIALOG_APP_LABEL_SIZE
import com.kuts.klaf.common.DialogAppLabel
import com.kuts.klaf.common.FullBackgroundDialog
import com.kuts.klaf.common.ScrollableBox
import com.kuts.klaf.common.noRippleClickable
import com.kuts.klaf.presentation.resources.Res
import com.kuts.klaf.presentation.resources.*
import com.kuts.klaf.theme.MainTheme
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.stringResource

@Composable
fun NotificationPermissionDialogs(
    shouldCheckPermission: Boolean,
    onPermissionCheckConsumed: () -> Unit,
    permissionManager: INotificationPermissionManager,
) {
    var showRequestDialog by remember { mutableStateOf(false) }
    var showSettingsDialog by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    LaunchedEffect(key1 = shouldCheckPermission) {
        if (shouldCheckPermission) {
            when (permissionManager.getPermissionState()) {
                NotificationPermissionState.GRANTED -> {
                    showRequestDialog = false
                    showSettingsDialog = false
                }

                NotificationPermissionState.DENIED_ALWAYS -> {
                    showRequestDialog = false
                    showSettingsDialog = true
                }

                NotificationPermissionState.NOT_GRANTED,
                NotificationPermissionState.DENIED,
                -> {
                    showRequestDialog = true
                    showSettingsDialog = false
                }
            }

            onPermissionCheckConsumed()
        }
    }

    if (showRequestDialog) {
        ScrollableBox(
            modifier = Modifier.noRippleClickable { showRequestDialog = false },
            dialogMode = true,
        ) {
            FullBackgroundDialog(
                onBackgroundClick = { showRequestDialog = false },
                topContent = ContentHolder(size = DIALOG_APP_LABEL_SIZE.dp) { DialogAppLabel() },
                mainContent = {
                    Text(
                        style = MainTheme.typographies.dialogTextStyle,
                        text = stringResource(
                            resource = Res.string.notification_permission_request_dialog_message
                        ),
                    )
                },
                bottomContent = {
                    ConfirmationButton(
                        onClick = {
                            showRequestDialog = false
                            scope.launch {
                                val result = permissionManager.requestPermissionIfNeeded()
                                if (result == NotificationPermissionRequestResult.DENIED_ALWAYS) {
                                    showSettingsDialog = true
                                }
                            }
                        },
                    )
                    ClosingButton(onClick = { showRequestDialog = false })
                },
            )
        }
    }

    if (showSettingsDialog) {
        ScrollableBox(
            modifier = Modifier.noRippleClickable { showSettingsDialog = false },
            dialogMode = true,
        ) {
            FullBackgroundDialog(
                onBackgroundClick = { showSettingsDialog = false },
                topContent = ContentHolder(size = DIALOG_APP_LABEL_SIZE.dp) { DialogAppLabel() },
                mainContent = {
                    Text(
                        style = MainTheme.typographies.dialogTextStyle,
                        text = stringResource(
                            resource = Res.string.notification_permission_settings_dialog_message
                        ),
                    )
                },
                bottomContent = {
                    ConfirmationButton(
                        onClick = {
                            showSettingsDialog = false
                            permissionManager.openAppSettings()
                        },
                    )
                    ClosingButton(onClick = { showSettingsDialog = false })
                },
            )
        }
    }
}
