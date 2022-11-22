package com.example.klaf.presentation.interimDeck

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.Checkbox
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

@Composable
fun InterimDeckScreen(viewModel: BaseInterimDeckViewModel) {
    val deck by viewModel.interimDeck.collectAsState(initial = null) ?: return
    val cardHolders by viewModel.cardHolders.collectAsState()

    Box(
        modifier = Modifier.fillMaxSize()
    ) {
        LazyColumn {
            itemsIndexed(items = cardHolders) { index, holder ->
                CardItem(
                    holder = holder,
                    onSelectedChanged = { viewModel.changeSelectionState(position = index) },
                )
                Spacer(modifier = Modifier.height(8.dp))
            }
        }
    }
}

@Composable
private fun CardItem(holder: SelectableCardHolder, onSelectedChanged: (Boolean) -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().border(BorderStroke(2.dp, Color.Red)),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(text = holder.card.foreignWord)
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            modifier = Modifier.weight(1F),
            text = holder.card.nativeWord,
        )
        Checkbox(
            checked = holder.isSelected,
            onCheckedChange = onSelectedChanged,
        )
    }
}