package com.kuts.klaf.deckList.common

import androidx.lifecycle.viewModelScope
import com.kuts.domain.common.*
import com.kuts.domain.common.CoroutineStateHolder.Companion.launchWithState
import com.kuts.domain.common.CoroutineStateHolder.Companion.onException
import com.kuts.domain.common.CoroutineStateHolder.Companion.onExceptionWithCrashlyticsReport
import com.kuts.domain.managers.IAppMaintenanceManager
import com.kuts.domain.managers.IAuthenticationSessionManager
import com.kuts.domain.managers.IAccountSession
import com.kuts.domain.managers.AccountOperationException
import com.kuts.domain.managers.ImageSynchronizationException
import com.kuts.klaf.authentication.accountErrorMessage
import com.kuts.klaf.authentication.imageSyncErrorMessage
import com.kuts.domain.common.IDataSynchronizationState
import com.kuts.domain.common.IDataSynchronizationState.Failed
import com.kuts.domain.common.IDataSynchronizationState.Initial
import com.kuts.domain.common.IDataSynchronizationState.SuccessfullyFinished
import com.kuts.domain.common.IDataSynchronizationState.Synchronizing
import com.kuts.domain.common.IDataSynchronizationState.Uncertain
import com.kuts.domain.common.ICoroutineContextProvider
import com.kuts.domain.common.launchIn
import com.kuts.domain.entities.Deck
import com.kuts.domain.interactors.AuthenticationInteractor
import com.kuts.domain.repositories.IAuthenticationRepository
import com.kuts.domain.repositories.IAuthenticationRepository.IAccountDeletingError
import com.kuts.domain.repositories.ICrashlyticsRepository
import com.kuts.domain.useCases.*
import com.kuts.klaf.presentation.resources.*
import com.kuts.klaf.common.EventMessage
import com.kuts.klaf.common.NavigationDestination
import com.kuts.klaf.common.SecretConstants
import com.kuts.klaf.common.tryEmitAsNegative
import com.kuts.klaf.common.tryEmitAsPositive
import com.kuts.klaf.deckList.common.IDeckListNavigationDestination.DataSynchronizationDialog
import com.kuts.klaf.deckList.common.IDeckListNavigationDestination.Unspecified
import com.kuts.klaf.deckList.common.IDeckListNavigationEvent.*
import com.kuts.klaf.deckList.drawer.DrawerViewState
import com.kuts.klaf.server.contract.SyncHistoryItem
import com.lib.lokdroid.core.logE
import com.lib.lokdroid.core.logV
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch

