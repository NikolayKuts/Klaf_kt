package com.example.klaf.presentation.deckList.deckCreation

import android.os.Bundle
import android.view.View
import androidx.compose.material.Surface
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.ComposeView
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import androidx.navigation.navGraphViewModels
import com.example.klaf.R
import com.example.klaf.presentation.common.*
import com.example.klaf.presentation.deckList.common.BaseDeckListViewModel
import com.example.klaf.presentation.theme.MainTheme

class DeckCreationDialogFragment : TransparentDialogFragment(R.layout.common_compose_layout) {

    private val viewModel by navGraphViewModels<BaseDeckListViewModel>(R.id.deckListFragment)

    private val sharedViewModel: BaseMainViewModel by activityViewModels<MainViewModel>()

    private val navController by lazy { findNavController() }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setEvenMessageObserver(view = view)
        setDeckCreationStateObserver()


        view.findViewById<ComposeView>(R.id.compose_view).setContent {
            MainTheme {
                TransparentSurface {
                    val message by sharedViewModel.eventMessage.collectAsState(initial = null)

                    DeckCreationDialog(
                        onConfirmCreationClick = ::confirmDeckCreation,
                        onCloseDialogClick = ::closeDialog,
                        eventMassage = message
                    )
                }
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