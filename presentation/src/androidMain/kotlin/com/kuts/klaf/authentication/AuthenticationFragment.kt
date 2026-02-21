package com.kuts.klaf.authentication

import android.os.Bundle
import android.view.View
import androidx.compose.ui.platform.ComposeView
import androidx.core.os.bundleOf
import androidx.fragment.app.Fragment
import androidx.fragment.app.setFragmentResult
import androidx.fragment.app.setFragmentResultListener
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import com.kuts.domain.common.AuthenticationAction
import com.kuts.klaf.presentation.R
import com.kuts.klaf.common.BaseFragment
import com.kuts.klaf.common.NavigationDestination
import com.kuts.klaf.common.TransparentSurface
import com.kuts.klaf.common.collectWhenStarted
import com.kuts.klaf.theme.MainTheme
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.koin.androidx.viewmodel.ext.android.viewModel

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

    private val viewModel: BaseAuthenticationViewModel by viewModel<AuthenticationViewModel>()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        observeEventMessage()

        view.findViewById<ComposeView>(R.id.compose_view).setContent {
            MainTheme {
                TransparentSurface {
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
            NavigationDestination.DECK_LIST_FRAGMENT -> {
                val authenticationResult =
                    AuthenticationActionResult(action = authenticationAction, isSuccessful = true)

                setFragmentResult(
                    requestKey = REQUEST_KEY,
                    result = buildResultBundle(result = authenticationResult)
                )

                findNavController().popBackStack()
            }

            NavigationDestination.DATA_SYNCHRONIZATION_DIALOG -> {
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
