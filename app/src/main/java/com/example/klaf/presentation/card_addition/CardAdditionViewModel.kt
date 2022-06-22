package com.example.klaf.presentation.card_addition

import android.content.Context
import androidx.lifecycle.*
import com.example.klaf.data.implementations.CardAdditionRepositoryRoomImp
import com.example.klaf.data.repositories.CardAdditionRepository
import com.example.klaf.domain.pojo.Card
import com.example.klaf.domain.pojo.Deck
import kotlinx.coroutines.launch

class CardAdditionViewModel(context: Context, deckId: Int) : ViewModel() {

    private val repository: CardAdditionRepository = CardAdditionRepositoryRoomImp(context)

    val deck: LiveData<Deck?> = repository.getObservableDeckById(deckId = deckId)

    fun addNewCard(card: Card) {
        viewModelScope.launch { repository.insertCard(card) }
    }
}