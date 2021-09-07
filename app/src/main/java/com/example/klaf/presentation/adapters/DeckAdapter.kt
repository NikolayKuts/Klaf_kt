package com.example.klaf.presentation.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.klaf.databinding.DeckListItemBinding
import com.example.klaf.domain.pojo.Deck

class DeckAdapter(
    var onClick: (deck: Deck) -> Unit = {},
    var onLongClick: (View, Deck) -> Unit = { _, _ -> }
) : RecyclerView.Adapter<DeckAdapter.DeckViewHolder>() {

    var decks: List<Deck> = ArrayList()
    set(value) {
        field = value
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): DeckViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        val binding = DeckListItemBinding.inflate(inflater, parent, false)
        return DeckViewHolder(binding)
    }

    override fun onBindViewHolder(holder: DeckViewHolder, position: Int) {
        with(holder.binding) {
            val deck = decks[position]
            textViewDeckName.text = deck.name
            textViewRepeatDay.text = deck.repeatDay.toString()
            textViewScheduledDate.text = deck.scheduledDate.toString()
            textViewRepeatQuantity.text = deck.repeatQuantity.toString()
            textViewCardQuantity.text = deck.cardQuantity.toString()

            deckListItem.setOnClickListener { onClick(decks[position]) }


            deckListItem.setOnLongClickListener { view ->
                onLongClick(view, deck)
                true
            }
        }
    }

    override fun getItemCount(): Int = decks.size

    inner class DeckViewHolder(val binding: DeckListItemBinding) :
        RecyclerView.ViewHolder(binding.root)
}