package com.kuts.klaf.deckList.sygningTypeChoosing

import android.os.Bundle
import android.view.View
import androidx.compose.ui.platform.ComposeView
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import androidx.navigation.navGraphViewModels
import com.kuts.domain.common.AuthenticationAction
import com.kuts.klaf.presentation.R
import com.kuts.klaf.common.TransparentDialogFragment
import com.kuts.klaf.deckList.common.BaseDeckListViewModel
import com.kuts.klaf.deckList.common.IDeckListNavigationEvent
import com.kuts.klaf.theme.MainTheme
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
                    fromSourceDestination = args.fromSourceDestination,
                    onSigningActionButtonClick = ::navigateByAuthenticationAction,
                    onCloseButtonClick = {
                        viewModel.handleNavigation(event = IDeckListNavigationEvent.ToPrevious)
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