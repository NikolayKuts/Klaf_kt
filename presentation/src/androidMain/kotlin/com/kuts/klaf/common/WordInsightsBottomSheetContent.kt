package com.kuts.klaf.common

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.kuts.domain.entities.CefrLevel
import com.kuts.domain.entities.WordMeaningItem

@Composable
internal fun WordInsightsBottomSheetContent(
    word: String,
    meanings: List<WordMeaningItem>,
    refreshedMeanings: List<WordMeaningItem> = emptyList(),
    refreshedErrorMessage: String = "",
    isRefreshing: Boolean = false,
    isApplyingRefreshed: Boolean = false,
    canRequestRefreshedInsights: Boolean = false,
    canApplyRefreshedInsights: Boolean = false,
    onRequestRefreshedInsights: (() -> Unit)? = null,
    onApplyRefreshedInsights: (() -> Unit)? = null,
) {
    val isRefreshingAvailable = onRequestRefreshedInsights != null && onApplyRefreshedInsights != null
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
            if (isRefreshingAvailable) {
                Text(
                    text = "Current Saved Variant",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.85f),
                )
            }

            meanings.forEachIndexed { index, meaning ->
                WordInsightMeaningSection(
                    index = index,
                    meaning = meaning,
                )
            }

            if (isRefreshingAvailable) {
                OutlinedButton(
                    modifier = Modifier.fillMaxWidth(),
                    enabled = canRequestRefreshedInsights,
                    onClick = { onRequestRefreshedInsights?.invoke() },
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
            }

            if (refreshedErrorMessage.isNotBlank()) {
                Text(
                    text = refreshedErrorMessage,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error,
                )
            }

            if (isRefreshingAvailable && refreshedMeanings.isNotEmpty()) {
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
                        WordInsightMeaningSection(
                            index = index,
                            meaning = meaning,
                        )
                    }

                    Button(
                        modifier = Modifier.fillMaxWidth(),
                        enabled = canApplyRefreshedInsights,
                        onClick = { onApplyRefreshedInsights?.invoke() },
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
private fun WordInsightMeaningSection(
    index: Int,
    meaning: WordMeaningItem,
) {
    val shape = RoundedCornerShape(14.dp)
    val levelColor = meaning.proficiencyLevel.toWordInsightBadgeColor()
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
            WordInsightLevelBadge(level = meaning.proficiencyLevel)
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
                        text = "- $example",
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }
            }
        }
    }
}

@Composable
private fun WordInsightLevelBadge(level: CefrLevel) {
    Box(
        modifier = Modifier
            .background(
                color = level.toWordInsightBadgeColor().copy(alpha = 0.88f),
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

private fun CefrLevel.toWordInsightBadgeColor(): Color = when (this) {
    CefrLevel.A1 -> Color(0x6943A047)
    CefrLevel.A2 -> Color(0x7243A047)
    CefrLevel.B1 -> Color(0x531e88e5)
    CefrLevel.B2 -> Color(0x701565C0)
    CefrLevel.C1 -> Color(0xc9fb8c00)
    CefrLevel.C2 -> Color(0xcfe53935)
}
