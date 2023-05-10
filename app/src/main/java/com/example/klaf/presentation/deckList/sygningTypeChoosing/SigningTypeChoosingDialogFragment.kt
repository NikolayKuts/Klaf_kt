package com.example.klaf.presentation.deckList.sygningTypeChoosing

import android.os.Bundle
import android.view.View
import androidx.compose.ui.platform.ComposeView
import androidx.navigation.fragment.findNavController
import androidx.navigation.navGraphViewModels
import com.example.klaf.R
import com.example.klaf.presentation.authentication.AuthenticationAction
import com.example.klaf.presentation.common.TransparentDialogFragment
import com.example.klaf.presentation.deckList.common.BaseDeckListViewModel
import com.example.klaf.presentation.deckList.common.DeckListNavigationEvent
import com.example.klaf.presentation.theme.MainTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class SigningTypeChoosingDialogFragment : TransparentDialogFragment(
    layoutId = R.layout.common_compose_layout
) {

    private val navController by lazy { findNavController() }
    private val viewModel by navGraphViewModels<BaseDeckListViewModel>(R.id.deckListFragment)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        view.findViewById<ComposeView>(R.id.compose_view).setContent {
            MainTheme {
                SigningTypeChoosingView(
                    onSignInButtonClick = ::navigateToSigningInDialog,
                    onSignUpButtonClick = ::navigateToSigningUpDialog,
                    onCloseButtonClick = {
                        viewModel.handleNavigation(event = DeckListNavigationEvent.ToPrevious)
                    }
                )
            }
        }
    }

    private fun navigateToSigningInDialog() {
        SigningTypeChoosingDialogFragmentDirections
            .actionSigningTypeChoosingDialogFragmentToAuthenticationFragment(
                authenticationAction = AuthenticationAction.SIGN_IN
            ).also { navController.navigate(directions = it) }
    }

    private fun navigateToSigningUpDialog() {
        SigningTypeChoosingDialogFragmentDirections
            .actionSigningTypeChoosingDialogFragmentToAuthenticationFragment(
                authenticationAction = AuthenticationAction.SIGN_UP
            ).also { navController.navigate(directions = it) }
    }
}