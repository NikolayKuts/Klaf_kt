package com.kuts.klaf.deckList.common

import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.kuts.domain.common.*
import com.kuts.domain.common.CoroutineStateHolder.Companion.launchWithState
import com.kuts.domain.common.CoroutineStateHolder.Companion.onException
import com.kuts.domain.common.CoroutineStateHolder.Companion.onExceptionWithCrashlyticsReport
import com.kuts.domain.managers.IAppMaintenanceManager
import com.kuts.domain.common.IDataSynchronizationState
import com.kuts.domain.common.IDataSynchronizationState.Failed
import com.kuts.domain.common.IDataSynchronizationState.Initial
import com.kuts.domain.common.IDataSynchronizationState.SuccessfullyFinished
import com.kuts.domain.common.IDataSynchronizationState.Synchronizing
import com.kuts.domain.common.IDataSynchronizationState.Uncertain
import com.kuts.domain.common.launchIn
import com.kuts.domain.entities.Deck
import com.kuts.domain.interactors.AuthenticationInteractor
import com.kuts.domain.repositories.IAuthenticationRepository
import com.kuts.domain.repositories.IAuthenticationRepository.IAccountDeletingError
import com.kuts.domain.repositories.ICrashlyticsRepository
import com.kuts.domain.useCases.*
import com.kuts.klaf.presentation.R
import com.kuts.klaf.common.EventMessage
import com.kuts.klaf.common.NavigationDestination
import com.kuts.klaf.common.tryEmitAsNegative
import com.kuts.klaf.common.tryEmitAsPositive
import com.kuts.klaf.deckList.common.IDeckListNavigationDestination.DataSynchronizationDialog
import com.kuts.klaf.deckList.common.IDeckListNavigationDestination.Unspecified
import com.kuts.klaf.deckList.common.IDeckListNavigationEvent.*
import com.kuts.klaf.deckList.drawer.DrawerViewState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class DeckListViewModel(
    fetchDeckSource: FetchDeckSourceUseCase,
    createInterimDeck: CreateInterimDeckUseCase,
    private val createDeck: CreateDeckUseCase,
    private val renameDeck: RenameDeckUseCase,
    private val removeDeck: RemoveDeckUseCase,
    private val fetchCardsUseCase: FetchCardsUseCase,
    private val auth: FirebaseAuth,
    private val crashlytics: ICrashlyticsRepository,
    private val appMaintenanceManager: IAppMaintenanceManager,
    private val authenticationInteractor: AuthenticationInteractor,
) : BaseDeckListViewModel() {

    override val eventMessage = MutableSharedFlow<EventMessage>(extraBufferCapacity = 1)

    override val dataSynchronizationState =
        MutableStateFlow<IDataSynchronizationState>(Initial)

    override val deckSource: StateFlow<List<Deck>?> = (fetchDeckSource() as Flow<List<Deck>?>)
        .catchWithCrashlyticsReport(crashlytics = crashlytics) { this.emit(value = null) }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.Eagerly,
            initialValue = emptyList()
        )

    override val navigationDestination = MutableStateFlow<IDeckListNavigationDestination>(
        value = Unspecified
    )

    override val navigationEvent = MutableSharedFlow<IDeckListNavigationEvent?>()

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
        appMaintenanceManager.initialize()
        viewModelScope.launchWithState { createInterimDeck() }
            .onException { _, throwable -> crashlytics.report(exception = throwable) }
        observeDataSynchronizationStateWorker()
//        appMaintenanceManager.scheduleDeckRepetitionChecking()
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
            if (appMaintenanceManager.isNetworkConnected()) {
                appMaintenanceManager.performDataSynchronization()
            } else {
                eventMessage.tryEmitAsNegative(
                    resId = R.string.data_synchronization_network_connection_warning
                )
            }
        }
    }

    override fun handleNavigation(event: IDeckListNavigationEvent) {
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
        appMaintenanceManager.scheduleAppReopening()
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

    override fun generateGptPromptWithDeckContent(deckId: Int) {
        viewModelScope.launch(Dispatchers.IO) {
            val cards = fetchCardsUseCase.invoke(deckId = deckId).firstOrNull() ?: return@launch
            val foreignWords = cards.joinToString { it.foreignWord }

            val message = EventMessage(
                resId = R.string.chat_gpt_story_crafter_prompt_is_created_and_copied,
                duration = EventMessage.Duration.Short
            )

            navigationEvent.emit(
                ToChatGptWithDeckContentPrompt(foreignWords = foreignWords, event = message)
            )
        }
    }

    private fun observeDataSynchronizationStateWorker() {
        appMaintenanceManager.observeDataSynchronizationState()
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
                        userEmail = it.email
                    ),
                )
            }
            .launchIn(scope = viewModelScope)
    }

    private fun getEventByDeckId(
        deckId: Int,
        ifDeckIsNotInterim: () -> IDeckListNavigationEvent,
    ): IDeckListNavigationEvent = if (deckId == Deck.INTERIM_DECK_ID) {
        ToCardTransferringScreen(deckId = deckId)
    } else {
        ifDeckIsNotInterim()
    }

    private suspend fun emitNavigationEvent(value: IDeckListNavigationEvent) {
        val actualEvent = when {
            value is ToDataSynchronizationDialog -> value
            value is ToPrevious -> value
            dataSynchronizationState.value !is Synchronizing -> value
            else -> null
        }

        navigationEvent.emit(value = actualEvent)
    }

    private fun handleAccountDeletingError(throwable: IAuthenticationRepository.IAuthenticationError) {
        val messageId = if (throwable is IAccountDeletingError) {
            when (throwable) {
                IAccountDeletingError.CommonError -> R.string.delete_account_failure_message
                IAccountDeletingError.NetworkError -> R.string.authentication_warning_network_error
                IAccountDeletingError.RecentLoginRequired -> TODO()
            }
        } else {
            R.string.delete_account_failure_message
        }

        eventMessage.tryEmitAsNegative(resId = messageId)
    }
}
