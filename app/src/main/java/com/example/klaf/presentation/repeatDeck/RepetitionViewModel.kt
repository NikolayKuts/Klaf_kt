package com.example.klaf.presentation.repeatDeck

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.klaf.domain.common.update
import com.example.klaf.domain.entities.Card
import com.example.klaf.domain.entities.Deck
import com.example.klaf.domain.enums.DifficultyRecallingLevel
import com.example.klaf.domain.useCases.FetchCardsUseCase
import com.example.klaf.domain.useCases.FetchDeckByIdUseCase
import com.example.klaf.domain.useCases.RemoveCardUseCase
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.util.*

class RepetitionViewModel @AssistedInject constructor(
    @Assisted deckId: Int,
    private val removeCard: RemoveCardUseCase,
    fetchCards: FetchCardsUseCase,
    fetchDeckById: FetchDeckByIdUseCase
) : ViewModel() {

//    private val repository: RepetitionRepository = RepetitionRepositoryRoomImp(context)

    private val _savedProgressCards: MutableList<Card> = LinkedList()
    val savedProgressCards: List<Card> get() = _savedProgressCards

    val cardsSource: SharedFlow<List<Card>> = fetchCards(deckId).shareIn(
        scope = viewModelScope,
        started = SharingStarted.Eagerly,
        replay = 1
    )

    val deck: SharedFlow<Deck?> = fetchDeckById(deckId = deckId).shareIn(
        scope = viewModelScope,
        started = SharingStarted.Eagerly,
        replay = 1
    )

    fun removeCard(cardId: Int) {
//        viewModelScope.launch { repository.deleteCard(cardId = cardId) }
    }
//
//    fun onGetDeck(deckId: Int, onDeckReceived: (Deck?) -> Unit) {
//        viewModelScope.launch { onDeckReceived(repository.getDeckById(deckId = deckId)) }
//    }

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