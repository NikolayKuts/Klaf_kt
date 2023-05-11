package com.example.klaf.presentation.deckList.common

import androidx.lifecycle.viewModelScope
import androidx.work.WorkManager
import com.example.domain.common.CoroutineStateHolder.Companion.launchWithState
import com.example.domain.common.CoroutineStateHolder.Companion.onException
import com.example.domain.common.CoroutineStateHolder.Companion.onExceptionWithCrashlyticsReport
import com.example.domain.common.catchWithCrashlyticsReport
import com.example.domain.common.getCurrentDateAsLong
import com.example.domain.common.ifTrue
import com.example.domain.common.launchIn
import com.example.domain.entities.Deck
import com.example.domain.repositories.CrashlyticsRepository
import com.example.domain.useCases.*
import com.example.klaf.R
import com.example.klaf.data.common.AppReopeningWorker.Companion.scheduleAppReopening
import com.example.klaf.data.common.DataSynchronizationState
import com.example.klaf.data.common.DataSynchronizationWorker.Companion.getDataSynchronizationProgressState
import com.example.klaf.data.common.DataSynchronizationWorker.Companion.performDataSynchronization
import com.example.klaf.data.common.DeckRepetitionReminderChecker.Companion.scheduleDeckRepetitionChecking
import com.example.klaf.data.common.notifications.NotificationChannelInitializer
import com.example.klaf.presentation.common.EventMessage
import com.example.klaf.presentation.common.tryEmit
import com.example.klaf.presentation.deckList.common.DeckListNavigationDestination.DataSynchronizationDialog
import com.example.klaf.presentation.deckList.common.DeckListNavigationDestination.Unspecified
import com.example.klaf.presentation.deckList.common.DeckListNavigationEvent.*
import com.google.firebase.auth.FirebaseAuth
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
    private val auth: FirebaseAuth,
    private val crashlytics: CrashlyticsRepository,
) : BaseDeckListViewModel() {

    override val eventMessage = MutableSharedFlow<EventMessage>(extraBufferCapacity = 1)

    override val dataSynchronizationState =
        MutableStateFlow<DataSynchronizationState>(DataSynchronizationState.Initial)

    override val deckSource: StateFlow<List<Deck>?> = (fetchDeckSource() as Flow<List<Deck>?>)
        .catchWithCrashlyticsReport(crashlytics = crashlytics) { this.emit(value = null) }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.Eagerly,
            initialValue = emptyList()
        )

    override val navigationDestination = MutableStateFlow<DeckListNavigationDestination>(
        value = Unspecified
    )

    override val navigationEvent = MutableSharedFlow<DeckListNavigationEvent>()

    init {
        notificationChannelInitializer.initialize()
        viewModelScope.launchWithState { createInterimDeck() }
            .onException { _, throwable -> crashlytics.report(exception = throwable) }
        observeDataSynchronizationStateWorker()
        workManager.scheduleDeckRepetitionChecking()
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
                        navigationEvent.emit(value = ToPrevious)
                    }.onExceptionWithCrashlyticsReport(crashlytics = crashlytics) { _, _ ->
                        eventMessage.tryEmit(messageId = R.string.problem_with_creating_deck)
                    }
                }
            }
        }
    }

    override fun renameDeck(deck: Deck, newName: String) {
        val updatedName = newName.trim()
        val decks = deckSource.value

        if (decks == null) {
            eventMessage.tryEmit(messageId = R.string.problem_fetching_decks)
        } else {
            when {
                updatedName.isEmpty() -> {
                    eventMessage.tryEmit(messageId = R.string.type_deck_name)
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
                        navigationEvent.emit(value = ToPrevious)
                    }.onExceptionWithCrashlyticsReport(crashlytics = crashlytics) { _, _ ->
                        eventMessage.tryEmit(messageId = R.string.problem_with_renaming_deck)
                    }
                }
            }
        }
    }

    override fun deleteDeck(deckId: Int) {
        viewModelScope.launchWithState {
            removeDeck(deckId = deckId)
            eventMessage.tryEmit(messageId = R.string.the_deck_has_been_removed)
            navigationEvent.emit(value = ToPrevious)
        }.onExceptionWithCrashlyticsReport(crashlytics = crashlytics) { _, _ ->
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
        if (auth.currentUser == null) {
            viewModelScope.launch { navigationEvent.emit(value = ToSigningTypeChoosingDialog) }
        } else {
            workManager.performDataSynchronization()
        }
    }

    override fun handleNavigation(event: DeckListNavigationEvent) {
        val targetEvent = when (event) {
            is ToDeckRepetitionScreen -> {
                getEventByDeckId(
                    deckId = event.deck.id,
                    ifDeckIsNotInterim = { ToDeckRepetitionScreen(deck = event.deck) }
                )
            }
            is ToDeckNavigationDialog -> {
                getEventByDeckId(
                    deckId = event.deck.id,
                    ifDeckIsNotInterim = { ToDeckNavigationDialog(deck = event.deck) }
                )
            }
            else -> event
        }

        viewModelScope.launch {
            navigationDestination.value = if (event == ToDataSynchronizationDialog) {
                DataSynchronizationDialog
            } else {
                Unspecified
            }

            navigationEvent.emit(value = targetEvent)
        }
    }

    override fun reopenApp() {
        workManager.scheduleAppReopening()
    }

    private fun observeDataSynchronizationStateWorker() {
        workManager.getDataSynchronizationProgressState()
            .catch { crashlytics.report(exception = it) }
            .filterNot { it is DataSynchronizationState.Uncertain }
            .onEach {
                dataSynchronizationState.value = it

                val shouldSendEventMessage = it is DataSynchronizationState.Failed
                        && navigationDestination.value != DataSynchronizationDialog

                shouldSendEventMessage.ifTrue {
                    eventMessage.tryEmit(messageId = R.string.problem_with_data_synchronization)
                }
            }.launchIn(scope = viewModelScope)
    }

    override fun resetSynchronizationState() {
        if (dataSynchronizationState.value == DataSynchronizationState.SuccessfullyFinished) {
            dataSynchronizationState.value = DataSynchronizationState.Initial
        }
    }

    private fun getEventByDeckId(
        deckId: Int,
        ifDeckIsNotInterim: () -> DeckListNavigationEvent,
    ): DeckListNavigationEvent = if (deckId == Deck.INTERIM_DECK_ID) {
        ToCardTransferringScreen(deckId = deckId)
    } else {
        ifDeckIsNotInterim()
    }
}