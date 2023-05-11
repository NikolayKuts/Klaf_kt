package com.example.klaf.presentation.authentication

import android.os.Bundle
import android.view.View
import androidx.compose.material.Surface
import androidx.compose.ui.platform.ComposeView
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import com.example.klaf.R
import com.example.klaf.presentation.common.BaseFragment
import com.example.klaf.presentation.common.collectWhenStarted
import com.example.klaf.presentation.theme.MainTheme
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
        viewModel.eventMessage.collectWhenStarted(lifecycleOwner = viewLifecycleOwner) { message ->
            sharedViewModel.notify(message = message)
        }
    }

    private fun navigateToDataSynchronizationDialog() {
        findNavController().navigate(
            R.id.action_authenticationFragment_to_dataSynchronizationDialogFragment
        )
    }
}