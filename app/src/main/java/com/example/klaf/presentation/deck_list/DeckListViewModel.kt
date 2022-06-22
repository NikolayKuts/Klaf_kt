package com.example.klaf.presentation.deck_list

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.klaf.R
import com.example.klaf.data.implementations.DeckListRepositoryRoomImp
import com.example.klaf.data.repositories.DeckListRepository
import com.example.klaf.domain.auxiliary.DateAssistant
import com.example.klaf.domain.common.launchWithExceptionHandler
import com.example.klaf.domain.pojo.Deck
import com.example.klaf.presentation.common.EventMessage
import com.example.klaf.presentation.common.tryEmit
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class DeckListViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: DeckListRepository = DeckListRepositoryRoomImp(application)

    val deckSource: StateFlow<List<Deck>> = repository.getDeckSource().stateIn(
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
                    _eventMessage.tryEmit(messageId = R.string.message_deck_has_been_created)
                },
                onCompletion = {
                    _eventMessage.tryEmit(messageId = R.string.card_has_been_added)
                    // TODO: 12/29/2021 to translate toast shoeing to DeckListFragment
                },
            ) {
                repository.insertDeck(
                    deck = Deck(
                        name = deckName,
                        creationDate = DateAssistant.getCurrentDateAsLong()
                    )
                )
            }
        }
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
                            _eventMessage.tryEmit(messageId = R.string.exception_renaming_deck)
                        },
                        onCompletion = {
                            _eventMessage.tryEmit(messageId = R.string.message_deck_has_been_renamed)
                        }
                    ) {
                        repository.insertDeck(deck = deck.copy(name = newName))
                    }
                }
            }
        }
    }

    fun removeDeck(deckId: Int) {
        viewModelScope.launchWithExceptionHandler(
            onException = { _, _ ->
                _eventMessage.tryEmit(messageId = R.string.exception_removing_deck)
            },
            onCompletion = {
                _eventMessage.tryEmit(messageId = R.string.the_deck_has_been_removed)
            }
        ) {
            launch { repository.removeDeck(deckId = deckId) }
            launch { repository.removeCardsOfDeck(deckId = deckId) }
        }
    }

    fun getDeckById(deckId: Int): Deck? = deckSource.value.find { deck -> deck.id == deckId }
}