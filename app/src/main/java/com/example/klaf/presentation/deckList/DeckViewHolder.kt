package com.example.klaf.presentation.deckList

import android.view.View
import androidx.compose.ui.platform.ComposeView
import androidx.recyclerview.widget.RecyclerView
import com.example.klaf.domain.entities.Deck
import com.example.klaf.presentation.theme.MainTheme

class DeckViewHolder(
    private val composeView: ComposeView,
    private val onItemClick: (Deck) -> Unit,
    private val onItemMenuClick: (View, Deck) -> Unit
) : RecyclerView.ViewHolder(composeView) {

    fun bind(deckList: List<Deck>) {
        val deck = deckList[adapterPosition]

        composeView.setContent {
            MainTheme() {
                DeckItemView(
                    deck = deck,
                    position = adapterPosition,
                    onItemClick = onItemClick
                )
            }
        }
    }
}