package com.example.klaf.presentation.deckList

import android.view.ViewGroup
import androidx.compose.ui.platform.ComposeView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.example.klaf.domain.common.update
import com.example.klaf.domain.entities.Deck

class DeckAdapter(
    private var onClick: (deck: Deck) -> Unit,
    private var onItemMenuClick: (Deck) -> Unit,
) : RecyclerView.Adapter<DeckViewHolder>() {

    private val deckList: MutableList<Deck> = mutableListOf()

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): DeckViewHolder {
        return DeckViewHolder(
            composeView = ComposeView(context = parent.context),
            onItemClick = onClick,
            onLongItemClick = onItemMenuClick
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