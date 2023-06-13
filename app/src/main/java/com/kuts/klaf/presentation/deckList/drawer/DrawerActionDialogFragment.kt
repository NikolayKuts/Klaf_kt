package com.kuts.klaf.presentation.deckList.drawer

import android.os.Bundle
import android.view.View
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.platform.ComposeView
import androidx.navigation.fragment.navArgs
import androidx.navigation.navGraphViewModels
import com.kuts.klaf.R
import com.kuts.klaf.presentation.common.TransparentDialogFragment
import com.kuts.klaf.presentation.deckList.common.BaseDeckListViewModel
import com.kuts.klaf.presentation.deckList.common.DeckListNavigationEvent
import com.kuts.klaf.presentation.theme.MainTheme

class DrawerActionDialogFragment : TransparentDialogFragment(
    layoutId = R.layout.common_compose_layout
) {

    private val args by navArgs<DrawerActionDialogFragmentArgs>()
    private val viewModel by navGraphViewModels<BaseDeckListViewModel>(navGraphId = R.id.deckListFragment)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        view.findViewById<ComposeView>(R.id.compose_view).setContent {
            MainTheme {
                val eventMessageState = sharedViewModel.eventMessage.collectAsState(initial = null)

                DrawerActionView(
                    action = args.drawerAction,
                    loadingState  = viewModel.drawerActionLoadingState.collectAsState().value,
                    onCloseDialog = ::closeDialog,
                    onConfirmationClick = ::handleActionConfirmation,
                    eventMessage = eventMessageState.value,
                )
            }
        }
    }

    private fun closeDialog() {
        viewModel.handleNavigation(event = DeckListNavigationEvent.ToPrevious)
    }

    private fun handleActionConfirmation() {
        when (args.drawerAction) {
            DrawerAction.LOG_OUT -> viewModel.logOut()
            DrawerAction.DELETE_ACCOUNT -> viewModel.deleteAccount()
        }
    }
}