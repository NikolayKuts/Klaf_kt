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
    fetchDeckByIdUseCase: FetchDeckByIdUseCase
) : ViewModel() {

    val deck: SharedFlow<Deck?> = fetchDeckByIdUseCase(deckId = deckId).shareIn(
        scope = viewModelScope,
        started = SharingStarted.Eagerly,
        replay = 0
    )

    private val _savedProgressCards: MutableList<Card> = LinkedList()
    val savedProgressCards: List<Card> get() = _savedProgressCards

    val cardSource: StateFlow<List<Card>> = fetchCards(deckId = deckId).stateIn(
        scope = viewModelScope,
        started = SharingStarted.Eagerly,
        initialValue = emptyList()
    )

    private val _cards = MutableStateFlow<MutableList<Card>>(value = LinkedList())
    val cards = _cards.asStateFlow()

    fun moveCardByDifficultyRecallingLevel(level: DifficultyRecallingLevel) {
        val cardForMoving = cardSource.value[0]
        _cards.value = LinkedList(cardSource.value).apply {
            removeAt(0)

            val newPosition = when (level) {
                DifficultyRecallingLevel.EASY -> _cards.value.size
                DifficultyRecallingLevel.GOOD -> _cards.value.size * 3 / 4
                DifficultyRecallingLevel.HARD -> _cards.value.size / 4
            }
            add(newPosition, cardForMoving)
        }

//        cards.removeAt(0)
//
//        val newPosition = when (level) {
//            DifficultyRecallingLevel.EASY -> cards.size
//            DifficultyRecallingLevel.GOOD -> cards.size * 3 / 4
//            DifficultyRecallingLevel.HARD -> cards.size / 4
//        }
//        cards.add(newPosition, cardForMoving)
    }


    fun deleteCard(cardId: Int) {
        viewModelScope.launch { removeCard(cardId = cardId) }
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