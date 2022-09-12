package com.example.klaf.presentation.deckList.common

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.work.WorkManager
import com.example.klaf.R
import com.example.klaf.data.common.DataSynchronizationState
import com.example.klaf.data.common.DataSynchronizationWorker.Companion.getDataSynchronizationProgressState
import com.example.klaf.data.common.DataSynchronizationWorker.Companion.performDataSynchronization
import com.example.klaf.domain.common.getCurrentDateAsLong
import com.example.klaf.domain.common.launchWithExceptionHandler
import com.example.klaf.domain.entities.Deck
import com.example.klaf.domain.useCases.*
import com.example.klaf.presentation.common.EventMessage
import com.example.klaf.presentation.common.log
import com.example.klaf.presentation.common.tryEmit
import com.example.klaf.presentation.deckList.dataSynchronization.DataSynchronizationNotifier
import com.example.klaf.presentation.deckList.deckCreation.DeckCreationState
import com.example.klaf.presentation.deckList.deckRenaming.DeckRenamingState
import com.example.klaf.presentation.deckRepetition.DeckRepetitionNotifier
import dagger.assisted.AssistedInject
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class DeckListViewModel @AssistedInject constructor(
    fetchDeckSource: FetchDeckSourceUseCase,
    private val createDeck: CreateDeckUseCase,
    private val renameDeck: RenameDeckUseCase,
    private val removeDeck: RemoveDeckUseCase,
    private val deleteAllCardsOfDeck: DeleteAllCardsOfDeck,
    createInterimDeck: CreateInterimDeckUseCase,
    deckRepetitionNotifier: DeckRepetitionNotifier,
    dataSynchronizationNotifier: DataSynchronizationNotifier,
    private val workManager: WorkManager,
) : ViewModel() {

    val deckSource: StateFlow<List<Deck>> = fetchDeckSource().stateIn(
        scope = viewModelScope,
        started = SharingStarted.Eagerly,
        initialValue = emptyList()
    )

    private val _eventMessage = MutableSharedFlow<EventMessage>(extraBufferCapacity = 1)
    val eventMessage = _eventMessage.asSharedFlow()

    private val _renamingState = MutableStateFlow(value = DeckRenamingState.NOT_RENAMED)
    val renamingState = _renamingState.asStateFlow()

    private val _deckCreationState = MutableStateFlow(value = DeckCreationState.NOT_CREATED)
    val deckCreationState = _deckCreationState.asStateFlow()

    private val _dataSynchronizationState =
        MutableStateFlow<DataSynchronizationState>(DataSynchronizationState.InitialState)
    val dataSynchronizationState = _dataSynchronizationState.asStateFlow()

    fun resetSynchronizationState() {
        _dataSynchronizationState.value = DataSynchronizationState.InitialState
    }

    init {
        deckRepetitionNotifier.createDeckRepetitionNotificationChannel()
        dataSynchronizationNotifier.createSynchronizationNotificationChannel()
        viewModelScope.launch { createInterimDeck() }
        observeDataSynchronizationStateWorker()
    }

    fun createNewDeck(deckName: String) {
        val deckNames = deckSource.value.map { deck -> deck.name }
        val trimmedDeckName = deckName.trim()

        when {
            deckNames.contains(deckName) -> {
                _eventMessage.tryEmit(value = EventMessage(R.string.such_name_is_already_exist))
            }
            trimmedDeckName.isEmpty() -> {
                _eventMessage.tryEmit(messageId = R.string.warning_deck_name_empty)
            }
            else -> {
                viewModelScope.launchWithExceptionHandler(
                    onException = { _, _ ->
                        _eventMessage.tryEmit(messageId = R.string.deck_has_been_created)
                    },
                    onCompletion = {
                        _eventMessage.tryEmit(messageId = R.string.deck_has_been_created)
                        _deckCreationState.value = DeckCreationState.CREATED
                    },
                ) {
                    createDeck(
                        deck = Deck(name = deckName, creationDate = getCurrentDateAsLong())
                    )
                }
            }
        }
    }

    fun renameDeck(deck: Deck?, newName: String) {
        val updatedName = newName.trim()
        deck?.let { notNullableDeck ->
            when (updatedName) {
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
                            _renamingState.value = DeckRenamingState.RENAMED
                        }
                    ) {
                        renameDeck(oldDeck = deck, name = updatedName)
                    }
                }
            }
        }
    }

    fun resetRenamingState() {
        _renamingState.value = DeckRenamingState.NOT_RENAMED
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
            launch { deleteAllCardsOfDeck(deckId = deckId) }
        }
    }

    fun getDeckById(deckId: Int): Deck? {
        val deck = deckSource.value.find { deck -> deck.id == deckId }
        if (deck == null) {
            _eventMessage.tryEmit(messageId = R.string.problem_with_fetching_deck)
        }
        return deck
    }

    fun synchronizeData() {
        log("view model")
        workManager.performDataSynchronization()
    }

    private fun observeDataSynchronizationStateWorker() {
        workManager.getDataSynchronizationProgressState()
            .catch { }
            .filter { it !is DataSynchronizationState.UncertainState }
            .onEach { _dataSynchronizationState.value = it }
            .launchIn(scope = viewModelScope)
    }
}