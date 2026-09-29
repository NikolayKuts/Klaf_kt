package com.kuts.klaf.deckList.conflictResolution

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.kuts.domain.common.ConflictResolutionAction
import com.kuts.domain.common.ConflictResolutionDecision
import com.kuts.domain.entities.Deck
import com.kuts.klaf.presentation.resources.Res
import com.kuts.klaf.presentation.resources.sync_conflicts_accept_server
import com.kuts.klaf.presentation.resources.sync_conflicts_close
import com.kuts.klaf.presentation.resources.sync_conflicts_history
import com.kuts.klaf.presentation.resources.sync_conflicts_keep_local_card
import com.kuts.klaf.presentation.resources.sync_conflicts_keep_local_source
import com.kuts.klaf.presentation.resources.sync_conflicts_keep_local_deck
import com.kuts.klaf.presentation.resources.sync_conflicts_keep_removal_due_now
import com.kuts.klaf.presentation.resources.sync_conflicts_keep_removal_schedule
import com.kuts.klaf.presentation.resources.sync_conflicts_local
import com.kuts.klaf.presentation.resources.sync_conflicts_apply_selected
import com.kuts.klaf.presentation.resources.sync_conflicts_choose_each
import com.kuts.klaf.presentation.resources.sync_conflicts_rescue_card
import com.kuts.klaf.presentation.resources.sync_conflicts_restore_deck
import com.kuts.klaf.presentation.resources.sync_conflicts_retarget_card
import com.kuts.klaf.presentation.resources.sync_conflicts_new_deck_name
import com.kuts.klaf.presentation.resources.sync_conflicts_create_destination
import com.kuts.klaf.presentation.resources.sync_conflicts_choose_destination
import com.kuts.klaf.presentation.resources.sync_conflicts_revision
import com.kuts.klaf.presentation.resources.sync_conflicts_server
import com.kuts.klaf.presentation.resources.sync_conflicts_title
import com.kuts.klaf.presentation.resources.sync_conflicts_unsupported
import org.jetbrains.compose.resources.stringResource

private fun ConflictResolutionAction.label() = when (this) {
    ConflictResolutionAction.ACCEPT_SERVER -> Res.string.sync_conflicts_accept_server
    ConflictResolutionAction.KEEP_LOCAL_DECK -> Res.string.sync_conflicts_keep_local_deck
    ConflictResolutionAction.KEEP_LOCAL_CARD -> Res.string.sync_conflicts_keep_local_card
    ConflictResolutionAction.KEEP_LOCAL_SOURCE -> Res.string.sync_conflicts_keep_local_source
    ConflictResolutionAction.RESCUE_MOVED_CARD -> Res.string.sync_conflicts_rescue_card
    ConflictResolutionAction.RESTORE_DELETED_DECK -> Res.string.sync_conflicts_restore_deck
    ConflictResolutionAction.KEEP_REMOVAL_RETAIN_SCHEDULE -> Res.string.sync_conflicts_keep_removal_schedule
    ConflictResolutionAction.KEEP_REMOVAL_DUE_NOW -> Res.string.sync_conflicts_keep_removal_due_now
    ConflictResolutionAction.RETARGET_MOVED_CARD -> Res.string.sync_conflicts_retarget_card
}

/** Presentation only: its host must use the account-scoped Room/REST coordinator. */
@Composable
fun ConflictResolutionScreen(
    model: ConflictResolutionUiModel,
    destinations: List<ConflictDestination>,
    isWorking: Boolean,
    error: String?,
    onAction: (ConflictResolutionAction) -> Unit,
    onRetarget: (targetDeckSyncId: String?, newDeckName: String?) -> Unit,
    onSelectedActions: (List<ConflictResolutionDecision>) -> Unit,
    onClose: () -> Unit,
) {
    var selectedActions by remember(model.revision, model.conflicts.map { it.operationId }) {
        mutableStateOf<Map<String, ConflictResolutionAction>>(emptyMap())
    }
    var isChoosingDestination by remember(model.revision, model.conflicts.map { it.operationId }) {
        mutableStateOf(false)
    }
    var newDeckName by remember(model.revision, model.conflicts.map { it.operationId }) {
        mutableStateOf("")
    }
    Column(
        modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(text = stringResource(Res.string.sync_conflicts_title))
        Text(text = stringResource(Res.string.sync_conflicts_revision, model.revision))
        model.conflicts.forEach { conflict ->
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Text(text = stringResource(Res.string.sync_conflicts_local))
                    Text(text = conflict.localDescription)
                    Text(text = stringResource(Res.string.sync_conflicts_server))
                    Text(text = conflict.serverDescription)
                    if (conflict.serverChanges.isNotEmpty()) {
                        Text(text = stringResource(Res.string.sync_conflicts_history))
                        conflict.serverChanges.forEach { change -> Text(text = change) }
                    }
                    if (model.manualSelectionAvailable) {
                        conflict.availableActions.sortedBy(ConflictResolutionAction::ordinal).forEach { action ->
                            OutlinedButton(
                                onClick = {
                                    selectedActions = selectedActions + (conflict.operationId to action)
                                },
                                enabled = !isWorking,
                                modifier = Modifier.fillMaxWidth(),
                            ) {
                                val selected = selectedActions[conflict.operationId] == action
                                Text(text = (if (selected) "✓ " else "○ ") + stringResource(action.label()))
                            }
                        }
                    }
                }
            }
        }
        if (model.manualSelectionAvailable) {
            Text(text = stringResource(Res.string.sync_conflicts_choose_each))
            Button(
                onClick = {
                    onSelectedActions(model.conflicts.map { conflict ->
                        ConflictResolutionDecision(conflict.operationId, selectedActions.getValue(conflict.operationId))
                    })
                },
                enabled = !isWorking && selectedActions.keys == model.conflicts.map { it.operationId }.toSet(),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(text = stringResource(Res.string.sync_conflicts_apply_selected))
            }
        } else if (model.availableActions.size == 1) {
            Text(text = stringResource(Res.string.sync_conflicts_unsupported))
        }
        error?.let { Text(text = it) }
        if (isChoosingDestination) {
            Text(text = stringResource(Res.string.sync_conflicts_choose_destination))
            destinations.forEach { destination ->
                OutlinedButton(
                    onClick = { onRetarget(destination.syncId, null) },
                    enabled = !isWorking,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(text = destination.name)
                }
            }
            OutlinedTextField(
                value = newDeckName,
                onValueChange = { newDeckName = it },
                label = { Text(stringResource(Res.string.sync_conflicts_new_deck_name)) },
                enabled = !isWorking,
                modifier = Modifier.fillMaxWidth(),
            )
            Button(
                onClick = { onRetarget(null, newDeckName.trim()) },
                enabled = !isWorking && newDeckName.trim().length in 1..Deck.MAX_NAME_LENGTH,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(text = stringResource(Res.string.sync_conflicts_create_destination))
            }
        }
        model.availableActions.sortedBy(ConflictResolutionAction::ordinal).forEach { action ->
            Button(
                onClick = {
                    if (action == ConflictResolutionAction.RETARGET_MOVED_CARD) isChoosingDestination = true
                    else onAction(action)
                },
                enabled = !isWorking,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(text = stringResource(action.label()))
            }
        }
        OutlinedButton(onClick = onClose, modifier = Modifier.fillMaxWidth()) {
            Text(text = stringResource(Res.string.sync_conflicts_close))
        }
    }
}
