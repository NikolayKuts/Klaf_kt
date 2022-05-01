package com.example.klaf.presentation.card_addition

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider

class CardAdditionViewModelFactory(
    private val context: Context,
    private val deckId: Int,
) : ViewModelProvider.Factory {

    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return CardAdditionViewModel(context, deckId) as T
    }
}