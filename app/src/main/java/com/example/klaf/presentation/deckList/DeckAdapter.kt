package com.example.klaf.presentation.deckList

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.example.klaf.databinding.DeckListItemBinding
import com.example.klaf.domain.common.update
import com.example.klaf.domain.entities.Deck

class DeckAdapter(
    private var onClick: (deck: Deck) -> Unit,
    private var onItemMenuClick: (View, Deck) -> Unit,
) : RecyclerView.Adapter<DeckViewHolder>() {

    private val deckList: MutableList<Deck> = mutableListOf()

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): DeckViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        val binding = DeckListItemBinding.inflate(inflater, parent, false)

        return DeckViewHolder(
            binding = binding,
            onItemClick = onClick,
            onItemMenuClick = onItemMenuClick
        )
    }

    override fun onBindViewHolder(holder: DeckViewHolder, position: Int) {
        holder.bind(deckList = deckList)
    }

    override fun getItemCount(): Int = deckList.size

    fun updateData(newDecks: List<Deck>) {
        val diffResult = DiffUtil.calculateDiff(DeckDiffUtilCallback(deckList, newDecks))
        deckList.update(newDecks)
        diffResult.dispatchUpdatesTo(this)
    }
}