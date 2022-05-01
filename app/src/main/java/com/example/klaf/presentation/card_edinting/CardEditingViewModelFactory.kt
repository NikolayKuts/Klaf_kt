package com.example.klaf.presentation.card_edinting

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider

class CardEditingViewModelFactory(
    private val context: Context,
    private val deckId: Int,
    private val cardId: Int
) : ViewModelProvider.Factory {

    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return CardEditingViewModel(context,deckId, cardId) as T
    }
}