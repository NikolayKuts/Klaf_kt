package com.kuts.klaf.presentation.deckList.dataSynchronization

import android.os.Bundle
import android.view.View
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.ComposeView
import androidx.navigation.fragment.navArgs
import androidx.navigation.navGraphViewModels
import com.kuts.klaf.R
import com.kuts.domain.common.AuthenticationAction
import com.kuts.klaf.presentation.common.EventMessage
import com.kuts.klaf.presentation.common.TransparentDialogFragment
import com.kuts.klaf.presentation.deckList.common.BaseDeckListViewModel
import com.kuts.klaf.presentation.deckList.common.DeckListNavigationEvent
import com.kuts.klaf.presentation.theme.MainTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class DataSynchronizationDialogFragment : TransparentDialogFragment(
    layoutId = R.layout.common_compose_layout
) {

    private val args by navArgs<DataSynchronizationDialogFragmentArgs>()
    private val viewModel by navGraphViewModels<BaseDeckListViewModel>(R.id.deckListFragment)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        view.findViewById<ComposeView>(R.id.compose_view).setContent {
            MainTheme {
                val eventMessage by sharedViewModel.eventMessage.collectAsState(initial = null)

                DataSynchronizationDialogView(
                    synchronizationState = viewModel.dataSynchronizationState.collectAsState().value,
                    onConfirmClick = viewModel::synchronizeData,
                    onCloseClick = {
                        viewModel.handleNavigation(event = DeckListNavigationEvent.ToPrevious)
                    },
                    onDispose = viewModel::resetSynchronizationState,
                    eventMassage = eventMessage,
                    onLaunched = ::notifyAboutAuthenticationActionResult
                )
            }
        }
    }

    private fun notifyAboutAuthenticationActionResult() {
        args.authenticationActionResult?.let { authenticationResult ->
            if (authenticationResult.isSuccessful) {
                val messageId = when (authenticationResult.action) {
                    AuthenticationAction.SIGN_IN -> R.string.authentication_sign_in_success
                    AuthenticationAction.SIGN_UP -> R.string.authentication_sign_up_success
                }

                sharedViewModel.notify(
                    message = EventMessage(resId = messageId, type = EventMessage.Type.Positive)
                )

                arguments?.clear()
            }
        }
    }
}