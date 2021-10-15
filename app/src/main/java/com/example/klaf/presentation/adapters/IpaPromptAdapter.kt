package com.example.klaf.presentation.adapters

import android.graphics.Color.red
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.example.klaf.R
import com.example.klaf.databinding.IpaPromptItemBinding
import com.example.klaf.domain.ipa.LetterInfo
import com.example.klaf.domain.update

class IpaPromptAdapter : RecyclerView.Adapter<IpaPromptAdapter.IpaPromptViewHolder>() {

    private val prompts = ArrayList<LetterInfo>()

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): IpaPromptViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        val binding = IpaPromptItemBinding.inflate(inflater, parent, false)
        return IpaPromptViewHolder(binding)
    }

    override fun onBindViewHolder(holder: IpaPromptViewHolder, position: Int) {
        with(holder.binding) {
            val letterInfo = prompts[position]
            ipaPromptTextView.text = letterInfo.letter

            if (letterInfo.isChecked) {
                ipaPromptTextView.setTextColor(
                    ContextCompat.getColor(ipaPromptTextView.context, R.color.ipa_prompt_color)
                )
            } else {
                ipaPromptTextView.setTextColor(
                    ContextCompat.getColor(
                        ipaPromptTextView.context,
                        R.color.ipa_prompt_transparent)
                )
            }
        }

    }

    override fun getItemCount(): Int = prompts.size

    fun setData(letterInfos: List<LetterInfo>) {
        prompts.update(letterInfos)
        notifyDataSetChanged()
    }

    inner class IpaPromptViewHolder(val binding: IpaPromptItemBinding) :
        RecyclerView.ViewHolder(binding.root)
}