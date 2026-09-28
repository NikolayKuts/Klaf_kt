package com.kuts.klaf.deckList.common

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.kuts.klaf.common.DateFormatPattern
import com.kuts.klaf.common.asFormattedDate
import com.kuts.klaf.presentation.resources.Res
import com.kuts.klaf.presentation.resources.*
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource

private val synchronizedColor = Color(0xFF2E7D32)
private val pendingColor = Color(0xFFF9A825)
private val failureColor = Color(0xFFC62828)
private val unavailableColor = Color.Gray

private fun statusColor(indicator: AccountSyncIndicator): Color = when (indicator) {
    AccountSyncIndicator.HIDDEN, AccountSyncIndicator.GRAY -> unavailableColor
    AccountSyncIndicator.GREEN -> synchronizedColor
    AccountSyncIndicator.YELLOW -> pendingColor
    AccountSyncIndicator.RED -> failureColor
    AccountSyncIndicator.SYNCING -> pendingColor
}

@Composable
private fun statusLabel(indicator: AccountSyncIndicator): String = stringResource(when (indicator) {
    AccountSyncIndicator.HIDDEN -> Res.string.sync_status_title
    AccountSyncIndicator.GREEN -> Res.string.sync_status_green
    AccountSyncIndicator.YELLOW -> Res.string.sync_status_yellow
    AccountSyncIndicator.RED -> Res.string.sync_status_red
    AccountSyncIndicator.GRAY -> Res.string.sync_status_gray
    AccountSyncIndicator.SYNCING -> Res.string.sync_status_syncing
})

@Composable
internal fun AccountSyncStatusIndicator(status: AccountSyncStatus, onClick: () -> Unit) {
    if (status.indicator == AccountSyncIndicator.HIDDEN) return
    val label = statusLabel(status.indicator)
    Row(
        modifier = Modifier.clickable(onClick = onClick).padding(12.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(painter = painterResource(Res.drawable.ic_sync_24), contentDescription = label)
        Box(Modifier.size(10.dp).background(statusColor(status.indicator), CircleShape))
    }
}

@Composable
internal fun AccountSyncDetailsDialog(
    status: AccountSyncStatus,
    history: AccountSyncHistoryState,
    onDismiss: () -> Unit,
    onSynchronize: () -> Unit,
    onResolve: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(Res.string.sync_status_title)) },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Text(statusLabel(status.indicator))
                Text(stringResource(Res.string.sync_status_local_revision, status.confirmedRevision))
                val serverRevision = status.serverRevision
                Text(if (serverRevision == null) stringResource(Res.string.sync_status_server_unknown)
                    else stringResource(Res.string.sync_status_server_revision, serverRevision))
                Text(stringResource(Res.string.sync_status_pending, status.pendingOperationCount))
                if (status.devices.isNotEmpty()) {
                    Text(stringResource(Res.string.sync_status_devices))
                    status.devices.forEach { device ->
                        Text(stringResource(
                            if (device.connected) Res.string.sync_status_device_online
                            else Res.string.sync_status_device_offline,
                            device.name,
                            device.platform,
                        ))
                        val confirmedRevision = device.lastConfirmedRevision
                        Text(if (confirmedRevision == null) {
                            stringResource(Res.string.sync_status_device_revision_unknown)
                        } else {
                            stringResource(Res.string.sync_status_device_revision, confirmedRevision)
                        })
                        val lastSyncAt = device.lastSuccessfulSyncAtMillis
                        Text(if (lastSyncAt == null) {
                            stringResource(Res.string.sync_status_device_never_synced)
                        } else {
                            stringResource(Res.string.sync_status_device_last_sync,
                                lastSyncAt.asFormattedDate(DateFormatPattern.FULL))
                        })
                    }
                }
                Text(stringResource(Res.string.sync_status_history_title))
                when (history) {
                    AccountSyncHistoryState.Loading -> Text(stringResource(Res.string.sync_status_history_loading))
                    AccountSyncHistoryState.Failed -> Text(stringResource(Res.string.sync_status_history_failed))
                    is AccountSyncHistoryState.Loaded -> {
                        if (history.entries.isEmpty()) {
                            Text(stringResource(Res.string.sync_status_history_empty))
                        } else {
                            history.entries.forEach { entry ->
                                Text(stringResource(
                                    Res.string.sync_status_history_entry,
                                    entry.revision,
                                    entry.deviceName(status.devices),
                                    entry.occurredAtMillis.asFormattedDate(DateFormatPattern.FULL),
                                ))
                                Text(stringResource(
                                    Res.string.sync_status_history_action,
                                    entry.action.replace('_', ' ').lowercase().replaceFirstChar(Char::uppercase),
                                    entry.affectedItemsSummary(),
                                ))
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(onClick = onSynchronize, enabled = status.indicator != AccountSyncIndicator.SYNCING) {
                Text(stringResource(Res.string.sync_status_start))
            }
        },
        dismissButton = {
            Row {
                if (status.hasConflict) {
                    TextButton(onClick = onResolve) { Text(stringResource(Res.string.sync_status_resolve)) }
                }
                TextButton(onClick = onDismiss) { Text(stringResource(Res.string.sync_conflicts_close)) }
            }
        },
    )
}
