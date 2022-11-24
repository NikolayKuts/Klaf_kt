package com.example.klaf.presentation.cardViewing

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.Divider
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.klaf.domain.entities.Card
import com.example.klaf.domain.ipa.decodeToCompletedViewingIpa
import com.example.klaf.presentation.theme.MainTheme

@Composable
fun CardViewingScreen(viewModel: CardViewingViewModel) {
    val cards by viewModel.cards.collectAsState()
    val deck by viewModel.deck.collectAsState(initial = null)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(8.dp)
    ) {
        DeckInfo(deckName = deck?.name)
        LazyColumn(
            modifier = Modifier.fillMaxWidth(),
            contentPadding = PaddingValues(bottom = 124.dp),
        ) {
            itemsIndexed(items = cards) { index, card ->
                CardItem(card = card, ordinal = index + 1)
                DividingLine()
            }
        }
    }
}

@Composable
private fun ColumnScope.DeckInfo(deckName: String?) {
    Text(
        modifier = Modifier
            .align(alignment = Alignment.CenterHorizontally)
            .padding(top = 8.dp, bottom = 8.dp),
        text = deckName ?: "",
        style = MainTheme.typographies.viewingCardDeckName
    )
}

@Composable
private fun CardItem(card: Card, ordinal: Int) {
    Row(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = ordinal.toString(),
            style = MainTheme.typographies.viewingCardOrdinal
        )
        ItemContentSpacer()
        Text(
            text = card.nativeWord,
            style = MainTheme.typographies.viewingCardNativeWord
        )
        ItemContentSpacer()
        Text(
            text = card.foreignWord,
            style = MainTheme.typographies.viewingCardForeignWord
        )
        ItemContentSpacer()
        Text(
            style = MainTheme.typographies.viewingCardIpa,
            text = card.decodeToCompletedViewingIpa()
        )
    }
}

@Composable
fun ItemContentSpacer() {
    Spacer(modifier = Modifier.width(8.dp))
}

@Composable
private fun DividingLine() {
    Divider(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 3.dp, bottom = 3.dp),
        color = MainTheme.colors.deckNavigationDialogSeparator,
    )
}