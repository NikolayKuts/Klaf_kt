package com.example.klaf.presentation.authentication

import android.os.Bundle
import android.view.View
import androidx.compose.ui.platform.ComposeView
import com.example.klaf.R
import com.example.klaf.presentation.common.TransparentDialogFragment
import com.example.klaf.presentation.theme.MainTheme

class SigningTypeChoosingDialogFragment : TransparentDialogFragment(R.layout.common_compose_layout) {

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        view.findViewById<ComposeView>(R.id.compose_view).setContent {
            MainTheme {
                SigningTypeChoosingView()
            }
        }
    }
}