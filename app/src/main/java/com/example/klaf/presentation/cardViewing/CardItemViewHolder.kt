package com.example.klaf.presentation.cardViewing

import androidx.recyclerview.widget.RecyclerView
import com.example.klaf.databinding.CardListItemBinding
import com.example.klaf.domain.entities.Card

class CardItemViewHolder(
    val binding: CardListItemBinding,
) : RecyclerView.ViewHolder(binding.root) {

    fun bind(cards: List<Card>) {
        val card = cards[adapterPosition]

        binding.composeViewCardItem.setContent { CardItemView(card = card) }
    }
}