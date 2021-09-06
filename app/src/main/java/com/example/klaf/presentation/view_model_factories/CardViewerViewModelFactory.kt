package com.example.klaf.presentation.view_model_factories

import android.app.Application
import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.klaf.presentation.view_models.CardViewerViewModel

class CardViewerViewModelFactory(
    private val application: Application,
    private val deckId: Int,
) :
    ViewModelProvider.Factory {

    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return CardViewerViewModel(application, deckId) as T
    }
}