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
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.kuts.klaf.cardManagement.cardAddition.CardManagementScreen
import com.kuts.klaf.common.WordInsightsBottomSheetContent
import com.kuts.klaf.presentation.R
import com.kuts.klaf.theme.MainTheme

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun CardEditingScreen(viewModel: CardEditingViewModel) {
    val insightsUiState by viewModel.insightsUiState.collectAsState()
    val sheetState = rememberModalBottomSheetState()

    Box(modifier = Modifier.fillMaxSize()) {
        CardManagementScreen(
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
    errorMessageResId: Int?,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Text(
            text = stringResource(id = R.string.word_insights_title),
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
            text = stringResource(id = R.string.word_insights_loading_error_message),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        Text(
            text = errorMessageResId?.let { stringResource(id = it) }
                ?: stringResource(id = R.string.word_insights_unknown_request_error),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.error,
        )
    }
}
