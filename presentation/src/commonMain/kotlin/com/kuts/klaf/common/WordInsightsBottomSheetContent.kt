package com.kuts.klaf.common

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.kuts.domain.entities.CefrLevel
import com.kuts.domain.entities.WordMeaningItem
import com.kuts.klaf.presentation.resources.Res
import com.kuts.klaf.presentation.resources.ic_youglish_logo
import com.kuts.klaf.presentation.resources.word_insights_applying_label
import com.kuts.klaf.presentation.resources.word_insights_current_saved_variant
import com.kuts.klaf.presentation.resources.word_insights_examples_label
import com.kuts.klaf.presentation.resources.word_insights_load_new_variant_action
import com.kuts.klaf.presentation.resources.word_insights_loading_label
import com.kuts.klaf.presentation.resources.word_insights_new_variant_preview
import com.kuts.klaf.presentation.resources.word_insights_use_new_variant_action
import com.kuts.klaf.presentation.resources.word_insights_word_label
import com.kuts.klaf.presentation.resources.word_insights_youglish_action
import com.kuts.klaf.theme.MainTheme
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource

@Composable
internal fun WordInsightsBottomSheetContent(
    word: String,
    meanings: List<WordMeaningItem>,
    refreshedMeanings: List<WordMeaningItem> = emptyList(),
    refreshedErrorMessageResId: StringResource? = null,
    isRefreshing: Boolean = false,
    isApplyingRefreshed: Boolean = false,
    canRequestRefreshedInsights: Boolean = false,
    canApplyRefreshedInsights: Boolean = false,
    onRequestRefreshedInsights: (() -> Unit)? = null,
    onApplyRefreshedInsights: (() -> Unit)? = null,
    onYouGlishClmmick: () -> Unit = {},
) {
    val isRefreshingAvailable =
        onRequestRefreshedInsights != null && onApplyRefreshedInsights != null
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
                text = stringResource(resource = Res.string.word_insights_word_label),
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

        if (word.isNotBlank()) {
            WordInsightActionsRow(
                onYouGlishClick = onYouGlishClick,
            )
        }

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
                    text = stringResource(resource = Res.string.word_insights_current_saved_variant),
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
                        Text(text = stringResource(resource = Res.string.word_insights_loading_label))
                    } else {
                        Text(text = stringResource(resource = Res.string.word_insights_load_new_variant_action))
                    }
                }
            }

            if (refreshedErrorMessageResId != null) {
                Text(
                    text = stringResource(resource = refreshedErrorMessageResId),
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
                        text = stringResource(resource = Res.string.word_insights_new_variant_preview),
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
                            Text(text = stringResource(resource = Res.string.word_insights_applying_label))
                        } else {
                            Text(text = stringResource(resource = Res.string.word_insights_use_new_variant_action))
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun WordInsightActionsRow(
    onYouGlishClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        OutlinedButton(
            modifier = Modifier
                .height(28.dp),
            onClick = onYouGlishClick,
            shape = RoundedCornerShape(12.dp),
            contentPadding = PaddingValues(horizontal = 6.dp, vertical = 6.dp),
            colors = ButtonDefaults.outlinedButtonColors(
                containerColor = Color(0x16ffffff),
            )
        ) {
            Image(
                painter = painterResource(resource = Res.drawable.ic_youglish_logo),
                contentDescription = stringResource(resource = Res.string.word_insights_youglish_action),
                colorFilter = ColorFilter.tint(Color.White)
            )
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
                text = stringResource(resource = Res.string.word_insights_examples_label),
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
    val bottomSheetColors = MainTheme.colors.wordInsightsBottomSheet

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
            color = bottomSheetColors.levelBadgeContent,
            fontWeight = FontWeight.SemiBold,
        )
    }
}

@Composable
private fun CefrLevel.toWordInsightBadgeColor(): Color {
    val bottomSheetColors = MainTheme.colors.wordInsightsBottomSheet

    return when (this) {
        CefrLevel.A1 -> bottomSheetColors.levelA1BadgeBackground
        CefrLevel.A2 -> bottomSheetColors.levelA2BadgeBackground
        CefrLevel.B1 -> bottomSheetColors.levelB1BadgeBackground
        CefrLevel.B2 -> bottomSheetColors.levelB2BadgeBackground
        CefrLevel.C1 -> bottomSheetColors.levelC1BadgeBackground
        CefrLevel.C2 -> bottomSheetColors.levelC2BadgeBackground
    }
}

@Preview(name = "Light", showBackground = true, backgroundColor = 0xFFF4F0E8)
@Composable
private fun WordInsightsBottomSheetContentPreview() {
    WordInsightsBottomSheetContentPreviewContent(darkTheme = false)
}

@Preview(name = "Dark", showBackground = true, backgroundColor = 0xFF1B1B1F)
@Composable
private fun WordInsightsBottomSheetContentDarkPreview() {
    WordInsightsBottomSheetContentPreviewContent(darkTheme = true)
}

@Composable
private fun WordInsightsBottomSheetContentPreviewContent(darkTheme: Boolean) {
    MainTheme(darkTheme = darkTheme) {
        Surface {
            WordInsightsBottomSheetContent(
                word = "bonjour",
                meanings = listOf(
                    WordMeaningItem(
                        frequencyRank = 120,
                        translation = "hello",
                        proficiencyLevel = CefrLevel.A1,
                        context = "Common greeting used in everyday conversations.",
                        examples = listOf(
                            "Bonjour, Marie.",
                            "Bonjour tout le monde.",
                        ),
                    ),
                    WordMeaningItem(
                        frequencyRank = 345,
                        translation = "good morning",
                        proficiencyLevel = CefrLevel.A2,
                        context = "Used as a polite morning greeting.",
                        examples = listOf("Bonjour, monsieur."),
                    ),
                ),
                refreshedMeanings = listOf(
                    WordMeaningItem(
                        frequencyRank = 98,
                        translation = "hi",
                        proficiencyLevel = CefrLevel.A1,
                        context = "A shorter, more informal translation variant.",
                        examples = listOf("Bonjour, Anna."),
                    ),
                ),
                canRequestRefreshedInsights = true,
                canApplyRefreshedInsights = true,
                onRequestRefreshedInsights = {},
                onApplyRefreshedInsights = {},
                onYouGlishClick = {},
            )
        }
    }
}
