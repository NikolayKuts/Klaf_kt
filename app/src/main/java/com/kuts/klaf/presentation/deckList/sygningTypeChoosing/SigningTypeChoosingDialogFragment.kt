package com.kuts.klaf.presentation.deckList.sygningTypeChoosing

import android.os.Bundle
import android.view.View
import androidx.compose.ui.platform.ComposeView
import androidx.navigation.fragment.findNavController
import androidx.navigation.navGraphViewModels
import com.kuts.klaf.R
import com.kuts.domain.common.AuthenticationAction
import com.kuts.klaf.presentation.common.TransparentDialogFragment
import com.kuts.klaf.presentation.deckList.common.BaseDeckListViewModel
import com.kuts.klaf.presentation.deckList.common.DeckListNavigationEvent
import com.kuts.klaf.presentation.theme.MainTheme
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