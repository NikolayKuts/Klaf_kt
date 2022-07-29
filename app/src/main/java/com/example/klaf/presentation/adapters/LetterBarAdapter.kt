package com.example.klaf.presentation.adapters

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.klaf.databinding.LetterBarItemBinding
import com.example.klaf.domain.common.update
import com.example.klaf.domain.ipa.IpaProcessor
import com.example.klaf.domain.ipa.LetterInfo
import com.example.klaf.presentation.cardAddition.LetterInfoViewHolder

class LetterBarAdapter(
    private val onUpdate: (uncompletedIpa: String?) -> Unit,
) : RecyclerView.Adapter<LetterInfoViewHolder>() {

    private val _letterInfos: MutableList<LetterInfo> = mutableListOf()
    val letterInfos: List<LetterInfo> = _letterInfos

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): LetterInfoViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        val binding = LetterBarItemBinding.inflate(inflater, parent, false)

        return LetterInfoViewHolder(
            binding = binding,
            onUpdate = onUpdate,
            onLetterClick = ::updateItem
        )
    }

    override fun onBindViewHolder(holder: LetterInfoViewHolder, position: Int) {
        holder.bind(letterInfos = _letterInfos)
    }

    override fun getItemCount(): Int = _letterInfos.size

    fun setData(letters: List<LetterInfo>) {
        _letterInfos.update(newData = letters)
        onUpdate(IpaProcessor.getUncompletedIpa(_letterInfos))
        notifyDataSetChanged()
    }

    private fun updateItem(adapterPosition: Int) {
        notifyItemChanged(adapterPosition)
    }
}