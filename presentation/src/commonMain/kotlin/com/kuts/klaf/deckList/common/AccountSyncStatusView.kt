package com.kuts.klaf.deckList.common

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.kuts.klaf.common.DateFormatPattern
import com.kuts.klaf.common.asFormattedDate
import com.kuts.klaf.presentation.resources.Res
import com.kuts.klaf.presentation.resources.*
import com.kuts.klaf.server.contract.SyncHistoryItem
import com.kuts.klaf.theme.MainTheme
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource

private val synchronizedColor = Color(0xFF2E7D32)
private val pendingColor = Color(0xFFF9A825)
private val failureColor = Color(0xFFC62828)
private val unavailableColor = Color.Gray

private const val PREVIEW_SYNC_TIME = 1_792_742_400_000L

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
        title = {
            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(painterResource(Res.drawable.ic_sync_24), contentDescription = null)
                Text(stringResource(Res.string.sync_status_title))
            }
        },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(20.dp),
            ) {
                SyncOverview(status)
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    SyncSectionHeading(stringResource(Res.string.sync_status_devices), status.devices.size)
                    if (status.devices.isEmpty()) {
                        SyncCard { SyncSecondaryText(stringResource(Res.string.sync_status_devices_empty)) }
                    }
                    status.devices.forEach { device -> SyncDeviceCard(device) }
                }
                SyncHistorySection(history, status.devices)
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

@Composable
private fun SyncOverview(status: AccountSyncStatus) {
    val accent = statusContentColor(status.indicator)
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Surface(
            color = accent.copy(alpha = 0.12f),
            contentColor = accent,
            shape = RoundedCornerShape(12.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(12.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(Modifier.size(8.dp).background(accent, CircleShape))
                Text(
                    text = statusLabel(status.indicator),
                    style = MaterialTheme.typography.labelLarge,
                )
            }
        }
        if (status.indicator == AccountSyncIndicator.SYNCING) {
            LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
        }
        SyncCard {
            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                SyncRevision(
                    label = stringResource(Res.string.sync_status_local_label),
                    revision = status.confirmedRevision,
                    modifier = Modifier.weight(1f),
                )
                SyncRevision(
                    label = stringResource(Res.string.sync_status_server_label),
                    revision = status.serverRevision,
                    modifier = Modifier.weight(1f),
                )
            }
            SyncMetadata(
                label = stringResource(Res.string.sync_status_pending_label),
                value = status.pendingOperationCount.toString(),
            )
        }
    }
}

@Composable
private fun SyncRevision(
    label: String,
    revision: Long?,
    modifier: Modifier,
) {
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(4.dp)) {
        SyncSecondaryText(label)
        Text(
            text = revision?.toString() ?: stringResource(Res.string.sync_status_unknown),
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.SemiBold,
        )
    }
}

