package com.kuts.klaf.presentation.deckList.common

import androidx.lifecycle.viewModelScope
import androidx.work.WorkManager
import com.google.firebase.auth.FirebaseAuth
import com.kuts.domain.common.CoroutineStateHolder.Companion.launchWithState
import com.kuts.domain.common.CoroutineStateHolder.Companion.onException
import com.kuts.domain.common.CoroutineStateHolder.Companion.onExceptionWithCrashlyticsReport
import com.kuts.domain.common.catchWithCrashlyticsReport
import com.kuts.domain.common.getCurrentDateAsLong
import com.kuts.domain.common.ifTrue
import com.kuts.domain.common.launchIn
import com.kuts.domain.entities.Deck
import com.kuts.domain.repositories.CrashlyticsRepository
import com.kuts.domain.useCases.*
import com.kuts.klaf.R
import com.kuts.klaf.data.common.AppReopeningWorker.Companion.scheduleAppReopening
import com.kuts.klaf.data.common.DataSynchronizationState
import com.kuts.klaf.data.common.DataSynchronizationWorker.Companion.getDataSynchronizationProgressState
import com.kuts.klaf.data.common.DataSynchronizationWorker.Companion.performDataSynchronization
import com.kuts.klaf.data.common.DeckRepetitionReminderChecker.Companion.scheduleDeckRepetitionChecking
import com.kuts.klaf.data.common.NetworkConnectivity
import com.kuts.klaf.data.common.notifications.NotificationChannelInitializer
import com.kuts.klaf.presentation.common.EventMessage
import com.kuts.klaf.presentation.common.tryEmitAsNegative
import com.kuts.klaf.presentation.common.tryEmitAsPositive
import com.kuts.klaf.presentation.deckList.common.DeckListNavigationDestination.DataSynchronizationDialog
import com.kuts.klaf.presentation.deckList.common.DeckListNavigationDestination.Unspecified
import com.kuts.klaf.presentation.deckList.common.DeckListNavigationEvent.*
import dagger.assisted.AssistedInject
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class DeckListViewModel @AssistedInject constructor(
    fetchDeckSource: FetchDeckSourceUseCase,
    createInterimDeck: CreateInterimDeckUseCase,
    notificationChannelInitializer: NotificationChannelInitializer,
    private val createDeck: CreateDeckUseCase,
    private val renameDeck: RenameDeckUseCase,
    private val removeDeck: RemoveDeckUseCase,
    private val workManager: WorkManager,
    private val auth: FirebaseAuth,
    private val crashlytics: CrashlyticsRepository,
    private val networkConnectivity: NetworkConnectivity,
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
            eventMessage.tryEmitAsNegative(resId = R.string.problem_fetching_decks)

        } else {
            val deckNames = decks.map { deck -> deck.name }
            val trimmedDeckName = deckName.trim()

            when {
                deckNames.contains(deckName) -> {
                    eventMessage.tryEmitAsNegative(resId = R.string.such_deck_is_already_exist)
                }
                trimmedDeckName.isEmpty() -> {
                    eventMessage.tryEmitAsNegative(resId = R.string.warning_deck_name_empty)
                }
                else -> {
                    viewModelScope.launchWithState {
                        createDeck(
                            deck = Deck(name = deckName, creationDate = getCurrentDateAsLong())
                        )
                        eventMessage.tryEmitAsPositive(resId = R.string.deck_has_been_created)
                        navigationEvent.emit(value = ToPrevious)
                    }.onExceptionWithCrashlyticsReport(crashlytics = crashlytics) { _, _ ->
                        eventMessage.tryEmitAsNegative(resId = R.string.problem_with_creating_deck)
                    }
                }
            }
        }
    }

    override fun renameDeck(deck: Deck, newName: String) {
        val updatedName = newName.trim()
        val decks = deckSource.value

        if (decks == null) {
            eventMessage.tryEmitAsNegative(resId = R.string.problem_fetching_decks)
        } else {
            when {
                updatedName.isEmpty() -> {
                    eventMessage.tryEmitAsNegative(resId = R.string.type_deck_name)
                }
                updatedName == deck.name -> {
                    eventMessage.tryEmitAsNegative(resId = R.string.deck_name_is_not_changed)
                }
                decks.any { it.name == newName } -> {
                    eventMessage.tryEmitAsNegative(resId = R.string.such_deck_is_already_exist)
                }
                else -> {
                    viewModelScope.launchWithState {
                        renameDeck(oldDeck = deck, name = updatedName)
                        eventMessage.tryEmitAsPositive(resId = R.string.deck_has_been_renamed)
                        navigationEvent.emit(value = ToPrevious)
                    }.onExceptionWithCrashlyticsReport(crashlytics = crashlytics) { _, _ ->
                        eventMessage.tryEmitAsPositive(resId = R.string.problem_with_renaming_deck)
                    }
                }
            }
        }
    }

    override fun deleteDeck(deckId: Int) {
        viewModelScope.launchWithState {
            removeDeck(deckId = deckId)
            eventMessage.tryEmitAsPositive(resId = R.string.the_deck_has_been_removed)
            navigationEvent.emit(value = ToPrevious)
        }.onExceptionWithCrashlyticsReport(crashlytics = crashlytics) { _, _ ->
            eventMessage.tryEmitAsNegative(resId = R.string.problem_with_removing_deck)
        }
    }

    override fun getDeckById(deckId: Int): Deck? {
        val deck = deckSource.value?.find { deck -> deck.id == deckId }

        if (deck == null) {
            eventMessage.tryEmitAsNegative(resId = R.string.problem_with_fetching_deck)
        }

        return deck
    }

    override fun synchronizeData() {
        if (auth.currentUser == null) {
            viewModelScope.launch { navigationEvent.emit(value = ToSigningTypeChoosingDialog) }
        } else {
            if (networkConnectivity.isNetworkConnected()) {
                workManager.performDataSynchronization()
            } else {
                eventMessage.tryEmitAsNegative(
                    resId = R.string.data_synchronization_network_connection_warning
                )
            }
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
                    eventMessage.tryEmitAsNegative(resId = R.string.problem_with_data_synchronization)
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