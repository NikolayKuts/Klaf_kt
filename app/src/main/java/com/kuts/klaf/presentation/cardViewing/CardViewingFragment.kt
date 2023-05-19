package com.kuts.klaf.presentation.cardViewing

import android.os.Bundle
import android.view.View
import androidx.compose.material.Surface
import androidx.compose.ui.platform.ComposeView
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.navArgs
import com.kuts.klaf.R
import com.kuts.klaf.presentation.common.BaseFragment
import com.kuts.klaf.presentation.common.collectWhenStarted
import com.kuts.klaf.presentation.theme.MainTheme
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class CardViewingFragment : BaseFragment(layoutId = R.layout.common_compose_layout) {

    private val args by navArgs<CardViewingFragmentArgs>()

    @Inject
    lateinit var assistedFactory: CardViewingViewModelFactory.CardViewingViewModelAssistedFactory
    private val viewModel: CardViewingViewModel by viewModels {
        CardViewingViewModelFactory(assistedFactory = assistedFactory, deckId = args.deckId)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        observeEventMessage()

        view.findViewById<ComposeView>(R.id.compose_view).setContent {
            MainTheme {
                Surface { CardViewingScreen(viewModel = viewModel) }
            }
        }
    }

    private fun observeEventMessage() {
        viewModel.eventMessage.collectWhenStarted(
            lifecycleOwner = viewLifecycleOwner,
            onEach = sharedViewModel::notify
        )
    }
}