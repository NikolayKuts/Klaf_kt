package com.example.klaf.presentation.authentication

import android.os.Bundle
import android.view.View
import androidx.compose.material.Surface
import androidx.compose.ui.platform.ComposeView
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import com.example.klaf.R
import com.example.klaf.presentation.common.collectWhenStarted
import com.example.klaf.presentation.common.showSnackBar
import com.example.klaf.presentation.theme.MainTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class AuthenticationFragment : Fragment(R.layout.common_compose_layout) {

    private val viewModel: BaseAuthenticationViewModel by viewModels<AuthenticationViewModel>()

    private val args by navArgs<AuthenticationFragmentArgs>()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

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

        observeEventMessage(view = view)
    }

    private fun observeEventMessage(view: View) {
        viewModel.eventMessage.collectWhenStarted(lifecycleOwner = viewLifecycleOwner) { message ->
            view.showSnackBar(messageId = message.resId)
        }
    }

    private fun navigateToDataSynchronizationDialog() {
        findNavController().navigate(
            R.id.action_authenticationFragment_to_dataSynchronizationDialogFragment
        )
    }
}