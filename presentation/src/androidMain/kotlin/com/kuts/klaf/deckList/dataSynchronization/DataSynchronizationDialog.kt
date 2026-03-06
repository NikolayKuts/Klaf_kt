package com.kuts.klaf.deckList.dataSynchronization

import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.keyframes
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.navigation.NavBackStackEntry
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import com.kuts.domain.common.AuthenticationAction
import com.kuts.domain.common.IDataSynchronizationState
import com.kuts.domain.common.IDataSynchronizationState.Failed
import com.kuts.domain.common.IDataSynchronizationState.Initial
import com.kuts.domain.common.IDataSynchronizationState.SuccessfullyFinished
import com.kuts.domain.common.IDataSynchronizationState.Synchronizing
import com.kuts.domain.common.IDataSynchronizationState.Uncertain
import com.kuts.domain.common.ifNotNull
import com.kuts.klaf.authentication.AuthenticationActionResult
import com.kuts.klaf.common.BaseMainViewModel
import com.kuts.klaf.common.ClosingButton
import com.kuts.klaf.common.ContentHolder
import com.kuts.klaf.common.EventMessage
import com.kuts.klaf.common.EventMessageView
import com.kuts.klaf.common.FullBackgroundDialog
import com.kuts.klaf.common.ROUNDED_ELEMENT_SIZE
import com.kuts.klaf.common.RoundButton
import com.kuts.klaf.common.RoundedIcon
import com.kuts.klaf.common.ScrollableBox
import com.kuts.klaf.common.WarningMessage
import com.kuts.klaf.common.noRippleClickable
import com.kuts.klaf.deckList.common.AnimatedSynchronizationLabel
import com.kuts.klaf.deckList.common.BaseDeckListViewModel
import com.kuts.klaf.deckList.common.SynchronizationLabel
import com.kuts.klaf.presentation.resources.*
import com.kuts.klaf.theme.MainTheme
import org.jetbrains.compose.resources.stringResource
import org.koin.androidx.compose.koinViewModel

@Composable
internal fun DataSynchronizationDialog(
    navController: NavHostController,
    backStackEntry: NavBackStackEntry,
    sharedViewModel: BaseMainViewModel,
    authenticationActionResult: AuthenticationActionResult?,
) {
    val owner = remember(backStackEntry) {
        navController.getBackStackEntry(navController.graph.findStartDestination().id)
    }
    val viewModel: BaseDeckListViewModel = koinViewModel(viewModelStoreOwner = owner)

    val eventMessage by sharedViewModel.eventMessage.collectAsState(initial = null)

    DataSynchronizationDialogContent(
        synchronizationState = viewModel.dataSynchronizationState.collectAsState().value,
        onConfirmClick = viewModel::synchronizeData,
        onCloseClick = { navController.popBackStack() },
        onDispose = viewModel::resetSynchronizationState,
        eventMassage = eventMessage,
        onLaunched = {
            if (authenticationActionResult?.isSuccessful != true) {
                return@DataSynchronizationDialogContent
            }

            val messageId = when (authenticationActionResult.action) {
                AuthenticationAction.SIGN_IN -> {
                    Res.string.authentication_sign_in_success
                }

                AuthenticationAction.SIGN_UP -> {
                    Res.string.authentication_sign_up_success
                }
            }

            sharedViewModel.notify(
                message = EventMessage(
                    resId = messageId,
                    type = EventMessage.Type.Positive,
                )
            )
        },
    )
}

@Composable
private fun DataSynchronizationDialogContent(
    synchronizationState: IDataSynchronizationState,
    onConfirmClick: () -> Unit,
    onCloseClick: () -> Unit,
    onDispose: () -> Unit,
    eventMassage: EventMessage?,
    onLaunched: () -> Unit,
) {
    ScrollableBox(
        modifier = Modifier.noRippleClickable { onCloseClick() },
        dialogMode = true,
        eventContent = {
            eventMassage.ifNotNull { EventMessageView(message = it) }
        }
    ) {
        when (synchronizationState) {
            Uncertain -> {}
            Initial -> {
                InitialStateView(
                    onCloseClick = onCloseClick,
                    onConfirmClick = onConfirmClick
                )
            }

            is Synchronizing -> {
                SynchronizationStateView(synchronizationData = synchronizationState.synchronizationData)
            }

            SuccessfullyFinished -> {
                FinishStateView(onCloseClick = onCloseClick)
            }

            Failed -> {
                FailureStateView(
                    onResynchronizeClick = onConfirmClick,
                    onCloseClick = onCloseClick
                )
            }
        }
        DisposableEffect(key1 = null) {
            onDispose { onDispose() }
        }

        LaunchedEffect(key1 = null) { onLaunched() }
    }
}