@Composable
private fun SyncDeviceCard(device: AccountSyncDevice) {
    SyncCard {
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.Top,
        ) {
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(device.name, style = MaterialTheme.typography.titleSmall)
                SyncSecondaryText(device.platform)
            }
            Column(
                horizontalAlignment = Alignment.End,
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                if (device.isCurrentDevice) {
                    SyncBadge(
                        text = stringResource(Res.string.sync_status_this_device),
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                }
                SyncBadge(
                    text = stringResource(
                        if (device.connected) Res.string.sync_status_connected else Res.string.sync_status_offline
                    ),
                    color = if (device.connected) statusContentColor(AccountSyncIndicator.GREEN)
                        else MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        SyncMetadata(
            label = stringResource(Res.string.sync_status_device_version_label),
            value = device.lastConfirmedRevision?.toString() ?: stringResource(Res.string.sync_status_unknown),
        )
        SyncMetadata(
            label = stringResource(Res.string.sync_status_last_sync_label),
            value = device.lastSuccessfulSyncAtMillis?.asFormattedDate(DateFormatPattern.FULL)
                ?: stringResource(Res.string.sync_status_device_never_synced),
        )
    }
}

@Composable
private fun SyncHistorySection(history: AccountSyncHistoryState, devices: List<AccountSyncDevice>) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        SyncSectionHeading(stringResource(Res.string.sync_status_history_title))
        when (history) {
            AccountSyncHistoryState.Loading -> SyncCard {
                SyncSecondaryText(stringResource(Res.string.sync_status_history_loading))
                LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
            }
            AccountSyncHistoryState.Failed -> SyncCard {
                Text(
                    text = stringResource(Res.string.sync_status_history_failed),
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
            is AccountSyncHistoryState.Loaded -> {
                if (history.entries.isEmpty()) {
                    SyncCard { SyncSecondaryText(stringResource(Res.string.sync_status_history_empty)) }
                }
                history.entries.forEach { entry -> SyncHistoryCard(entry, devices) }
            }
        }
    }
}

@Composable
private fun SyncHistoryCard(entry: SyncHistoryItem, devices: List<AccountSyncDevice>) {
    SyncCard {
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.Top,
        ) {
            Text(
                text = entry.action.replace('_', ' ').lowercase().replaceFirstChar(Char::uppercase),
                style = MaterialTheme.typography.titleSmall,
                modifier = Modifier.weight(1f),
            )
            SyncBadge(
                text = stringResource(Res.string.sync_status_version_badge, entry.revision),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Text(entry.deviceName(devices), style = MaterialTheme.typography.bodyMedium)
        SyncSecondaryText(entry.occurredAtMillis.asFormattedDate(DateFormatPattern.FULL))
        SyncMetadata(
            label = stringResource(Res.string.sync_status_affected_label),
            value = entry.affectedItemsSummary(),
        )
    }
}

@Composable
private fun SyncSectionHeading(title: String, count: Int? = null) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(title, style = MaterialTheme.typography.titleSmall, modifier = Modifier.weight(1f))
        if (count != null) {
            SyncBadge(count.toString(), MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun SyncCard(content: @Composable ColumnScope.() -> Unit) {
    Surface(
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
        shape = RoundedCornerShape(12.dp),
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
            content = content,
        )
    }
}

@Composable
private fun SyncMetadata(label: String, value: String) {
    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
        SyncSecondaryText(label)
        Text(value, style = MaterialTheme.typography.bodyMedium)
    }
}

@Composable
private fun SyncSecondaryText(text: String) {
    Text(
        text = text,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        style = MaterialTheme.typography.bodySmall,
    )
}

@Composable
private fun SyncBadge(text: String, color: Color) {
    Surface(color = color.copy(alpha = 0.12f), contentColor = color, shape = RoundedCornerShape(6.dp)) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelSmall,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
        )
    }
}

private fun statusColor(indicator: AccountSyncIndicator): Color = when (indicator) {
    AccountSyncIndicator.HIDDEN, AccountSyncIndicator.GRAY -> unavailableColor
    AccountSyncIndicator.GREEN -> synchronizedColor
    AccountSyncIndicator.YELLOW, AccountSyncIndicator.SYNCING -> pendingColor
    AccountSyncIndicator.RED -> failureColor
}

@Composable
private fun statusContentColor(indicator: AccountSyncIndicator): Color {
    val dark = MaterialTheme.colorScheme.surface.luminance() < 0.5f
    return when (indicator) {
        AccountSyncIndicator.GREEN -> if (dark) Color(0xFF81C784) else Color(0xFF1B5E20)
        AccountSyncIndicator.YELLOW, AccountSyncIndicator.SYNCING -> if (dark) Color(0xFFFFD54F) else Color(0xFF795500)
        AccountSyncIndicator.RED -> MaterialTheme.colorScheme.error
        AccountSyncIndicator.HIDDEN, AccountSyncIndicator.GRAY -> MaterialTheme.colorScheme.onSurfaceVariant
    }
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

@Preview(name = "Sync details · Light", widthDp = 420, heightDp = 860)
@Composable
private fun AccountSyncDetailsDialogPreview() {
    AccountSyncDetailsPreviewContent(darkTheme = false)
}

@Preview(name = "Sync details · Dark", widthDp = 420, heightDp = 860)
@Composable
private fun AccountSyncDetailsDialogDarkPreview() {
    AccountSyncDetailsPreviewContent(darkTheme = true)
}

@Preview(name = "Sync details · Compact / offline", widthDp = 320, heightDp = 640, fontScale = 1.2f)
@Composable
private fun AccountSyncDetailsDialogCompactPreview() {
    AccountSyncDetailsPreviewContent(darkTheme = false, unavailable = true)
}

@Preview(name = "Sync details · Conflict", widthDp = 420, heightDp = 860)
@Composable
private fun AccountSyncDetailsDialogConflictPreview() {
    AccountSyncDetailsPreviewContent(darkTheme = true, conflict = true)
}

@Composable
private fun AccountSyncDetailsPreviewContent(
    darkTheme: Boolean,
    unavailable: Boolean = false,
    conflict: Boolean = false,
) {
    val devices = listOf(
        AccountSyncDevice("desktop", "Home laptop", "Desktop", !unavailable, 128L, PREVIEW_SYNC_TIME, isCurrentDevice = true),
        AccountSyncDevice("tablet", "My tablet", "Android", !unavailable, 128L, PREVIEW_SYNC_TIME),
        AccountSyncDevice("phone", "My Android phone", "Android", false, null, null),
    )
    MainTheme(darkTheme = darkTheme) {
        AccountSyncDetailsDialog(
            status = AccountSyncStatus(
                indicator = when {
                    unavailable -> AccountSyncIndicator.GRAY
                    conflict -> AccountSyncIndicator.RED
                    else -> AccountSyncIndicator.YELLOW
                },
                confirmedRevision = 128L,
                serverRevision = if (unavailable) null else 130L,
                pendingOperationCount = 3,
                hasConflict = conflict,
                devices = devices,
            ),
            history = if (unavailable) AccountSyncHistoryState.Failed else AccountSyncHistoryState.Loaded(
                listOf(
                    SyncHistoryItem("preview-1", 130L, "desktop", "UPSERT_CARD", listOf("card-24a7b92e"), PREVIEW_SYNC_TIME),
                    SyncHistoryItem("preview-2", 129L, "desktop", "UPSERT_DECK", listOf("deck-18f7c64a"), PREVIEW_SYNC_TIME - 60_000L),
                )
            ),
            onDismiss = {},
            onSynchronize = {},
            onResolve = {},
        )
    }
}