class DeckListViewModel(
    fetchDeckSource: FetchDeckSourceUseCase,
    createInterimDeck: CreateInterimDeckUseCase,
    private val createDeck: CreateDeckUseCase,
    private val renameDeck: RenameDeckUseCase,
    private val removeDeck: RemoveDeckUseCase,
    private val fetchCardsUseCase: FetchCardsUseCase,
    private val authenticationSessionManager: IAuthenticationSessionManager,
    private val crashlytics: ICrashlyticsRepository,
    private val appMaintenanceManager: IAppMaintenanceManager,
    private val authenticationInteractor: AuthenticationInteractor,
    private val observeKlafServerConnectionState: ObserveKlafServerConnectionStateUseCase,
    private val retryKlafServerConnectionUseCase: RetryKlafServerConnectionUseCase,
    private val coroutineContextProvider: ICoroutineContextProvider,
    private val accountGateway: AccountDeckListGateway? = null,
    private val accountStatusGateway: AccountSyncStatusGateway? = null,
    private val accountSession: IAccountSession? = null,
) : BaseDeckListViewModel() {

    private companion object {

        private const val DEFAULT_CHAT_GPT_URL = "https://chatgpt.com/"
    }

    private val syncRouter = DeckListSyncRouter(
        accountGateway = accountGateway,
        legacySignedIn = authenticationSessionManager::isSignedIn,
        networkConnected = appMaintenanceManager::isNetworkConnected,
        startLegacyWorker = appMaintenanceManager::performDataSynchronization,
    )

    override val eventMessage = MutableSharedFlow<EventMessage>(extraBufferCapacity = 1)

    override val dataSynchronizationState = MutableStateFlow<IDataSynchronizationState>(Initial)

    override val accountSyncStatus = (accountStatusGateway?.status ?: flowOf(AccountSyncStatus()))
        .stateIn(viewModelScope, SharingStarted.Eagerly, AccountSyncStatus())

    override val deckSource: StateFlow<List<Deck>?> = (fetchDeckSource() as Flow<List<Deck>?>)
        .catchWithCrashlyticsReport(crashlytics = crashlytics) { throwable ->
            // logE("Failed to fetch deck source\n${throwable.stackTraceToString()}")
            this.emit(value = null)
        }
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
        logV {
            "INIT LIST"()
        }
        viewModelScope.launchWithState {
            appMaintenanceManager.initialize()
            createInterimDeck()
        }
            .onException { _, throwable ->
                // logE("Failed to create interim deck\n${throwable.stackTraceToString()}")
                crashlytics.report(exception = throwable)
            }
        if (accountGateway == null) observeDataSynchronizationStateWorker()
        accountGateway?.selectedAccountEmail
            ?.ensureGuestInterimDeck(createInterimDeck::invoke, crashlytics::report)
            ?.catch { failure -> crashlytics.report(exception = failure) }
            ?.launchIn(scope = viewModelScope)
        appMaintenanceManager.scheduleDeckRepetitionChecking()
        observeAuthenticationState()
    }

    override fun createNewDeck(deckName: String) {
        val decks = deckSource.value

        if (decks == null) {
            eventMessage.tryEmitAsNegative(resId = Res.string.problem_fetching_decks)

        } else {
            val deckNames = decks.map { deck -> deck.name }
            val trimmedDeckName = deckName.trim()

            when {
                deckNames.contains(deckName) -> {
                    eventMessage.tryEmitAsNegative(resId = Res.string.such_deck_is_already_exist)
                }

                trimmedDeckName.isEmpty() -> {
                    eventMessage.tryEmitAsNegative(resId = Res.string.warning_deck_name_empty)
                }

                else -> {
                    viewModelScope.launchWithState {
                        createDeck(
                            deck = Deck(name = deckName, creationDate = getCurrentDateAsLong())
                        )
                        eventMessage.tryEmitAsPositive(resId = Res.string.deck_has_been_created)
                        emitNavigationEvent(value = ToPrevious)
                    }.onExceptionWithCrashlyticsReport(crashlytics = crashlytics) { _, error ->
                        logE( "Failed to create deck: \"$deckName\" $error")
                        eventMessage.tryEmitAsNegative(resId = Res.string.problem_with_creating_deck)
                    }
                }
            }
        }
    }

    override fun renameDeck(deck: Deck, newName: String) {
        val updatedName = newName.trim()
        val decks = deckSource.value

        if (decks == null) {
            eventMessage.tryEmitAsNegative(resId = Res.string.problem_fetching_decks)
        } else {
            when {
                updatedName.isEmpty() -> {
                    eventMessage.tryEmitAsNegative(resId = Res.string.type_deck_name)
                }

                updatedName == deck.name -> {
                    eventMessage.tryEmitAsNegative(resId = Res.string.deck_name_is_not_changed)
                }

                decks.any { it.name == newName } -> {
                    eventMessage.tryEmitAsNegative(resId = Res.string.such_deck_is_already_exist)
                }

                else -> {
                    viewModelScope.launchWithState {
                        renameDeck(oldDeck = deck, name = updatedName)
                        eventMessage.tryEmitAsPositive(resId = Res.string.deck_has_been_renamed)
                        emitNavigationEvent(value = ToPrevious)
                    }.onExceptionWithCrashlyticsReport(crashlytics = crashlytics) { _, throwable ->
                        // logE("Failed to rename deck\n${throwable.stackTraceToString()}")
                        eventMessage.tryEmitAsPositive(resId = Res.string.problem_with_renaming_deck)
                    }
                }
            }
        }
    }

    override fun deleteDeck(deckId: Int) {
        viewModelScope.launchWithState {
            removeDeck(deckId = deckId)
            eventMessage.tryEmitAsPositive(resId = Res.string.the_deck_has_been_removed)
            emitNavigationEvent(value = ToPrevious)
        }.onExceptionWithCrashlyticsReport(crashlytics = crashlytics) { _, throwable ->
            // logE("Failed to delete deck\n${throwable.stackTraceToString()}")
            eventMessage.tryEmitAsNegative(resId = Res.string.problem_with_removing_deck)
        }
    }

    override fun getDeckById(deckId: Int): Deck? {
        val deck = deckSource.value?.find { deck -> deck.id == deckId }

        if (deck == null) {
            eventMessage.tryEmitAsNegative(resId = Res.string.problem_with_fetching_deck)
        }

        return deck
    }

    override fun synchronizeData() {
        viewModelScope.launch {
            try {
                if (accountGateway?.selectedAccountEmail?.firstOrNull() != null) {
                    dataSynchronizationState.value = Synchronizing("...")
                }
                when (syncRouter.synchronize()) {
                    DeckListSyncStart.NEEDS_SIGN_IN -> {
                        dataSynchronizationState.value = Initial
                        val source = NavigationDestination.DATA_SYNCHRONIZATION_DIALOG
                        emitNavigationEvent(value = ToSigningTypeChoosingDialog(fromSourceDestination = source))
                    }
                    DeckListSyncStart.NETWORK_UNAVAILABLE -> {
                        dataSynchronizationState.value = Initial
                        eventMessage.tryEmitAsNegative(
                            resId = Res.string.data_synchronization_network_connection_warning,
                        )
                    }
                    DeckListSyncStart.LEGACY_STARTED -> Unit
                    DeckListSyncStart.APPLIED -> {
                        dataSynchronizationState.value = SuccessfullyFinished
                        eventMessage.tryEmitAsPositive(
                            resId = Res.string.data_synchronization_dialog_data_synchronized,
                        )
                    }
                    DeckListSyncStart.NEEDS_RESOLUTION -> {
                        dataSynchronizationState.value = Failed
                        emitNavigationEvent(value = ToConflictResolutionScreen)
                    }
                }
            } catch (failure: CancellationException) {
                throw failure
            } catch (failure: Exception) {
                dataSynchronizationState.value = Failed
                crashlytics.report(exception = failure)
                eventMessage.tryEmitAsNegative(resId = when (failure) {
                    is AccountOperationException -> accountErrorMessage(failure)
                    is ImageSynchronizationException -> imageSyncErrorMessage(failure)
                    else -> Res.string.problem_with_data_synchronization
                })
            }
        }
    }

    override suspend fun recentSyncHistory(accountEmail: String): List<SyncHistoryItem> =
        requireNotNull(accountStatusGateway) { "Recent history requires account storage" }
            .recentHistory(accountEmail)

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
        if (accountGateway != null) {
            if (!accountSignOutAllowed(true, dataSynchronizationState.value, accountSyncStatus.value)) return
            viewModelScope.launch {
                drawerActionLoadingState.value = true
                try {
                    accountGateway.signOut()
                    emitNavigationEvent(value = ToPrevious)
                    eventMessage.tryEmitAsPositive(resId = Res.string.log_out_success_message)
                } catch (failure: CancellationException) {
                    throw failure
                } catch (failure: Exception) {
                    crashlytics.report(exception = failure)
                    eventMessage.tryEmitAsNegative(resId = Res.string.log_out_failure_message)
                } finally {
                    drawerActionLoadingState.value = false
                }
            }
            return
        }
        authenticationInteractor.logOut().onEach { loadingState ->
            drawerActionLoadingState.value = loadingState is LoadingState.Loading

            when (loadingState) {
                is LoadingState.Success -> {
                    emitNavigationEvent(value = ToPrevious)
                    eventMessage.tryEmitAsPositive(resId = Res.string.log_out_success_message)
                }

                is LoadingState.Error -> {
                    // logE("Logout failed with state error: ${loadingState.value}")
                    eventMessage.tryEmitAsNegative(resId = Res.string.log_out_failure_message)
                }

                LoadingState.Loading -> {}
                LoadingState.Non -> {}
            }
        }.launchIn(viewModelScope)
    }

    override fun deleteAccount() {
        if (accountGateway != null) return
        authenticationInteractor.deleteAccount().flowOn(context = coroutineContextProvider.io)
            .onEach { loadingState ->
                drawerActionLoadingState.value = loadingState is LoadingState.Loading

                when (loadingState) {
                    is LoadingState.Success -> {
                        emitNavigationEvent(value = ToPrevious)
                        eventMessage.tryEmitAsPositive(resId = Res.string.delete_account_success_message)
                    }

                    is LoadingState.Error -> {
                        // logE("Delete account failed with state error: ${loadingState.value}")
                        handleAccountDeletingError(throwable = loadingState.value)
                    }

                    LoadingState.Loading -> {}
                    LoadingState.Non -> {}
                }
            }.launchIn(scope = viewModelScope)
    }

    override fun generateGptPromptWithDeckContent(deckId: Int) {
        viewModelScope.launch(coroutineContextProvider.io) {
            val cards = fetchCardsUseCase.invoke(deckId = deckId).firstOrNull() ?: return@launch
            val foreignWords = cards.joinToString { it.foreignWord }
            val chatGptUrl = getValidatedChatGptUrl()

            val message = EventMessage(
                resId = Res.string.chat_gpt_story_crafter_prompt_is_created_and_copied,
                duration = EventMessage.Duration.Short
            )

            navigationEvent.emit(
                ToChatGptWithDeckContentPrompt(
                    foreignWords = foreignWords,
                    chatGptUrl = chatGptUrl,
                    event = message,
                )
            )
        }
    }

    override fun retryKlafServerConnection() {
        viewModelScope.launchWithState {
            retryKlafServerConnectionUseCase()
        }.onException { _, throwable ->
            crashlytics.report(exception = throwable)
        }
    }

    private fun getValidatedChatGptUrl(): String {
        val rawUrl = SecretConstants.ChatGpt.STORY_CRAFTER_URL.trim()

        if (rawUrl.isBlank() || rawUrl.equals("empty", ignoreCase = true)) {
            return DEFAULT_CHAT_GPT_URL
        }

        val hasValidScheme = rawUrl.startsWith(prefix = "https://", ignoreCase = true) ||
                rawUrl.startsWith(prefix = "http://", ignoreCase = true)
        val host = rawUrl.substringAfter(delimiter = "://", missingDelimiterValue = "")
            .substringBefore(delimiter = "/")
            .substringBefore(delimiter = "?")
            .substringBefore(delimiter = "#")
            .trim()
        val hasHost = host.isNotBlank()

        return if (hasValidScheme && hasHost) rawUrl else DEFAULT_CHAT_GPT_URL
    }

    private fun observeDataSynchronizationStateWorker() {
        appMaintenanceManager.observeDataSynchronizationState()
            .catch {
                // logE("Failed to observe synchronization state worker\n${it.stackTraceToString()}")
                crashlytics.report(exception = it)
            }
            .filterNot { it is Uncertain }
            .flowOn(context = coroutineContextProvider.io)
            .onEach {
                dataSynchronizationState.value = it

                if (navigationDestination.value != DataSynchronizationDialog) {
                    when (it) {
                        is Failed -> {
                            eventMessage.tryEmitAsNegative(resId = Res.string.problem_with_data_synchronization)
                        }

                        is SuccessfullyFinished -> {
                            eventMessage.tryEmitAsPositive(resId = Res.string.data_synchronization_dialog_data_synchronized)
                        }

                        else -> {}
                    }
                }
            }.launchIn(scope = viewModelScope)
    }

    private fun observeAuthenticationState() {
        val emailSource = accountGateway?.selectedAccountEmail
            ?: authenticationInteractor.getObservableAuthenticationState().map { it.email }
        val pendingSource = accountSession?.pendingEnrollment ?: flowOf(null)
        combine(
            emailSource,
            observeKlafServerConnectionState(),
            dataSynchronizationState,
            accountSyncStatus,
            pendingSource,
        ) { email, connectionState, synchronizationState, accountStatus, pending ->
            DrawerViewState(
                signedIn = email != null,
                userEmail = email,
                klafServerConnectionState = connectionState,
                canDeleteAccount = accountGateway == null,
                canSignOut = accountSignOutAllowed(accountGateway != null, synchronizationState, accountStatus),
                pendingEnrollment = pending,
            )
        }.flowOn(coroutineContextProvider.io)
            .onEach { drawerViewState -> drawerState.emit(drawerViewState) }
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
                IAccountDeletingError.CommonError -> Res.string.delete_account_failure_message
                IAccountDeletingError.NetworkError -> Res.string.authentication_warning_network_error
                IAccountDeletingError.RecentLoginRequired -> TODO()
            }
        } else {
            Res.string.delete_account_failure_message
        }

        eventMessage.tryEmitAsNegative(resId = messageId)
    }

}
