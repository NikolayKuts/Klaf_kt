package com.example.klaf.presentation.cardViewer

import android.app.Application
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider

class CardViewerViewModelFactory(
    private val application: Application,
    private val deckId: Int,
) :
    ViewModelProvider.Factory {

    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return CardViewerViewModel(application, deckId) as T
    }
}