package com.example.klaf.presentation.cardEditing

import android.os.Bundle
import android.view.View
import androidx.compose.material.Surface
import androidx.compose.ui.platform.ComposeView
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import com.example.klaf.R
import com.example.klaf.presentation.common.collectWhenStarted
import com.example.klaf.presentation.common.showSnackBar
import com.example.klaf.presentation.theme.MainTheme
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class CardEditingFragment : Fragment(R.layout.fragment_card_editing) {

    private val args by navArgs<CardEditingFragmentArgs>()

    @Inject
    lateinit var cardEditingAssistedViewModelFactory: CardEditingAssistedViewModelFactory
    private val viewModel: CardEditingViewModel by viewModels {
        CardEditingViewModelFactory(
            assistedFactory = cardEditingAssistedViewModelFactory,
            deckId = args.deckId,
            cardId = args.cardId
        )
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setObserves(view = view)

        view.findViewById<ComposeView>(R.id.compose_view_card_editing).setContent {
            MainTheme {
                Surface {
                    CardEditingScreen(viewModel = viewModel)
                }
            }
        }
    }

    private fun setObserves(view: View) {
        setEventMessageObserver(view = view)
        setCardEditingStateObserver()
    }

    private fun setEventMessageObserver(view: View) {
        viewModel.eventMessage.collectWhenStarted(
            viewLifecycleOwner.lifecycleScope
        ) { eventMessage ->
            view.showSnackBar(messageId = eventMessage.resId)
        }
    }

    private fun setCardEditingStateObserver() {
        viewModel.cardEditingState.collectWhenStarted(
            viewLifecycleOwner.lifecycleScope
        ) { editingState ->
            when (editingState) {
                CardEditingState.NOT_CHANGED -> {}
                CardEditingState.CHANGED -> findNavController().popBackStack()
            }
        }
    }
}