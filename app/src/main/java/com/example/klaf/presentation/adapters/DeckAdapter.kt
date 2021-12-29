package com.example.klaf.presentation.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.klaf.databinding.DeckListItemBinding
import com.example.klaf.domain.pojo.Deck
import com.example.klaf.domain.update

class DeckAdapter(
    private var onClick: (deck: Deck) -> Unit,
    private var onPopupMenuClick: (View, Deck) -> Unit,
) : RecyclerView.Adapter<DeckAdapter.DeckViewHolder>() {

    private val deckList: MutableList<Deck> = mutableListOf()

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): DeckViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        val binding = DeckListItemBinding.inflate(inflater, parent, false)
        return DeckViewHolder(binding)
    }

    override fun onBindViewHolder(holder: DeckViewHolder, position: Int) {
        with(holder.binding) {
            val deck = deckList[position]
            textViewDeckName.text = deck.name
            textViewRepeatDay.text = deck.repeatDay.toString()
            textViewScheduledDate.text = deck.scheduledDate.toString()
            textViewRepeatQuantity.text = deck.repeatQuantity.toString()
            textViewCardQuantity.text = deck.cardQuantity.toString()

            deckListItem.setOnClickListener { onClick(deckList[position]) }


            popupMenuImageView.setOnClickListener { view ->
                onPopupMenuClick(view, deck)
//                true
            }
        }
    }

    override fun getItemCount(): Int = deckList.size

    fun updateData(newDecks: List<Deck>) {
        deckList.update(newDecks)
        notifyDataSetChanged()

        // TODO: 12/29/2021 implement DiffUtilCallback
    }

    class DeckViewHolder(val binding: DeckListItemBinding) : RecyclerView.ViewHolder(binding.root)
}