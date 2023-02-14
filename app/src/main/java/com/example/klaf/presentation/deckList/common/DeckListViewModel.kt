package com.example.klaf.presentation.deckList.common

import androidx.lifecycle.viewModelScope
import androidx.work.WorkManager
import com.example.klaf.R
import com.example.klaf.data.common.AppReopeningWorker.Companion.scheduleAppReopening
import com.example.klaf.data.common.DataSynchronizationState
import com.example.klaf.data.common.DataSynchronizationWorker.Companion.getDataSynchronizationProgressState
import com.example.klaf.data.common.DataSynchronizationWorker.Companion.performDataSynchronization
import com.example.klaf.data.common.notifications.NotificationChannelInitializer
import com.example.klaf.domain.common.CoroutineStateHolder.Companion.launchWithState
import com.example.klaf.domain.common.CoroutineStateHolder.Companion.onException
import com.example.klaf.domain.common.getCurrentDateAsLong
import com.example.klaf.domain.entities.Deck
import com.example.klaf.domain.useCases.*
import com.example.klaf.presentation.common.EventMessage
import com.example.klaf.presentation.common.tryEmit
import com.example.klaf.presentation.deckList.common.DeckListNavigationDestination.*
import com.example.klaf.presentation.deckList.common.DeckListNavigationEvent.*
import com.example.klaf.presentation.deckList.deckCreation.DeckCreationState
import com.example.klaf.presentation.deckList.deckRenaming.DeckRenamingState
import dagger.assisted.AssistedInject
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class DeckListViewModel @AssistedInject constructor(
    fetchDeckSource: FetchDeckSourceUseCase,
    private val createDeck: CreateDeckUseCase,
    private val renameDeck: RenameDeckUseCase,
    private val removeDeck: RemoveDeckUseCase,
    createInterimDeck: CreateInterimDeckUseCase,
    notificationChannelInitializer: NotificationChannelInitializer,
    private val workManager: WorkManager,
) : BaseDeckListViewModel() {

    override val eventMessage = MutableSharedFlow<EventMessage>(extraBufferCapacity = 1)

    override val renamingState = MutableStateFlow(value = DeckRenamingState.NOT_RENAMED)

    override val deckCreationState = MutableStateFlow(value = DeckCreationState.NOT_CREATED)

    override val dataSynchronizationState =
        MutableStateFlow<DataSynchronizationState>(DataSynchronizationState.InitialState)

    override val deckSource: StateFlow<List<Deck>?> = (fetchDeckSource() as Flow<List<Deck>?>)
        .catch { this.emit(value = null) }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.Eagerly,
            initialValue = emptyList()
        )

    override fun resetSynchronizationState() {
        dataSynchronizationState.value = DataSynchronizationState.InitialState
    }

    override val navigationDestination = MutableSharedFlow<DeckListNavigationDestination>()

    init {
        notificationChannelInitializer.initialize()
        viewModelScope.launch { createInterimDeck() }
        observeDataSynchronizationStateWorker()
    }

    override fun createNewDeck(deckName: String) {
        val decks = deckSource.value

        if (decks == null) {
            eventMessage.tryEmit(messageId = R.string.problem_fetching_decks)
        } else {
            val deckNames = decks.map { deck -> deck.name }
            val trimmedDeckName = deckName.trim()

            when {
                deckNames.contains(deckName) -> {
                    eventMessage.tryEmit(messageId = R.string.such_deck_is_already_exist)
                }
                trimmedDeckName.isEmpty() -> {
                    eventMessage.tryEmit(messageId = R.string.warning_deck_name_empty)
                }
                else -> {
                    viewModelScope.launchWithState {
                        createDeck(
                            deck = Deck(name = deckName, creationDate = getCurrentDateAsLong())
                        )
                        eventMessage.tryEmit(messageId = R.string.deck_has_been_created)
                        deckCreationState.value = DeckCreationState.CREATED
                    } onException { _, _ ->
                        eventMessage.tryEmit(messageId = R.string.problem_with_creating_deck)
                    }
                }
            }
        }
    }

    override fun resetDeckCreationState() {
        deckCreationState.value = DeckCreationState.NOT_CREATED
    }

    override fun renameDeck(deck: Deck, newName: String) {
        val updatedName = newName.trim()
        val decks = deckSource.value

        if (decks == null) {
            eventMessage.tryEmit(messageId = R.string.problem_fetching_decks)
        } else {
            when {
                updatedName.isEmpty() -> {
                    eventMessage.tryEmit(messageId = R.string.type_new_deck_name)
                }
                updatedName == deck.name -> {
                    eventMessage.tryEmit(messageId = R.string.deck_name_is_not_changed)
                }
                decks.any { it.name == newName } -> {
                    eventMessage.tryEmit(messageId = R.string.such_deck_is_already_exist)
                }
                else -> {
                    viewModelScope.launchWithState {
                        renameDeck(oldDeck = deck, name = updatedName)
                        eventMessage.tryEmit(messageId = R.string.deck_has_been_renamed)
                        renamingState.value = DeckRenamingState.RENAMED
                    } onException { _, _ ->
                        eventMessage.tryEmit(messageId = R.string.problem_with_renaming_deck)
                    }
                }
            }
        }
    }

    override fun resetDeckRenamingState() {
        renamingState.value = DeckRenamingState.NOT_RENAMED
    }

    override fun deleteDeck(deckId: Int) {
        viewModelScope.launchWithState {
            removeDeck(deckId = deckId)
            eventMessage.tryEmit(messageId = R.string.the_deck_has_been_removed)
        } onException { _, _ ->
            eventMessage.tryEmit(messageId = R.string.problem_with_removing_deck)
        }
    }

    override fun getDeckById(deckId: Int): Deck? {
        val deck = deckSource.value?.find { deck -> deck.id == deckId }

        if (deck == null) {
            eventMessage.tryEmit(messageId = R.string.problem_with_fetching_deck)
        }

        return deck
    }

    override fun synchronizeData() {
        workManager.performDataSynchronization()
    }

    override fun navigate(event: DeckListNavigationEvent) {
        val destination = when (event) {
            ToDeckCreationDialog -> DeckCreationDialogDestination
            ToDataSynchronizationDialog -> DataSynchronizationDialogDestination
            is ToDeckNavigationDialog -> {
                getDestinationByDeckId(
                    deckId = event.deck.id,
                    ifDeckIsNotInterim = { DeckNavigationDialogDestination(deck = event.deck) }
                )
            }
            is ToFragment -> {
                getDestinationByDeckId(
                    deckId = event.deck.id,
                    ifDeckIsNotInterim = { DeckRepetitionFragmentDestination(deck = event.deck) }
                )
            }
        }

        viewModelScope.launch { navigationDestination.emit(value = destination) }
    }

    override fun reopenApp() {
        workManager.scheduleAppReopening()
    }

    private fun observeDataSynchronizationStateWorker() {
        workManager.getDataSynchronizationProgressState()
            .filterNot { it is DataSynchronizationState.UncertainState }
            .onEach { dataSynchronizationState.value = it }
            .launchIn(scope = viewModelScope)
    }

    private fun getDestinationByDeckId(
        deckId: Int,
        ifDeckIsNotInterim: () -> DeckListNavigationDestination,
    ): DeckListNavigationDestination {
        return if (deckId == Deck.INTERIM_DECK_ID) {
            CardTransferringDestination(deckId = deckId)
        } else {
            ifDeckIsNotInterim()
        }
    }
}