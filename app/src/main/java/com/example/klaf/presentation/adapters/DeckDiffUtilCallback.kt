package com.example.klaf.presentation.adapters

import androidx.recyclerview.widget.DiffUtil
import com.example.klaf.domain.pojo.Deck

class DeckDiffUtilCallback(
    private val oldList: List<Deck>,
    private val newList: List<Deck>
) : DiffUtil.Callback() {
    override fun getOldListSize(): Int = oldList.size

    override fun getNewListSize(): Int = newList.size

    override fun areItemsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
        return oldList[oldItemPosition] == newList[newItemPosition]
    }

    override fun areContentsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
        return oldList[oldItemPosition] == newList[newItemPosition]
    }
}