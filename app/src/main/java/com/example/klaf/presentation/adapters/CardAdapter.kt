package com.example.klaf.presentation.adapters

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.klaf.databinding.CardListItemBinding
import com.example.klaf.domain.pojo.Card

class CardAdapter : RecyclerView.Adapter<CardAdapter.CardViewHolder>() {

    var cards: List<Card> = ArrayList()
    set(value) {
        field = value
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CardViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        val binding = CardListItemBinding.inflate(inflater, parent, false)
        return CardViewHolder(binding)
    }

    override fun onBindViewHolder(holder: CardViewHolder, position: Int) {
        val card = cards[position]
        with(holder.binding) {
            textViewCardNumber.text = "${position + 1}"
            textViewNativeWord.text = card.nativeWord
            textViewForeignWord.text = card.foreignWord
            textViewIpa.text = card.ipa
        }
    }

    override fun getItemCount(): Int = cards.size

    inner class CardViewHolder(val binding: CardListItemBinding):
        RecyclerView.ViewHolder(binding.root)
}