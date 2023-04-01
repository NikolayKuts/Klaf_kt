package com.example.klaf.presentation.authentication

import android.os.Bundle
import android.view.View
import androidx.compose.ui.platform.ComposeView
import androidx.navigation.fragment.findNavController
import com.example.klaf.R
import com.example.klaf.presentation.common.TransparentDialogFragment
import com.example.klaf.presentation.theme.MainTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class SigningTypeChoosingDialogFragment :
    TransparentDialogFragment(R.layout.common_compose_layout) {

    private val navController by lazy { findNavController() }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        view.findViewById<ComposeView>(R.id.compose_view).setContent {
            MainTheme {
                SigningTypeChoosingView(
                    onSignInButtonClick = {
                        SigningTypeChoosingDialogFragmentDirections
                            .actionSigningTypeChoosingDialogFragmentToAuthenticationFragment(
                            authenticationAction = AuthenticationAction.SIGN_IN
                        ).also { navController.navigate(directions = it) }
                    },
                    onSignUpButtonClick = {
                        SigningTypeChoosingDialogFragmentDirections
                            .actionSigningTypeChoosingDialogFragmentToAuthenticationFragment(
                            authenticationAction = AuthenticationAction.SIGN_UP
                        ).also { navController.navigate(directions = it) }
                    },
                    onCloseButtonClick = navController::popBackStack
                )
            }
        }
    }
}