package com.example.klaf.presentation.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.klaf.databinding.DeckListItemBinding
import com.example.klaf.domain.pojo.Deck

class DeckAdapter(
    //decks: List<Deck>,
    var onClick: () -> Unit = {},
    var onLongClick: () -> Unit = {}
) : RecyclerView.Adapter<DeckAdapter.DeckViewHolder>() {

    var testDecks: List<Deck> = ArrayList()
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
        holder.binding.textView.text = testDecks[position].name
        holder.binding.textView.setOnClickListener { onClick() }
        holder.binding.deckListItem.setOnLongClickListener {
            onLongClick()
            true
        }
    }

    override fun getItemCount(): Int = testDecks.size

    inner class DeckViewHolder(val binding: DeckListItemBinding) :
        RecyclerView.ViewHolder(binding.root)
}