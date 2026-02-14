package com.kuts.klaf.deckList.dataSynchronization

import android.os.Bundle
import android.view.View
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.ComposeView
import androidx.navigation.fragment.navArgs
import com.kuts.domain.common.AuthenticationAction
import com.kuts.klaf.presentation.R
import com.kuts.klaf.common.EventMessage
import com.kuts.klaf.common.TransparentDialogFragment
import com.kuts.klaf.common.TransparentSurface
import com.kuts.klaf.deckList.common.BaseDeckListViewModel
import com.kuts.klaf.deckList.common.IDeckListNavigationEvent
import com.kuts.klaf.theme.MainTheme
import org.koin.androidx.navigation.koinNavGraphViewModel

class DataSynchronizationDialogFragment : TransparentDialogFragment(
    layoutId = R.layout.common_compose_layout
) {

    private val args by navArgs<DataSynchronizationDialogFragmentArgs>()
    private val viewModel by koinNavGraphViewModel<BaseDeckListViewModel>(
        navGraphId = R.id.deckListFragment
    )

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        view.findViewById<ComposeView>(R.id.compose_view).setContent {
            MainTheme {
                TransparentSurface {
                    val eventMessage by sharedViewModel.eventMessage.collectAsState(initial = null)

                    DataSynchronizationDialogView(
                        synchronizationState = viewModel.dataSynchronizationState.collectAsState().value,
                        onConfirmClick = viewModel::synchronizeData,
                        onCloseClick = {
                            viewModel.handleNavigation(event = IDeckListNavigationEvent.ToPrevious)
                        },
                        onDispose = viewModel::resetSynchronizationState,
                        eventMassage = eventMessage,
                        onLaunched = ::notifyAboutAuthenticationActionResult
                    )
                }
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
