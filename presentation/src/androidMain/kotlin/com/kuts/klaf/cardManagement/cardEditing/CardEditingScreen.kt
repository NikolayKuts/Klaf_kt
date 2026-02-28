package com.kuts.klaf.cardManagement.cardEditing

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
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
import com.kuts.domain.entities.CefrLevel
import com.kuts.domain.entities.WordMeaningItem
import com.kuts.klaf.cardManagement.cardAddition.CardManagementScreen

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
                InsightsBottomSheetContent(
                    word = insightsUiState.word,
                    meanings = insightsUiState.meanings,
                    refreshedMeanings = insightsUiState.refreshedMeanings,
                    refreshedErrorMessage = insightsUiState.refreshedErrorMessage,
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
                    errorMessage = insightsUiState.errorMessage,
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
    val containerColor = when (status) {
        CardEditingInsightsStatus.Idle -> Color(0xFF1A1A1A)
        CardEditingInsightsStatus.Loading -> Color(0xFF1A1A1A)
        CardEditingInsightsStatus.Success -> Color(0x664CAF50)
        CardEditingInsightsStatus.Error -> Color(0x66E53935)
    }
    val indicatorColor = when (status) {
        CardEditingInsightsStatus.Idle -> Color(0xFFB6B6B6)
        CardEditingInsightsStatus.Loading -> MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.9f)
        CardEditingInsightsStatus.Success -> Color(0xFF66BB6A)
        CardEditingInsightsStatus.Error -> Color(0xFFEF5350)
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
    errorMessage: String,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Text(
            text = "Word insights",
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
            text = "Unable to load insights for this word.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        Text(
            text = errorMessage.ifBlank { "Unknown request error." },
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.error,
        )
    }
}

@Composable
private fun InsightsBottomSheetContent(
    word: String,
    meanings: List<WordMeaningItem>,
    refreshedMeanings: List<WordMeaningItem>,
    refreshedErrorMessage: String,
    isRefreshing: Boolean,
    isApplyingRefreshed: Boolean,
    canRequestRefreshedInsights: Boolean,
    canApplyRefreshedInsights: Boolean,
    onRequestRefreshedInsights: () -> Unit,
    onApplyRefreshedInsights: () -> Unit,
) {
    val refreshedSectionShape = RoundedCornerShape(14.dp)
    val refreshedSectionBorderColor = MaterialTheme.colorScheme.error.copy(alpha = 0.75f)

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = "Word",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.72f),
            )

            if (word.isNotBlank()) {
                Box(
                    modifier = Modifier
                        .background(
                            color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.45f),
                            shape = RoundedCornerShape(8.dp),
                        )
                        .padding(horizontal = 10.dp, vertical = 4.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = word,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.SemiBold,
                    )
                }
            }
        }

        Text(
            text = "Meanings are ordered by usage frequency.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.78f),
        )

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(max = 520.dp)
                .verticalScroll(state = rememberScrollState())
                .padding(bottom = 16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Text(
                text = "Current Saved Variant",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.85f),
            )

            meanings.forEachIndexed { index, meaning ->
                InsightMeaningSection(
                    index = index,
                    meaning = meaning,
                )
            }

            OutlinedButton(
                modifier = Modifier.fillMaxWidth(),
                enabled = canRequestRefreshedInsights,
                onClick = onRequestRefreshedInsights,
            ) {
                if (isRefreshing) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(16.dp),
                        strokeWidth = 2.dp,
                    )
                    Spacer(modifier = Modifier.size(8.dp))
                    Text(text = "Loading...")
                } else {
                    Text(text = "Load New Variant")
                }
            }

            if (refreshedErrorMessage.isNotBlank()) {
                Text(
                    text = refreshedErrorMessage,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error,
                )
            }

            if (refreshedMeanings.isNotEmpty()) {
                HorizontalDivider(color = MaterialTheme.colorScheme.error.copy(alpha = 0.6f))

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(
                            width = 1.dp,
                            color = refreshedSectionBorderColor,
                            shape = refreshedSectionShape,
                        )
                        .padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    Text(
                        text = "New Variant Preview",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.error,
                    )

                    refreshedMeanings.forEachIndexed { index, meaning ->
                        InsightMeaningSection(
                            index = index,
                            meaning = meaning,
                        )
                    }

                    Button(
                        modifier = Modifier.fillMaxWidth(),
                        enabled = canApplyRefreshedInsights,
                        onClick = onApplyRefreshedInsights,
                    ) {
                        if (isApplyingRefreshed) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(16.dp),
                                strokeWidth = 2.dp,
                            )
                            Spacer(modifier = Modifier.size(8.dp))
                            Text(text = "Applying...")
                        } else {
                            Text(text = "Use New Variant")
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun InsightMeaningSection(
    index: Int,
    meaning: WordMeaningItem,
) {
    val shape = RoundedCornerShape(14.dp)
    val levelColor = meaning.proficiencyLevel.toBadgeColor()
    val translation = meaning.translation
    val context = meaning.context
    val examples = meaning.examples

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .border(
                width = 1.dp,
                color = levelColor.copy(alpha = 0.35f),
                shape = shape,
            )
            .background(
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.28f),
                shape = shape,
            )
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                modifier = Modifier.weight(1f),
                text = "${index + 1}. $translation",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
            )
            LevelBadge(level = meaning.proficiencyLevel)
        }

        if (context.isNotEmpty()) {
            Text(
                text = context,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        if (examples.isNotEmpty()) {
            Text(
                text = "Examples",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.68f),
                fontWeight = FontWeight.Medium,
            )

            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                examples.forEach { example ->
                    Text(
                        text = "• $example",
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }
            }
        }
    }
}

@Composable
private fun LevelBadge(level: CefrLevel) {
    Box(
        modifier = Modifier
            .background(
                color = level.toBadgeColor().copy(alpha = 0.88f),
                shape = RoundedCornerShape(10.dp),
            )
            .padding(horizontal = 10.dp, vertical = 5.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = level.name,
            style = MaterialTheme.typography.labelMedium,
            color = Color.White,
            fontWeight = FontWeight.SemiBold,
        )
    }
}

private fun CefrLevel.toBadgeColor(): Color = when (this) {
    CefrLevel.A1 -> Color(0x6943A047)
    CefrLevel.A2 -> Color(0x7243A047)
    CefrLevel.B1 -> Color(0x531e88e5)
    CefrLevel.B2 -> Color(0x701565C0)
    CefrLevel.C1 -> Color(0xc9fb8c00)
    CefrLevel.C2 -> Color(0xcfe53935)
}
