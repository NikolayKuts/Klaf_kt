package com.example.klaf.presentation.adapters

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.klaf.databinding.CardListItemBinding
import com.example.klaf.domain.common.update
import com.example.klaf.domain.entities.Card

class CardAdapter : RecyclerView.Adapter<CardAdapter.CardViewHolder>() {

    private val cardList: MutableList<Card> = mutableListOf()

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CardViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        val binding = CardListItemBinding.inflate(inflater, parent, false)
        return CardViewHolder(binding)
    }

    override fun onBindViewHolder(holder: CardViewHolder, position: Int) {
        if (holder.adapterPosition == RecyclerView.NO_POSITION) return
        holder.binding.apply {
            val adapterPosition = holder.adapterPosition
            val card = cardList[adapterPosition]
            textViewCardNumber.text = (adapterPosition + 1).toString()
            textViewNativeWord.text = card.nativeWord
            textViewForeignWord.text = card.foreignWord
            textViewIpa.text = card.ipa
        }
    }

    override fun getItemCount(): Int = cardList.size

    fun updateData(cards: List<Card>) {
        cardList.update(cards)
        notifyDataSetChanged()
        // TODO: 12/30/2021 implement DiffUtilCallback
    }

    class CardViewHolder(val binding: CardListItemBinding) : RecyclerView.ViewHolder(binding.root)
}