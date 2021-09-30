package com.example.klaf.presentation.view_model_factories

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.klaf.presentation.view_models.CardAdditionViewModel

class CardAdditionViewModelFactory(
    private val context: Context,
    private val deckId: Int,
) : ViewModelProvider.Factory {

    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return CardAdditionViewModel(context, deckId) as T
    }
}