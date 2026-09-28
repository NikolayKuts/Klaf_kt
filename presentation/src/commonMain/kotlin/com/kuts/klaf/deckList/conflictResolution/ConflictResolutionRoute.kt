package com.kuts.klaf.deckList.conflictResolution

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.OutlinedButton
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
import com.kuts.klaf.deckList.common.AccountSyncOutcome
import com.kuts.klaf.presentation.resources.Res
import com.kuts.klaf.presentation.resources.sync_conflicts_close
import com.kuts.klaf.presentation.resources.sync_conflicts_none
import com.kuts.klaf.presentation.resources.sync_conflicts_resolution_failed
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.koinInject

@Composable
fun ConflictResolutionRoute(
    onClose: () -> Unit,
    gateway: AccountConflictGateway = koinInject(),
) {
    val scope = rememberCoroutineScope()
    val failureText = stringResource(Res.string.sync_conflicts_resolution_failed)
    var snapshot by remember { mutableStateOf<AccountConflictSnapshot?>(null) }
    var isLoading by remember { mutableStateOf(true) }
    var isWorking by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(gateway) {
        try {
            snapshot = gateway.current()
        } catch (failure: CancellationException) {
            throw failure
        } catch (_: Exception) {
            error = failureText
        } finally {
            isLoading = false
        }
    }

    val current = snapshot
    if (current != null) {
        ConflictResolutionScreen(
            model = current.response.toConflictResolutionUiModel(current.deckNames, current.cardNames),
            destinations = current.destinations,
            isWorking = isWorking,
            error = error,
            onAction = { action ->
                scope.launch {
                    isWorking = true
                    error = null
                    try {
                        when (gateway.resolve(current.accountEmail, action)) {
                            AccountSyncOutcome.APPLIED -> onClose()
                            AccountSyncOutcome.NEEDS_RESOLUTION -> {
                                snapshot = gateway.current()
                                if (snapshot == null) error = failureText
                            }
                        }
                    } catch (failure: CancellationException) {
                        throw failure
                    } catch (_: Exception) {
                        error = failureText
                    } finally {
                        isWorking = false
                    }
                }
            },
            onRetarget = { targetDeckSyncId, newDeckName ->
                scope.launch {
                    isWorking = true
                    error = null
                    try {
                        when (gateway.resolveMovedCard(current.accountEmail, targetDeckSyncId, newDeckName)) {
                            AccountSyncOutcome.APPLIED -> onClose()
                            AccountSyncOutcome.NEEDS_RESOLUTION -> {
                                snapshot = gateway.current()
                                if (snapshot == null) error = failureText
                            }
                        }
                    } catch (failure: CancellationException) {
                        throw failure
                    } catch (_: Exception) {
                        error = failureText
                    } finally {
                        isWorking = false
                    }
                }
            },
            onSelectedActions = { decisions ->
                scope.launch {
                    isWorking = true
                    error = null
                    try {
                        when (gateway.resolveSelected(current.accountEmail, decisions)) {
                            AccountSyncOutcome.APPLIED -> onClose()
                            AccountSyncOutcome.NEEDS_RESOLUTION -> {
                                snapshot = gateway.current()
                                if (snapshot == null) error = failureText
                            }
                        }
                    } catch (failure: CancellationException) {
                        throw failure
                    } catch (_: Exception) {
                        error = failureText
                    } finally {
                        isWorking = false
                    }
                }
            },
            onClose = onClose,
        )
    } else {
        Column(
            modifier = Modifier.fillMaxSize().padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            if (isLoading) {
                CircularProgressIndicator()
            } else {
                Text(text = error ?: stringResource(Res.string.sync_conflicts_none))
                OutlinedButton(onClick = onClose, modifier = Modifier.fillMaxWidth()) {
                    Text(text = stringResource(Res.string.sync_conflicts_close))
                }
            }
        }
    }
}
