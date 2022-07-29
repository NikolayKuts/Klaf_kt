package com.example.klaf.presentation.cardAddition

import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.example.klaf.R
import com.example.klaf.databinding.LetterBarItemBinding
import com.example.klaf.domain.ipa.IpaProcessor
import com.example.klaf.domain.ipa.LetterInfo

class LetterInfoViewHolder(
    private val binding: LetterBarItemBinding,
    private val onUpdate: (uncompletedIpa: String?) -> Unit,
    private val onLetterClick: (adapterPosition: Int) -> Unit,
) : RecyclerView.ViewHolder(binding.root) {

    fun bind(letterInfos: List<LetterInfo>) {
        with(binding) {
            val letterInfo = letterInfos[adapterPosition]
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
            setClickListener(letterInfos = letterInfos)
        }
    }


    private fun setClickListener(letterInfos: List<LetterInfo>) {
        binding.letterTextView.setOnClickListener {
            val letterInfo = letterInfos[adapterPosition]

            if (letterInfo.letter == " ") {
                letterInfo.isChecked = false
            } else {
                letterInfo.isChecked = !letterInfo.isChecked
                onLetterClick(adapterPosition)
            }

            onUpdate(IpaProcessor.getUncompletedIpa(letterInfos))
        }
    }
}