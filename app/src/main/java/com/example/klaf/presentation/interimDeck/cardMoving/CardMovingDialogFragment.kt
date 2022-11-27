package com.example.klaf.presentation.interimDeck.cardMoving

import android.os.Bundle
import android.view.View
import androidx.compose.ui.platform.ComposeView
import androidx.navigation.fragment.findNavController
import androidx.navigation.navGraphViewModels
import com.example.klaf.R
import com.example.klaf.presentation.common.TransparentDialogFragment
import com.example.klaf.presentation.interimDeck.common.BaseInterimDeckViewModel
import com.example.klaf.presentation.theme.MainTheme

class CardMovingDialogFragment :
    TransparentDialogFragment(contentLayoutId = R.layout.dialog_interim_deck) {

    private val viewModel by navGraphViewModels<BaseInterimDeckViewModel>(R.id.interimDeckFragment)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        view.findViewById<ComposeView>(R.id.compose_view_dialog).setContent {
            MainTheme {
                CardMovingDialogView(viewModel = viewModel, onCloseClick = ::closeDialog)
            }
        }
    }

    private fun closeDialog() {
        findNavController().popBackStack()
    }
}