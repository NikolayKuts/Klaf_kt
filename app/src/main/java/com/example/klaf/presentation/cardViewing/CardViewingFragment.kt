package com.example.klaf.presentation.cardViewing

import android.os.Bundle
import android.view.View
import androidx.compose.material.Surface
import androidx.compose.ui.platform.ComposeView
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.navArgs
import com.example.klaf.R
import com.example.klaf.presentation.common.collectWhenStarted
import com.example.klaf.presentation.common.showSnackBar
import com.example.klaf.presentation.theme.MainTheme
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class CardViewingFragment : Fragment(R.layout.fragment_card_viewing) {

    private val args by navArgs<CardViewingFragmentArgs>()

    @Inject
    lateinit var assistedFactory: CardViewingViewModelFactory.CardViewingViewModelAssistedFactory
    private val viewModel: CardViewingViewModel by viewModels {
        CardViewingViewModelFactory(assistedFactory = assistedFactory, deckId = args.deckId)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        observeEventMessage(view = view)

        view.findViewById<ComposeView>(R.id.compose_view_card_viewing).setContent {
            MainTheme {
                Surface { CardViewingScreen(viewModel = viewModel) }
            }
        }
    }

    private fun observeEventMessage(view: View) {
        viewModel.eventMessage.collectWhenStarted(
            lifecycleOwner = viewLifecycleOwner
        ) { eventMessage ->
            view.showSnackBar(messageId = eventMessage.resId)
        }
    }
}