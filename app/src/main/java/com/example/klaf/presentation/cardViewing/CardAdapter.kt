package com.example.klaf.presentation.cardViewing

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.klaf.databinding.CardListItemBinding
import com.example.klaf.domain.common.update
import com.example.klaf.domain.entities.Card

class CardAdapter : RecyclerView.Adapter<CardItemViewHolder>() {

    private val cardList: MutableList<Card> = mutableListOf()

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CardItemViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        val binding = CardListItemBinding.inflate(inflater, parent, false)
        return CardItemViewHolder(binding)
    }

    override fun onBindViewHolder(holder: CardItemViewHolder, position: Int) {
        if (holder.adapterPosition == RecyclerView.NO_POSITION) {
            throw Exception("The adapter position is \"NO_POSITION\'")
        }

        holder.bind(cards = cardList)
    }

    override fun getItemCount(): Int = cardList.size

    fun updateData(cards: List<Card>) {
        cardList.update(cards)
        notifyDataSetChanged()
        // TODO: 12/30/2021 implement DiffUtilCallback
    }
}