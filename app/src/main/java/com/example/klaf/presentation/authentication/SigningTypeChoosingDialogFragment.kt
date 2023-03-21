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
                        navController.navigate(R.id.action_signingTypeChoosingDialogFragment_to_authenticationFragment)
                    },
                    onSignUpButtonClick = {
                        navController.navigate(R.id.action_signingTypeChoosingDialogFragment_to_authenticationFragment)
                    },
                    onCloseButtonClick = {
                        navController.popBackStack()
                    }
                )
            }
        }
    }
}