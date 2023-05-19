package com.kuts.klaf.presentation.authentication

import android.os.Bundle
import android.view.View
import androidx.compose.material.Surface
import androidx.compose.ui.platform.ComposeView
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import com.kuts.domain.common.AuthenticationAction
import com.kuts.klaf.R
import com.kuts.klaf.presentation.common.BaseFragment
import com.kuts.klaf.presentation.common.collectWhenStarted
import com.kuts.klaf.presentation.theme.MainTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class AuthenticationFragment : BaseFragment(layoutId = R.layout.common_compose_layout) {

    private val args by navArgs<AuthenticationFragmentArgs>()

    private val viewModel: BaseAuthenticationViewModel by viewModels<AuthenticationViewModel>()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        observeEventMessage()

        view.findViewById<ComposeView>(R.id.compose_view).setContent {
            MainTheme {
                Surface {
                    AuthenticationScreen(
                        action = args.authenticationAction,
                        viewModel = viewModel,
                        onAuthenticationFinished = ::navigateToDataSynchronizationDialog,
                    )
                }
            }
        }
    }

    private fun observeEventMessage() {
        viewModel.eventMessage.collectWhenStarted(
            lifecycleOwner = viewLifecycleOwner,
            onEach = sharedViewModel::notify
        )
    }

    private fun navigateToDataSynchronizationDialog(authenticationAction: AuthenticationAction) {
        AuthenticationFragmentDirections
            .actionAuthenticationFragmentToDataSynchronizationDialogFragment(
                authenticationActionResult = AuthenticationActionResult(
                    action = authenticationAction,
                    isSuccessful = true,
                )
            ).also { findNavController().navigate(directions = it) }
    }
}