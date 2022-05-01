package com.example.klaf.presentation.repeat_deck

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider

class RepetitionViewModelFactory(
    private val context: Context,
    private val deckId: Int,
) : ViewModelProvider.Factory {

    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return RepetitionViewModel(context = context, deckId = deckId) as T
    }
}