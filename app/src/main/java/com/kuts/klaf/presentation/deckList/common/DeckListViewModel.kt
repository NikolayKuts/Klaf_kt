package com.kuts.klaf.presentation.deckList.common

import androidx.lifecycle.viewModelScope
import androidx.work.WorkManager
import com.google.firebase.auth.FirebaseAuth
import com.kuts.domain.common.*
import com.kuts.domain.common.CoroutineStateHolder.Companion.launchWithState
import com.kuts.domain.common.CoroutineStateHolder.Companion.onException
import com.kuts.domain.common.CoroutineStateHolder.Companion.onExceptionWithCrashlyticsReport
import com.kuts.domain.common.launchIn
import com.kuts.domain.entities.Deck
import com.kuts.domain.interactors.AuthenticationInteractor
import com.kuts.domain.repositories.CrashlyticsRepository
import com.kuts.domain.useCases.*
import com.kuts.klaf.R
import com.kuts.klaf.data.common.AppReopeningWorker.Companion.scheduleAppReopening
import com.kuts.klaf.data.common.DataSynchronizationState
import com.kuts.klaf.data.common.DataSynchronizationState.*
import com.kuts.klaf.data.common.DataSynchronizationWorker.Companion.getDataSynchronizationProgressState
import com.kuts.klaf.data.common.DataSynchronizationWorker.Companion.performDataSynchronization
import com.kuts.klaf.data.common.DeckRepetitionReminderChecker.Companion.scheduleDeckRepetitionChecking
import com.kuts.klaf.data.common.NetworkConnectivity
import com.kuts.klaf.data.common.notifications.NotificationChannelInitializer
import com.kuts.klaf.data.firestore.repositoryImplementations.AuthenticationRepositoryFirebaseImp
import com.kuts.klaf.data.firestore.repositoryImplementations.AuthenticationRepositoryFirebaseImp.*
import com.kuts.klaf.presentation.common.EventMessage
import com.kuts.klaf.presentation.common.NavigationDestination
import com.kuts.klaf.presentation.common.tryEmitAsNegative
import com.kuts.klaf.presentation.common.tryEmitAsPositive
import com.kuts.klaf.presentation.deckList.common.DeckListNavigationDestination.DataSynchronizationDialog
import com.kuts.klaf.presentation.deckList.common.DeckListNavigationDestination.Unspecified
import com.kuts.klaf.presentation.deckList.common.DeckListNavigationEvent.*
import com.kuts.klaf.presentation.deckList.drawer.DrawerViewState
import dagger.assisted.AssistedInject
import kotlinx.coroutines.Dispatchers
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
    private val authenticationInteractor: AuthenticationInteractor,
) : BaseDeckListViewModel() {

    override val eventMessage = MutableSharedFlow<EventMessage>(extraBufferCapacity = 1)

    override val dataSynchronizationState =
        MutableStateFlow<DataSynchronizationState>(Initial)

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

    override val navigationEvent = MutableSharedFlow<DeckListNavigationEvent?>()

    override val shouldSynchronizationIndicatorBeShown = combine(
        dataSynchronizationState,
        navigationDestination,
    ) { synchronizationState, destination ->
        synchronizationState is Synchronizing && destination is Unspecified
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.Eagerly,
        initialValue = false,
    )

    override val drawerState = MutableSharedFlow<DrawerViewState>(replay = 1)

    override val drawerActionLoadingState = MutableStateFlow(value = false)

    init {
        notificationChannelInitializer.initialize()
        viewModelScope.launchWithState { createInterimDeck() }
            .onException { _, throwable -> crashlytics.report(exception = throwable) }
        observeDataSynchronizationStateWorker()
        workManager.scheduleDeckRepetitionChecking()
        observeAuthenticationState()
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
                        emitNavigationEvent(value = ToPrevious)
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
                        emitNavigationEvent(value = ToPrevious)
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
            emitNavigationEvent(value = ToPrevious)
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
            viewModelScope.launch {
                val source = NavigationDestination.DATA_SYNCHRONIZATION_DIALOG
                emitNavigationEvent(value = ToSigningTypeChoosingDialog(fromSourceDestination = source))
            }
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

            emitNavigationEvent(value = targetEvent)
        }
    }

    override fun reopenApp() {
        workManager.scheduleAppReopening()
    }

    override fun resetSynchronizationState() {
        if (dataSynchronizationState.value == SuccessfullyFinished) {
            dataSynchronizationState.value = Initial
        }
    }

    override fun logOut() {
        authenticationInteractor.logOut().onEach { loadingState ->
            drawerActionLoadingState.value = loadingState is LoadingState.Loading

            when (loadingState) {
                is LoadingState.Success -> {
                    emitNavigationEvent(value = ToPrevious)
                    eventMessage.tryEmitAsPositive(resId = R.string.log_out_success_message)
                }
                is LoadingState.Error -> {
                    eventMessage.tryEmitAsNegative(resId = R.string.log_out_failure_message)
                }
                LoadingState.Loading -> {}
                LoadingState.Non -> {}
            }
        }.launchIn(viewModelScope)
    }

    override fun deleteAccount() {
        authenticationInteractor.deleteAccount().flowOn(context = Dispatchers.IO)
            .onEach { loadingState ->
                drawerActionLoadingState.value = loadingState is LoadingState.Loading

                when (loadingState) {
                    is LoadingState.Success -> {
                        emitNavigationEvent(value = ToPrevious)
                        eventMessage.tryEmitAsPositive(resId = R.string.delete_account_success_message)
                    }
                    is LoadingState.Error -> {
                        handleAccountDeletingError(throwable = loadingState.value)
                    }
                    LoadingState.Loading -> {}
                    LoadingState.Non -> {}
                }
            }.launchIn(scope = viewModelScope)
    }

    private fun observeDataSynchronizationStateWorker() {
        workManager.getDataSynchronizationProgressState()
            .catch { crashlytics.report(exception = it) }
            .filterNot { it is Uncertain }
            .flowOn(context = Dispatchers.IO)
            .onEach {
                dataSynchronizationState.value = it

                if (navigationDestination.value != DataSynchronizationDialog) {
                    when (it) {
                        is Failed -> {
                            eventMessage.tryEmitAsNegative(resId = R.string.problem_with_data_synchronization)
                        }
                        is SuccessfullyFinished -> {
                            eventMessage.tryEmitAsPositive(resId = R.string.data_synchronization_dialog_data_synchronized)
                        }
                        else -> {}
                    }
                }
            }.launchIn(scope = viewModelScope)
    }

    private fun observeAuthenticationState() {
        authenticationInteractor.getObservableAuthenticationState()
            .flowOn(Dispatchers.IO)
            .onEach {
                drawerState.emit(
                    DrawerViewState(
                        signedIn = it.email.isNotNull(),
                        userEmail = it.email),
                )
            }
            .launchIn(scope = viewModelScope)
    }

    private fun getEventByDeckId(
        deckId: Int,
        ifDeckIsNotInterim: () -> DeckListNavigationEvent,
    ): DeckListNavigationEvent = if (deckId == Deck.INTERIM_DECK_ID) {
        ToCardTransferringScreen(deckId = deckId)
    } else {
        ifDeckIsNotInterim()
    }

    private suspend fun emitNavigationEvent(value: DeckListNavigationEvent) {
        val actualEvent = when {
            value is ToDataSynchronizationDialog -> value
            value is ToPrevious -> value
            dataSynchronizationState.value !is Synchronizing -> value
            else -> null
        }

        navigationEvent.emit(value = actualEvent)
    }

    private fun handleAccountDeletingError(throwable: LoadingError) {
        val messageId = if (throwable is AccountDeletingError) {
            when (throwable) {
                AccountDeletingError.CommonError -> R.string.delete_account_failure_message
                AccountDeletingError.NetworkError -> R.string.authentication_warning_network_error
                AccountDeletingError.RecentLoginRequired -> TODO()
            }
        } else {
            R.string.delete_account_failure_message
        }

        eventMessage.tryEmitAsNegative(resId = messageId)
    }
}