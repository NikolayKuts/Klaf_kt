package com.example.klaf.presentation.view_model_factories

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.klaf.presentation.view_models.CardEditingViewModel

class CardEditingViewModelFactory(
    private val context: Context,
    private val cardId: Int
) : ViewModelProvider.Factory {

    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return CardEditingViewModel(context, cardId) as T
    }
}