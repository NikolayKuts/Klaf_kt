package com.example.presentation.deckList.deckCreation

import android.os.Bundle
import android.view.View
import androidx.compose.ui.platform.ComposeView
import androidx.navigation.fragment.findNavController
import androidx.navigation.navGraphViewModels
import com.example.presentation.R
import com.example.presentation.common.TransparentDialogFragment
import com.example.presentation.common.collectWhenStarted
import com.example.presentation.common.showSnackBar
import com.example.presentation.deckList.common.BaseDeckListViewModel
import com.example.presentation.theme.MainTheme

class DeckCreationDialogFragment : TransparentDialogFragment(R.layout.dialog_deck_creation) {

    private val viewModel by navGraphViewModels<BaseDeckListViewModel>(R.id.deckListFragment)

    private val navController by lazy { findNavController() }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setEvenMessageObserver(view = view)
        setDeckCreationStateObserver()

        view.findViewById<ComposeView>(R.id.dialog_deck_creation_view).setContent {
            MainTheme {
                DeckCreationDialog(
                    onConfirmCreationClick = ::confirmDeckCreation,
                    onCloseDialogClick = ::closeDialog
                )
            }
        }
    }

    private fun setEvenMessageObserver(view: View) {
        viewModel.eventMessage.collectWhenStarted(
            lifecycleOwner = viewLifecycleOwner
        ) { eventMessage ->
            view.showSnackBar(messageId = eventMessage.resId)
        }
    }

    private fun setDeckCreationStateObserver() {
        viewModel.deckCreationState.collectWhenStarted(
            lifecycleOwner = viewLifecycleOwner
        ) { deckCreationState ->
            when (deckCreationState) {
                DeckCreationState.NOT_CREATED -> {}
                DeckCreationState.CREATED -> closeDialog()
            }
        }
    }

    private fun confirmDeckCreation(deckName: String) {
        viewModel.createNewDeck(deckName = deckName)
    }

    private fun closeDialog() {
        viewModel.resetDeckCreationState()
        navController.popBackStack()
    }
}