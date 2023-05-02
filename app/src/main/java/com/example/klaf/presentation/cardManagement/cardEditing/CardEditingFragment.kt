package com.example.klaf.presentation.cardManagement.cardEditing

import android.os.Bundle
import android.view.View
import androidx.compose.material.Surface
import androidx.compose.ui.platform.ComposeView
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
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
    private val viewModel: BaseCardEditingViewModel by viewModels {
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

    override fun onDestroy() {
        super.onDestroy()
        lifecycle.removeObserver(viewModel.audioPlayer)
    }

    private fun setObserves(view: View) {
        setEventMessageObserver(view = view)
        setCardEditingStateObserver()
        subscribeAudioPlayerObserver()
    }

    private fun setEventMessageObserver(view: View) {
        viewModel.eventMessage.collectWhenStarted(
            lifecycleOwner = viewLifecycleOwner
        ) { eventMessage ->
            view.showSnackBar(messageId = eventMessage.resId)
        }
    }

    private fun setCardEditingStateObserver() {
        viewModel.cardEditingState.collectWhenStarted(
            lifecycleOwner = viewLifecycleOwner
        ) { editingState ->
            when (editingState) {
                CardEditingState.NOT_CHANGED -> {}
                CardEditingState.CHANGED -> findNavController().popBackStack()
            }
        }
    }

    private fun subscribeAudioPlayerObserver() {
        lifecycle.addObserver(viewModel.audioPlayer)
    }
}