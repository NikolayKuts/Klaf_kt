package com.example.klaf.presentation.deckList

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.klaf.R
import com.example.klaf.domain.common.getCurrentDateAsLong
import com.example.klaf.domain.common.launchWithExceptionHandler
import com.example.klaf.domain.entities.Deck
import com.example.klaf.domain.useCases.*
import com.example.klaf.presentation.common.EventMessage
import com.example.klaf.presentation.common.Notifier
import com.example.klaf.presentation.common.tryEmit
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class DeckListViewModel @Inject constructor(
    fetchDeckSource: FetchDeckSourceUseCase,
    private val createDeck: CreateDeckUseCase,
    private val renameDeck: RenameDeckUseCase,
    private val removeDeck: RemoveDeckUseCase,
    private val removeCardsOfDeck: RemoveCardsOfDeckUseCase,
    notifier: Notifier,
) : ViewModel() {

    val deckSource: StateFlow<List<Deck>> = fetchDeckSource().stateIn(
        scope = viewModelScope,
        started = SharingStarted.Eagerly,
        initialValue = emptyList()
    )

    private val _eventMessage = MutableSharedFlow<EventMessage>(extraBufferCapacity = 1)
    val eventMessage = _eventMessage.asSharedFlow()

    fun createNewDeck(deckName: String) {
        val deckNames = deckSource.value.map { deck -> deck.name }

        if (deckNames.contains(deckName)) {
            _eventMessage.tryEmit(value = EventMessage(R.string.such_name_is_already_exist))
        } else {
            viewModelScope.launchWithExceptionHandler(
                onException = { _, _ ->
                    _eventMessage.tryEmit(messageId = R.string.deck_has_been_created)
                },
                onCompletion = {
                    _eventMessage.tryEmit(messageId = R.string.deck_has_been_created)
                },
            ) {
                createDeck(
                    deck = Deck(
                        name = deckName,
                        creationDate = getCurrentDateAsLong()
                    )
                )
            }
        }
    }

    init {
        notifier.createDeckRepetitionNotificationChannel()
    }

    fun renameDeck(deck: Deck?, newName: String) {
        deck?.let { notNullableDeck ->
            when (newName) {
                "" -> {
                    _eventMessage.tryEmit(messageId = R.string.type_new_deck_name)
                }
                notNullableDeck.name -> {
                    _eventMessage.tryEmit(messageId = R.string.deck_name_is_not_changed)
                }
                else -> {
                    viewModelScope.launchWithExceptionHandler(
                        onException = { _, _ ->
                            _eventMessage.tryEmit(messageId = R.string.problem_with_renaming_deck)
                        },
                        onCompletion = {
                            _eventMessage.tryEmit(messageId = R.string.deck_has_been_renamed)
                        }
                    ) {
                        renameDeck(oldDeck = deck, name = newName)
                    }
                }
            }
        }
    }

    fun deleteDeck(deckId: Int) {
        viewModelScope.launchWithExceptionHandler(
            onException = { _, _ ->
                _eventMessage.tryEmit(messageId = R.string.problem_with_removing_deck)
            },
            onCompletion = {
                _eventMessage.tryEmit(messageId = R.string.the_deck_has_been_removed)
            }
        ) {
            launch { removeDeck(deckId = deckId) }
            launch { removeCardsOfDeck(deckId = deckId) }
        }
    }

    fun getDeckById(deckId: Int): Deck? = deckSource.value.find { deck -> deck.id == deckId }
}