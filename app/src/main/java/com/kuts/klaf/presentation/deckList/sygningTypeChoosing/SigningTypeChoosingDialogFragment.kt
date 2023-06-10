package com.kuts.klaf.presentation.deckList.sygningTypeChoosing

import android.os.Bundle
import android.view.View
import androidx.compose.ui.platform.ComposeView
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import androidx.navigation.navGraphViewModels
import com.kuts.domain.common.AuthenticationAction
import com.kuts.klaf.R
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
    private val args by navArgs<SigningTypeChoosingDialogFragmentArgs>()
    private val viewModel by navGraphViewModels<BaseDeckListViewModel>(R.id.deckListFragment)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        view.findViewById<ComposeView>(R.id.compose_view).setContent {
            MainTheme {
                SigningTypeChoosingView(
                    onSigningActionButtonClick = ::navigateByAuthenticationAction,
                    onCloseButtonClick = {
                        viewModel.handleNavigation(event = DeckListNavigationEvent.ToPrevious)
                    }
                )
            }
        }
    }

    private fun navigateByAuthenticationAction(action: AuthenticationAction) {
        SigningTypeChoosingDialogFragmentDirections
            .actionSigningTypeChoosingDialogFragmentToAuthenticationFragment(
                authenticationAction = action,
                fromSourceDestination = args.fromSourceDestination
            ).also { navController.navigate(directions = it) }
    }
}