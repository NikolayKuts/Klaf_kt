package com.kuts.klaf.cardViewing

import android.os.Bundle
import android.view.View
import androidx.compose.ui.platform.ComposeView
import androidx.navigation.fragment.navArgs
import com.kuts.klaf.presentation.R
import com.kuts.klaf.common.BaseFragment
import com.kuts.klaf.common.TransparentSurface
import com.kuts.klaf.common.collectWhenStarted
import com.kuts.klaf.theme.MainTheme
import org.koin.androidx.viewmodel.ext.android.viewModel
import org.koin.core.parameter.parametersOf

class CardViewingFragment : BaseFragment(layoutId = R.layout.common_compose_layout) {

    private val args by navArgs<CardViewingFragmentArgs>()

    private val viewModel: CardViewingViewModel by viewModel {
        parametersOf(args.deckId)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        observeEventMessage()

        view.findViewById<ComposeView>(R.id.compose_view).setContent {
            MainTheme {
                TransparentSurface { CardViewingScreen(viewModel = viewModel) }
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
