package com.example.klaf.presentation.deckList

import android.view.View
import androidx.recyclerview.widget.RecyclerView
import com.example.klaf.databinding.DeckListItemBinding
import com.example.klaf.domain.entities.Deck

class DeckViewHolder(
    private val binding: DeckListItemBinding,
    private val onItemClick: (Deck) -> Unit,
    private val onItemMenuClick: (View, Deck) -> Unit
) : RecyclerView.ViewHolder(binding.root) {

    fun onBind(deckList: List<Deck>) {
        val deck = deckList[adapterPosition]

        binding.apply {
            textViewDeckName.text = deck.name
            textViewRepeatDay.text = deck.repeatDay.toString()
            textViewScheduledDate.text = deck.scheduledDate.toString()
            textViewRepeatQuantity.text = deck.repeatQuantity.toString()
            textViewCardQuantity.text = deck.cardQuantity.toString()

            deckListItem.setOnClickListener { onItemClick(deck) }
            popupMenuImageView.setOnClickListener { view -> onItemMenuClick(view, deck) }
        }
    }
}