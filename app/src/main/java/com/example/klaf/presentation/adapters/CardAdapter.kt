package com.example.klaf.presentation.adapters

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.klaf.databinding.CardListItemBinding
import com.example.klaf.domain.pojo.Card
import com.example.klaf.domain.update

class CardAdapter : RecyclerView.Adapter<CardAdapter.CardViewHolder>() {

    private val cardList: MutableList<Card> = mutableListOf()

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CardViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        val binding = CardListItemBinding.inflate(inflater, parent, false)
        return CardViewHolder(binding)
    }

    override fun onBindViewHolder(holder: CardViewHolder, position: Int) {
        val card = cardList[position]
        with(holder.binding) {
            textViewCardNumber.text = "${position + 1}"
            textViewNativeWord.text = card.nativeWord
            textViewForeignWord.text = card.foreignWord
            textViewIpa.text = card.ipa
        }
    }

    override fun getItemCount(): Int = cardList.size

    fun updateData(cards: List<Card>) {
        cardList.update(cards)
        notifyDataSetChanged()
    }

    class CardViewHolder(val binding: CardListItemBinding): RecyclerView.ViewHolder(binding.root)
}