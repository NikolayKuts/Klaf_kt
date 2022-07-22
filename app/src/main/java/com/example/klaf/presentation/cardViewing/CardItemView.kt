package com.example.klaf.presentation.cardViewing

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.width
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.klaf.domain.entities.Card

@Composable
fun CardItemView(card: Card) {
    Row() {
        Text(text = card.nativeWord)
        Spacer(modifier = Modifier.width(8.dp))
        Text(text = card.foreignWord)
        Spacer(modifier = Modifier.width(8.dp))
        Text(text = card.ipa)
    }
}