@Composable
private fun InitialStateView(
    onCloseClick: () -> Unit,
    onConfirmClick: () -> Unit,
) {
    FullBackgroundDialog(
        onBackgroundClick = onCloseClick,
        topContent = ContentHolder(size = ROUNDED_ELEMENT_SIZE.dp) {
            SynchronizationLabel()
        },
        mainContent = {
            Text(
                style = MainTheme.typographies.dialogTextStyle,
                text = stringResource(resource = Res.string.data_synchronization_dialog_title)
            )
        },
        bottomContent = {
            RoundButton(
                background = MainTheme.colors.common.positiveDialogButton,
                iconRes = Res.drawable.ic_confirmation_24,
                onClick = onConfirmClick
            )

            RoundButton(
                background = MainTheme.colors.common.neutralDialogButton,
                iconRes = Res.drawable.ic_close_24,
                onClick = onCloseClick
            )
        }
    )
}

@Composable
private fun SynchronizationStateView(synchronizationData: String) {
    FullBackgroundDialog(
        onBackgroundClick = { },
        topContent = ContentHolder(size = ROUNDED_ELEMENT_SIZE.dp) { AnimatedSynchronizationLabel() },
        mainContent = {
            Column(modifier = Modifier.width(IntrinsicSize.Max)) {
                WarningMessage(textRes = Res.string.data_synchronization_dialog_waiting_message)
                ContentSpacer()
                SynchronizingText()

                if (synchronizationData.isNotEmpty()) {
                    ContentSpacer()
                    Text(
                        modifier = Modifier.fillMaxWidth(),
                        text = synchronizationData,
                        overflow = TextOverflow.Ellipsis,
                        maxLines = 1
                    )
                }
            }
        },
    )
}

@Composable
private fun FinishStateView(onCloseClick: () -> Unit) {
    FullBackgroundDialog(
        onBackgroundClick = onCloseClick,
        topContent = ContentHolder(size = ROUNDED_ELEMENT_SIZE.dp) {
            RoundedIcon(
                background = MainTheme.colors.common.positiveDialogButton,
                iconRes = Res.drawable.ic_confirmation_24,
            )
        },
        mainContent = {
            Text(
                style = MainTheme.typographies.dialogTextStyle,
                text = stringResource(resource = Res.string.data_synchronization_dialog_data_synchronized)
            )
        },
        bottomContent = {
            RoundButton(
                background = MainTheme.colors.common.neutralDialogButton,
                iconRes = Res.drawable.ic_close_24,
                onClick = onCloseClick
            )
        }
    )
}

@Composable
private fun FailureStateView(
    onResynchronizeClick: () -> Unit,
    onCloseClick: () -> Unit,
) {
    FullBackgroundDialog(
        mainContentModifier = Modifier,
        onBackgroundClick = onCloseClick,
        topContent = ContentHolder(size = ROUNDED_ELEMENT_SIZE.dp) {
            RoundedIcon(
                background = MainTheme.colors.common.negativeDialogButton,
                iconRes = Res.drawable.ic_attention_mark_24,
            )
        },
        mainContent = {
            Column {
                WarningMessage(textRes = Res.string.data_synchronization_dialog_failure_message)
                ContentSpacer()
                Text(
                    modifier = Modifier.padding(6.dp),
                    text = stringResource(resource = Res.string.data_synchronization_dialog_resync_question),
                    style = MainTheme.typographies.dialogTextStyle
                )
            }
        },
        bottomContent = {
            RoundButton(
                background = MainTheme.colors.common.positiveDialogButton,
                iconRes = Res.drawable.ic_sync_24,
                onClick = onResynchronizeClick,
            )

            ClosingButton(onClick = onCloseClick)
        },
    )
}

@Composable
private fun SynchronizingText() {
    val animationDuration = 700
    val stepDuration = animationDuration / 3

    val alpha1 by animateAlphaWithDelay(
        delay = 50,
        commonDuration = animationDuration,
        stepDuration = stepDuration
    )
    val alpha2 by animateAlphaWithDelay(
        delay = stepDuration,
        commonDuration = animationDuration,
        stepDuration = stepDuration
    )
    val alpha3 by animateAlphaWithDelay(
        delay = stepDuration * 2,
        commonDuration = animationDuration,
        stepDuration = stepDuration
    )

    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center
    ) {
        Text(
            style = MainTheme.typographies.dialogTextStyle,
            text = stringResource(resource = Res.string.data_synchronization_dialog_sync_process)
        )
        TextDot(alpha1)
        TextDot(alpha2)
        TextDot(alpha3)
    }
}

@Composable
private fun TextDot(alpha: Float) {
    Text(
        modifier = Modifier.alpha(alpha),
        text = "."
    )
}

@Composable
private fun animateAlphaWithDelay(
    delay: Int,
    commonDuration: Int,
    stepDuration: Int,
    minAlpha: Float = 0.0f,
    maxAlpha: Float = 1F,
): State<Float> = rememberInfiniteTransition().animateFloat(
    initialValue = minAlpha,
    targetValue = minAlpha,
    animationSpec = infiniteRepeatable(
        animation = keyframes {
            durationMillis = commonDuration
            minAlpha at 0
            minAlpha at delay
            maxAlpha at delay + (stepDuration / 2)
            maxAlpha at commonDuration
        }
    )
)

@Composable
private fun ContentSpacer() {
    Spacer(modifier = Modifier.height(16.dp))
}
