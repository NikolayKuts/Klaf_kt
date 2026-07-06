package com.kuts.klaf.cardManagement.cardEditing

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavBackStackEntry
import androidx.navigation.NavHostController
import com.kuts.klaf.cardManagement.cardAddition.CardManagementContent
import com.kuts.klaf.cardManagement.common.CardManagementState
import com.kuts.klaf.common.BaseMainViewModel
import com.kuts.klaf.common.WordInsightsBottomSheetContent
import com.kuts.klaf.navigation.AppDestination
import com.kuts.klaf.navigation.CollectFlowWithLifecycle
import com.kuts.klaf.navigation.ObserveAudioLifecycle
import com.kuts.klaf.presentation.resources.*
import com.kuts.klaf.theme.MainTheme
import com.kuts.klaf.webContent.WebContentSource
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf

@Composable
internal fun CardEditingScreen(
    navController: NavHostController,
    backStackEntry: NavBackStackEntry,
    sharedViewModel: BaseMainViewModel,
    deckId: Int,
    cardId: Int,
) {
    val viewModel: CardEditingViewModel = koinViewModel(
        viewModelStoreOwner = backStackEntry,
        parameters = { parametersOf(deckId, cardId) },
    )

    ObserveAudioLifecycle(
        onCreate = viewModel.audioPlayer::onCreate,
        onResume = viewModel.audioPlayer::onResume,
        onStop = viewModel.audioPlayer::onStop,
        onDestroy = viewModel.audioPlayer::onDestroy,
    )

    CollectFlowWithLifecycle(flow = viewModel.eventMessage, onEach = sharedViewModel::notify)

    CollectFlowWithLifecycle(flow = viewModel.cardManagementState) { managementState ->
        if (managementState is CardManagementState.Finished) {
            navController.popBackStack()
        }
    }

    Surface {
        CardEditingContent(
            viewModel = viewModel,
            onYouGlishClick = { word ->
                viewModel.hideInsightsSheet()
                navController.navigate(
                    route = AppDestination.WebContent(
                        source = WebContentSource.YouGlish(word = word),
                    )
                )
            },
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CardEditingContent(
    viewModel: CardEditingViewModel,
    onYouGlishClick: (String) -> Unit,
) {
    val insightsUiState by viewModel.insightsUiState.collectAsState()
    val sheetState = rememberModalBottomSheetState()

    Box(modifier = Modifier.fillMaxSize()) {
        CardManagementContent(
            viewModel = viewModel,
            isCambridgeBottomSheetEnabled = false,
        )

        InsightsSheetHandle(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 12.dp),
            status = insightsUiState.status,
            isEnabled = insightsUiState.isExpandable,
            onClick = viewModel::showInsightsSheet,
        )
    }

    if (insightsUiState.isSheetVisible && insightsUiState.isExpandable) {
        ModalBottomSheet(
            onDismissRequest = viewModel::hideInsightsSheet,
            sheetState = sheetState,
        ) {
            if (insightsUiState.status == CardEditingInsightsStatus.Success) {
                WordInsightsBottomSheetContent(
                    word = insightsUiState.word,
                    meanings = insightsUiState.meanings,
                    refreshedMeanings = insightsUiState.refreshedMeanings,
                    refreshedErrorMessageResId = insightsUiState.refreshedErrorMessageResId,
                    isRefreshing = insightsUiState.isRefreshing,
                    isApplyingRefreshed = insightsUiState.isApplyingRefreshed,
                    canRequestRefreshedInsights = insightsUiState.canRequestRefreshedInsights,
                    canApplyRefreshedInsights = insightsUiState.canApplyRefreshedInsights,
                    onRequestRefreshedInsights = viewModel::requestRefreshedInsights,
                    onApplyRefreshedInsights = viewModel::applyRefreshedInsights,
                    onYouGlishClick = {
                        insightsUiState.word
                            .takeIf(String::isNotBlank)
                            ?.let(onYouGlishClick)
                    },
                )
            } else {
                InsightsErrorBottomSheetContent(
                    word = insightsUiState.word,
                    errorMessageResId = insightsUiState.errorMessageResId,
                )
            }
        }
    }
}

@Composable
private fun InsightsSheetHandle(
    modifier: Modifier = Modifier,
    status: CardEditingInsightsStatus,
    isEnabled: Boolean,
    onClick: () -> Unit,
) {
    val wordInsightsColors = MainTheme.colors.wordInsightsBottomSheet

    val containerColor = when (status) {
        CardEditingInsightsStatus.Idle -> wordInsightsColors.editingHandleIdleContainer
        CardEditingInsightsStatus.Loading -> wordInsightsColors.editingHandleLoadingContainer
        CardEditingInsightsStatus.Success -> wordInsightsColors.editingHandleSuccessContainer
        CardEditingInsightsStatus.Error -> wordInsightsColors.editingHandleErrorContainer
    }
    val indicatorColor = when (status) {
        CardEditingInsightsStatus.Idle -> wordInsightsColors.editingHandleIdleIndicator
        CardEditingInsightsStatus.Loading -> wordInsightsColors.editingHandleLoadingIndicator
        CardEditingInsightsStatus.Success -> wordInsightsColors.editingHandleSuccessIndicator
        CardEditingInsightsStatus.Error -> wordInsightsColors.editingHandleErrorIndicator
    }
    val handleModifier = modifier
        .background(
            color = containerColor,
            shape = RoundedCornerShape(16.dp),
        )
        .let { currentModifier ->
            if (isEnabled) {
                currentModifier.clickable(onClick = onClick)
            } else {
                currentModifier
            }
        }
        .padding(horizontal = 16.dp, vertical = 10.dp)

    Box(
        modifier = handleModifier,
        contentAlignment = Alignment.Center,
    ) {
        if (status == CardEditingInsightsStatus.Loading) {
            InsightsLoadingDots(color = indicatorColor)
        } else {
            Box(
                modifier = Modifier
                    .size(width = 42.dp, height = 4.dp)
                    .background(
                        color = indicatorColor,
                        shape = RoundedCornerShape(50),
                    ),
            )
        }
    }
}

@Composable
private fun InsightsLoadingDots(color: Color) {
    val transition = rememberInfiniteTransition(label = "insights-handle-loading")
    val firstDotAlpha by transition.animateFloat(
        initialValue = 0.25f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 600, delayMillis = 0, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "insights-first-dot",
    )
    val secondDotAlpha by transition.animateFloat(
        initialValue = 0.25f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 600, delayMillis = 140, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "insights-second-dot",
    )
    val thirdDotAlpha by transition.animateFloat(
        initialValue = 0.25f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 600, delayMillis = 280, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "insights-third-dot",
    )

    Row(
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        InsightsLoadingDot(color = color, alpha = firstDotAlpha)
        InsightsLoadingDot(color = color, alpha = secondDotAlpha)
        InsightsLoadingDot(color = color, alpha = thirdDotAlpha)
    }
}

@Composable
private fun InsightsLoadingDot(color: Color, alpha: Float) {
    Box(
        modifier = Modifier
            .size(6.dp)
            .background(
                color = color.copy(alpha = alpha),
                shape = CircleShape,
            ),
    )
}

@Composable
private fun InsightsErrorBottomSheetContent(
    word: String,
    errorMessageResId: StringResource?,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Text(
            text = stringResource(resource = Res.string.word_insights_title),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
        )

        if (word.isNotBlank()) {
            Text(
                text = word,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.SemiBold,
            )
        }

        Text(
            text = stringResource(resource = Res.string.word_insights_loading_error_message),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        Text(
            text = errorMessageResId?.let { stringResource(resource = it) }
                ?: stringResource(resource = Res.string.word_insights_unknown_request_error),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.error,
        )
    }
}
