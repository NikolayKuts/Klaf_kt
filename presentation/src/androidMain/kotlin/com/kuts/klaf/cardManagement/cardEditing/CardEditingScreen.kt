package com.kuts.klaf.cardManagement.cardEditing

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
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
            showGeminiDebugButton = true,
            isCambridgeBottomSheetEnabled = false,
        )

        if (insightsUiState.hasData) {
            InsightsSheetHandle(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 12.dp),
                onClick = viewModel::showInsightsSheet,
            )
        }
    }

    if (insightsUiState.isSheetVisible) {
        ModalBottomSheet(
            onDismissRequest = viewModel::hideInsightsSheet,
            sheetState = sheetState,
        ) {
            InsightsBottomSheetContent(
                word = insightsUiState.word,
                meanings = insightsUiState.meanings,
            )
        }
    }
}

@Composable
private fun InsightsSheetHandle(
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    Box(
        modifier = modifier
            .background(
                color = Color(0xFF1A1A1A),
                shape = RoundedCornerShape(16.dp),
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 10.dp),
        contentAlignment = Alignment.Center,
    ) {
        Box(
            modifier = Modifier
                .size(width = 42.dp, height = 4.dp)
                .background(
                    color = Color(0xFFB6B6B6),
                    shape = RoundedCornerShape(50),
                ),
        )
    }
}

@Composable
private fun InsightsBottomSheetContent(
    word: String,
    meanings: List<WordMeaningItem>,
) {
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

        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(max = 520.dp)
                .padding(bottom = 16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            itemsIndexed(meanings) { index, meaning ->
                InsightMeaningSection(
                    index = index,
                    meaning = meaning,
                )
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

