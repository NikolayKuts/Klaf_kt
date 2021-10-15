package com.example.klaf.presentation.adapters

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.example.klaf.R
import com.example.klaf.databinding.LetterBarItemBinding
import com.example.klaf.domain.ipa.IpaProcessor
import com.example.klaf.domain.ipa.LetterInfo

class LetterBarAdapter(
    private val letterInfos: MutableList<LetterInfo>,
    private val onItemClickListener: (uncompletedIpa: String?) -> Unit,
) :
    RecyclerView.Adapter<LetterBarAdapter.LetterInfoViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): LetterInfoViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        val binding = LetterBarItemBinding.inflate(inflater, parent, false)
        return LetterInfoViewHolder(binding)
    }

    override fun onBindViewHolder(holder: LetterInfoViewHolder, position: Int) {
        val letterInfo = letterInfos[position]
        with(holder.binding) {
            letterTextView.text = letterInfo.letter

            when {
                letterInfo.letter == " " -> {
                    letterTextView.textSize = 40.0f
                }
                letterInfo.isChecked -> {
                    letterTextView.textSize = 50.0f
                    letterTextView.setBackgroundColor(
                        ContextCompat.getColor(
                            letterTextView.context,
                            R.color.checked_letter_background
                        )
                    )
                }
                else -> {
                    letterTextView.textSize = 40.0f
                    letterTextView.setBackgroundColor(
                        ContextCompat.getColor(
                            letterTextView.context,
                            R.color.unchecked_letter_background
                        )
                    )
                }
            }

            letterTextView.setOnClickListener {
                if (letterInfo.letter == " ") {
                    letterInfo.isChecked = false
                } else {
                    letterInfo.isChecked = !letterInfo.isChecked
                    notifyItemChanged(position)
                }
                onItemClickListener(IpaProcessor().getUncompletedIpa(letterInfos))
            }
        }
    }

    override fun getItemCount(): Int = letterInfos.size

    fun setData(letters: List<LetterInfo>) {
        this.letterInfos.clear()
        this.letterInfos.addAll(letters)
        notifyDataSetChanged()
    }

    inner class LetterInfoViewHolder(val binding: LetterBarItemBinding) :
        RecyclerView.ViewHolder(binding.root)
}