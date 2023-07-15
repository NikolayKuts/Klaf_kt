package com.kuts.klaf.presentation.deckList.common

import android.os.Bundle
import android.view.View
import androidx.compose.material.*
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.platform.ComposeView
import androidx.navigation.fragment.findNavController
import androidx.navigation.navGraphViewModels
import com.kuts.domain.common.AuthenticationAction
import com.kuts.domain.entities.Deck
import com.kuts.klaf.R
import com.kuts.klaf.presentation.authentication.AuthenticationFragment.Companion.setAuthenticationFragmentResultListener
import com.kuts.klaf.presentation.common.*
import com.kuts.klaf.presentation.deckList.common.DeckListNavigationEvent.*
import com.kuts.klaf.presentation.deckList.dataSynchronization.DataSynchronizationDialogFragmentDirections
import com.kuts.klaf.presentation.deckList.drawer.Drawer
import com.kuts.klaf.presentation.deckList.drawer.DrawerAction
import com.kuts.klaf.presentation.deckList.drawer.DrawerViewState
import com.kuts.klaf.presentation.theme.MainTheme
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class DeckListFragment : BaseFragment(layoutId = R.layout.common_compose_layout) {

    private val navController by lazy { findNavController() }

    @Inject
    lateinit var assistedFactory: DeckListViewModelAssistedFactory
    private val viewModel: BaseDeckListViewModel by navGraphViewModels(R.id.deckListFragment) {
        DeckListViewModelFactory(assistedFactory = assistedFactory)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        observeNavigationEvent()
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        observeEvenMessage()
        observeAuthenticationResult()

        view.findViewById<ComposeView>(R.id.compose_view).setContent {
            MainTheme {
                Surface {
                    val scaffoldState = rememberScaffoldState(
                        drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
                    )

                    val scope = rememberCoroutineScope()

                    Scaffold(
                        scaffoldState = scaffoldState,
                        drawerContent = {
                            Drawer(
                                state = viewModel.drawerState.collectAsState(
                                    initial = DrawerViewState(signedIn = false, userEmail = null)
                                ).value,
                                onLogInClick = {
                                    scope.launch {
                                        scaffoldState.drawerState.close()
                                        val event = ToSigningTypeChoosingDialog(
                                            fromSourceDestination = NavigationDestination.DECK_LIST_FRAGMENT
                                        )
                                        viewModel.handleNavigation(event = event)
                                    }
                                },
                                onLogOutClick = {
                                    scope.launch {
                                        scaffoldState.drawerState.close()
                                        viewModel.handleNavigation(
                                            event = ToDrawerActionDialog(action = DrawerAction.LOG_OUT)
                                        )
                                    }
                                },
                                onDeleteAccountClick = {
                                    scope.launch {
                                        scaffoldState.drawerState.close()
                                        viewModel.handleNavigation(
                                            event = ToDrawerActionDialog(action = DrawerAction.DELETE_ACCOUNT)
                                        )
                                    }
                                },

                                )
                        },
//                        drawerGesturesEnabled = false,
//                        drawerShape =,
//                        drawerElevation =,
//                        drawerBackgroundColor =,
//                        drawerContentColor =,
//                        drawerScrimColor =,
//                        backgroundColor =,
//                        contentColor =,
                    ) { paddingValues ->
                        DeckListScreen(
                            decks = viewModel.deckSource.collectAsState().value,
                            shouldSynchronizationIndicatorBeShown = viewModel.shouldSynchronizationIndicatorBeShown
                                .collectAsState().value,
                            contentPadding = paddingValues,
                            onItemClick = {
                                viewModel.handleNavigation(event = ToDeckRepetitionScreen(deck = it))
                            },
                            onLongItemClick = {
                                viewModel.handleNavigation(event = ToDeckNavigationDialog(deck = it))
                            },
                            onRefresh = {
                                viewModel.handleNavigation(event = ToDataSynchronizationDialog)
                            },
                            onMainButtonClick = {
                                viewModel.handleNavigation(event = ToDeckCreationDialog)
                            },
                            onRestartApp = ::restartApp,
                        )
                    }
                }
            }
        }
    }

    private fun observeEvenMessage() {
        viewModel.eventMessage.collectWhenStarted(
            lifecycleOwner = viewLifecycleOwner,
            onEach = sharedViewModel::notify
        )
    }

    private fun observeAuthenticationResult() {
        setAuthenticationFragmentResultListener { authenticationResult ->
            if (authenticationResult.isSuccessful) {
                val messageId = when (authenticationResult.action) {
                    AuthenticationAction.SIGN_IN -> R.string.authentication_sign_in_success
                    AuthenticationAction.SIGN_UP -> R.string.authentication_sign_up_success
                }

                sharedViewModel.notify(
                    message = EventMessage(resId = messageId, type = EventMessage.Type.Positive)
                )
            }
        }
    }

    private fun observeNavigationEvent() {
        viewModel.navigationEvent.collectWhenStarted(lifecycleOwner = this) { event ->
            when (event) {
                ToDataSynchronizationDialog -> {
                    navigateToDataSynchronizationDialog()
                }
                ToDeckCreationDialog -> navigateToDeckCreationDialog()
                is ToDeckNavigationDialog -> {
                    navigateToDeckNavigationDialog(deck = event.deck)
                }
                is ToDeckRepetitionScreen -> {
                    navigateToRepetitionFragment(deck = event.deck)
                }
                ToPrevious -> navController.popBackStack()
                is ToCardTransferringScreen -> {
                    navigateCardTransferringFragment(deckId = event.deckId)
                }
                is ToSigningTypeChoosingDialog -> {
                    navigateToSigningTypeChoosingDialog(fromDestination = event.fromSourceDestination)
                }
                is ToDrawerActionDialog -> {
                    navigateToDrawerActionDialog(action = event.action)
                }
                null -> {}
            }
        }
    }

    private fun navigateToDeckCreationDialog() {
        navController.navigate(R.id.action_deckListFragment_to_deckCreationDialogFragment)
    }

    private fun navigateToRepetitionFragment(deck: Deck) {
        DeckListFragmentDirections.actionDeckListFragmentToDeckRepetitionFragment(
            deckId = deck.id,
            deckName = deck.name
        ).also { navController.navigate(it) }
    }

    private fun navigateToDeckNavigationDialog(deck: Deck) {
        DeckListFragmentDirections.actionDeckListFragmentToDeckNavigationDialog(
            deckId = deck.id,
            deckName = deck.name
        ).also { navController.navigate(directions = it) }
    }

    private fun navigateToDataSynchronizationDialog() {
        navController.navigate(R.id.action_deckListFragment_to_dataSynchronizationDialogFragment)
    }

    private fun navigateCardTransferringFragment(deckId: Int) {
        DeckListFragmentDirections.actionDeckListFragmentToCardTransferringFragment(
            sourceDeckId = deckId
        ).also { navController.navigate(directions = it) }
    }

    private fun navigateToSigningTypeChoosingDialog(fromDestination: NavigationDestination) {
        val navDirection = when (fromDestination) {
            NavigationDestination.DECK_LIST_FRAGMENT -> {
                DeckListFragmentDirections.actionDeckListFragmentToSigningTypeChoosingDialogFragment(
                    fromSourceDestination = fromDestination
                )
            }
            NavigationDestination.DATA_SYNCHRONIZATION_DIALOG -> {
                DataSynchronizationDialogFragmentDirections
                    .actionDataSynchronizationDialogFragmentToSigningTypeChoosingDialogFragment(
                        fromSourceDestination = fromDestination
                    )
            }
        }
        navController.navigate(directions = navDirection)
    }

    private fun navigateToDrawerActionDialog(action: DrawerAction) {
        DeckListFragmentDirections.actionDeckListFragmentToDrawerActionDialogFragment(
            drawerAction = action,
        ).also { navController.navigate(directions = it) }
    }

    private fun restartApp() {
        viewModel.reopenApp()
        requireActivity().finish()
    }
}