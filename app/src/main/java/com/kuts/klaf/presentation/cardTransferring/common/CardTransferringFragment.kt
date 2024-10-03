package com.kuts.klaf.presentation.cardTransferring.common

import android.os.Bundle
import android.view.View
import androidx.compose.material.Surface
import androidx.compose.ui.platform.ComposeView
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import androidx.navigation.navGraphViewModels
import com.kuts.klaf.R
import com.kuts.klaf.presentation.cardTransferring.common.CardTransferringNavigationEvent.ToCardAddingScreen
import com.kuts.klaf.presentation.cardTransferring.common.CardTransferringNavigationEvent.ToCardDeletingDialog
import com.kuts.klaf.presentation.cardTransferring.common.CardTransferringNavigationEvent.ToCardEditingScreen
import com.kuts.klaf.presentation.cardTransferring.common.CardTransferringNavigationEvent.ToCardMovingDialog
import com.kuts.klaf.presentation.cardTransferring.common.CardTransferringNavigationEvent.ToPrevious
import com.kuts.klaf.presentation.common.BaseFragment
import com.kuts.klaf.presentation.common.collectWhenStarted
import com.kuts.klaf.presentation.theme.MainTheme
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class CardTransferringFragment : BaseFragment(R.layout.common_compose_layout) {

    private val args by navArgs<CardTransferringFragmentArgs>()
    private val navController by lazy { findNavController() }

    @Inject
    lateinit var assistedFactory: CardTransferringViewModelAssistedFactory
    private val viewModel: BaseCardTransferringViewModel by navGraphViewModels(
        navGraphId = R.id.cardTransferringFragment,
        factoryProducer = ::provideViewModelFactory
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        observeNavigationChanges()
        subscribeAudioPlayerObserver()
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        observeEventMessage()

        view.findViewById<ComposeView>(R.id.compose_view).setContent {
            MainTheme {
                Surface {
                    CardTransferringScreen(viewModel = viewModel)
                }
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()

        lifecycle.removeObserver(viewModel.audioPlayer)
    }

    private fun provideViewModelFactory(): ViewModelProvider.Factory {
        return CardTransferringViewModuleFactory(
            sourceDeckId = args.sourceDeckId,
            assistedFactory = assistedFactory
        )
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

    private fun subscribeAudioPlayerObserver() {
        lifecycle.addObserver(viewModel.audioPlayer)
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