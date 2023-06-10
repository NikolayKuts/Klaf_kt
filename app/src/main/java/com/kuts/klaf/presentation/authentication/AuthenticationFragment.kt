package com.kuts.klaf.presentation.authentication

import android.os.Bundle
import android.view.View
import androidx.compose.material.Surface
import androidx.compose.ui.platform.ComposeView
import androidx.core.os.bundleOf
import androidx.fragment.app.Fragment
import androidx.fragment.app.setFragmentResult
import androidx.fragment.app.setFragmentResultListener
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import com.kuts.domain.common.AuthenticationAction
import com.kuts.klaf.R
import com.kuts.klaf.presentation.common.BaseFragment
import com.kuts.klaf.presentation.common.NavigationDestination
import com.kuts.klaf.presentation.common.collectWhenStarted
import com.kuts.klaf.presentation.theme.MainTheme
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

@AndroidEntryPoint
class AuthenticationFragment : BaseFragment(layoutId = R.layout.common_compose_layout) {

    companion object {

        private const val REQUEST_KEY = "authentication_result_key"
        private const val RESULT_KEY = "success_mark_key"

        fun Fragment.setAuthenticationFragmentResultListener(
            block: (AuthenticationActionResult) -> Unit,
        ) {
            setFragmentResultListener(requestKey = REQUEST_KEY) { _, bundle ->
                bundle.getString(RESULT_KEY)?.let { encodedResult ->
                    Json.decodeFromString<AuthenticationActionResult>(string = encodedResult)
                }?.let { decodedResult -> block(decodedResult) }
            }
        }

        private fun buildResultBundle(result: AuthenticationActionResult): Bundle = bundleOf(
            RESULT_KEY to Json.encodeToString(value = result),
        )
    }

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
                        onAuthenticationFinished = ::navigateBySourceDestination,
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

    private fun navigateBySourceDestination(authenticationAction: AuthenticationAction) {
        when (args.fromSourceDestination) {
            NavigationDestination.DeckListFragment -> {
                val authenticationResult =
                    AuthenticationActionResult(action = authenticationAction, isSuccessful = true)

                setFragmentResult(
                    requestKey = REQUEST_KEY,
                    result = buildResultBundle(result = authenticationResult)
                )

                findNavController().popBackStack()
            }

            NavigationDestination.DataSynchronizationDialogFragment -> {
                AuthenticationFragmentDirections
                    .actionAuthenticationFragmentToDataSynchronizationDialogFragment(
                        authenticationActionResult = AuthenticationActionResult(
                            action = authenticationAction,
                            isSuccessful = true,
                        )
                    ).also { findNavController().navigate(directions = it) }
            }
        }
    }
}