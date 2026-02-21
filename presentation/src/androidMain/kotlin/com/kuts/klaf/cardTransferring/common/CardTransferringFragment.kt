package com.kuts.klaf.cardTransferring.common

import android.os.Bundle
import android.view.View
import androidx.compose.ui.platform.ComposeView
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import com.kuts.klaf.presentation.R
import com.kuts.klaf.cardTransferring.common.ICardTransferringNavigationEvent.ToCardAddingScreen
import com.kuts.klaf.cardTransferring.common.ICardTransferringNavigationEvent.ToCardDeletingDialog
import com.kuts.klaf.cardTransferring.common.ICardTransferringNavigationEvent.ToCardEditingScreen
import com.kuts.klaf.cardTransferring.common.ICardTransferringNavigationEvent.ToCardMovingDialog
import com.kuts.klaf.cardTransferring.common.ICardTransferringNavigationEvent.ToPrevious
import com.kuts.klaf.common.BaseFragment
import com.kuts.klaf.common.TransparentSurface
import com.kuts.klaf.common.collectWhenStarted
import com.kuts.klaf.theme.MainTheme
import org.koin.androidx.navigation.koinNavGraphViewModel
import org.koin.core.parameter.parametersOf

class CardTransferringFragment : BaseFragment(R.layout.common_compose_layout) {

    private val args by navArgs<CardTransferringFragmentArgs>()
    private val navController by lazy { findNavController() }

    private val viewModel: BaseCardTransferringViewModel by koinNavGraphViewModel(
        navGraphId = R.id.cardTransferringFragment,
    ) {
        parametersOf(args.sourceDeckId)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        observeNavigationChanges()
        viewModel.audioPlayer.onCreate()
    }

    override fun onResume() {
        super.onResume()
        viewModel.audioPlayer.onResume()
    }

    override fun onStop() {
        viewModel.audioPlayer.onStop()
        super.onStop()
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        observeEventMessage()

        view.findViewById<ComposeView>(R.id.compose_view).setContent {
            MainTheme {
                TransparentSurface {
                    CardTransferringScreen(viewModel = viewModel)
                }
            }
        }
    }

    override fun onDestroy() {
        viewModel.audioPlayer.onDestroy()
        super.onDestroy()
    }

    private fun observeNavigationChanges() {
        viewModel.navigationEvent.collectWhenStarted(lifecycleOwner = this) { event ->
            when (event) {
                is ToCardEditingScreen -> {
                    navigateToCardEditingScreen(cardId = event.cardId, deckId = event.deckId)
                }

                ToCardMovingDialog -> navigateToCardMovingDialog()
                is ToCardAddingScreen -> {
                    navigateToCardAdditionScreen(sourceDeckId = event.sourceDeckId)
                }

                is ToCardDeletingDialog -> {
                    navigateToCardDeletingDialog(cardQuantity = event.cardQuantity)
                }

                ToPrevious -> navController.popBackStack()
            }
        }
    }

    private fun observeEventMessage() {
        viewModel.eventMessage.collectWhenStarted(
            lifecycleOwner = viewLifecycleOwner,
            onEach = sharedViewModel::notify,
        )
    }

    private fun navigateToCardMovingDialog() {
        navController.navigate(R.id.action_interimDeckFragment_to_cardMovingDialogFragment)
    }

    private fun navigateToCardAdditionScreen(sourceDeckId: Int) {
        CardTransferringFragmentDirections.actionInterimDeckFragmentToCardAdditionFragment(
            deckId = sourceDeckId
        ).also { navController.navigate(directions = it) }
    }

    private fun navigateToCardDeletingDialog(cardQuantity: Int) {
        CardTransferringFragmentDirections.actionInterimDeckFragmentToCardDeletingDialogFragment(
            cardQuantity = cardQuantity
        ).also { navController.navigate(directions = it) }
    }

    private fun navigateToCardEditingScreen(cardId: Int, deckId: Int) {
        CardTransferringFragmentDirections.actionCardTransferringFragmentToCardEditingFragment(
            cardId = cardId,
            deckId = deckId,
        ).also { navController.navigate(directions = it) }
    }
}
