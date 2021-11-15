package com.example.klaf.presentation.view_models

import android.content.Context
import androidx.lifecycle.*
import com.example.klaf.data.implementations.RepetitionRepositoryRoomImp
import com.example.klaf.data.repositories.RepetitionRepository
import com.example.klaf.domain.pojo.Card
import com.example.klaf.domain.pojo.Deck
import com.example.klaf.domain.update
import kotlinx.coroutines.launch
import java.util.*

class RepetitionViewModel(context: Context, deckId: Int) : ViewModel() {

    private val repository: RepetitionRepository = RepetitionRepositoryRoomImp(context)
    private val _savedProgressCards: MutableList<Card> = LinkedList()
    val savedProgressCards: List<Card> get() = _savedProgressCards
    val cardSource: LiveData<List<Card>> = repository.getCardsByDeckId(deckId)

    fun removeCard(cardId: Int) {
        viewModelScope.launch { repository.deleteCard(cardId = cardId) }
    }

    fun onGetDeck(deckId: Int, onDeckReceived: (Deck?) -> Unit) {
        viewModelScope.launch { onDeckReceived(repository.getDeckById(deckId = deckId)) }
    }

    fun saveRepetitionProgress(cards: List<Card>) {
        _savedProgressCards.update(cards)
    }

    fun clearProgress() {
        _savedProgressCards.clear()
    }

    fun getCardsByProgress(receivedCards: List<Card>): List<Card> {
        return LinkedList<Card>().apply result@{

            val newAddedCards = LinkedList<Card>().apply addedCards@{
                if (_savedProgressCards.size < receivedCards.size) {

                    receivedCards.forEach { receivedCard ->
                        if (!_savedProgressCards.contains(receivedCard)) {
                            this@addedCards.add(receivedCard)
                        }
                    }
                }
            }

            val temporary: MutableList<Card> = LinkedList(receivedCards).apply temporary@{
                this@temporary.removeAll(newAddedCards)
            }

            _savedProgressCards.forEach { savedCard ->
                temporary.forEach { relevantCard ->
                    if (relevantCard.id == savedCard.id) {
                        this@result.add(relevantCard)
                    }
                }
            }
            this@result.addAll(newAddedCards)
        }
    }
}