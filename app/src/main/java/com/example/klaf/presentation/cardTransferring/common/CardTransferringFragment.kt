package com.example.klaf.presentation.cardTransferring.common

import android.os.Bundle
import android.view.View
import androidx.compose.material.Surface
import androidx.compose.ui.platform.ComposeView
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import androidx.navigation.navGraphViewModels
import com.example.klaf.R
import com.example.klaf.presentation.cardTransferring.common.CardTransferringNavigationDestination.*
import com.example.klaf.presentation.cardTransferring.common.CardTransferringNavigationDestination.CardTransferringFragment
import com.example.klaf.presentation.common.collectWhenStarted
import com.example.klaf.presentation.common.showSnackBar
import com.example.klaf.presentation.theme.MainTheme
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class CardTransferringFragment : Fragment(R.layout.common_compose_layout) {

    private val args by navArgs<CardTransferringFragmentArgs>()
    private val navController by lazy { findNavController() }

    @Inject
    lateinit var assistedFactory: CardTransferringViewModelAssistedFactory
    private val viewModel: BaseCardTransferringViewModel by navGraphViewModels(
        navGraphId = R.id.cardTransferringFragment,
        factoryProducer = ::provideViewModelFactory
    )

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        observeNavigationChanges()
        observeEventMessage(view = view)

        view.findViewById<ComposeView>(R.id.compose_view).setContent {
            MainTheme {
                Surface {
                    CardTransferringScreen(viewModel = viewModel)
                }
            }
        }
    }

    private fun provideViewModelFactory(): ViewModelProvider.Factory {
        return CardTransferringViewModuleFactory(
            sourceDeckId = args.sourceDeckId,
            assistedFactory = assistedFactory
        )
    }

    private fun observeNavigationChanges() {
        viewModel.navigationDestination.collectWhenStarted(
            lifecycleOwner = viewLifecycleOwner
        ) { destination ->
            when (destination) {
                CardMovingDialogDestination -> navigateToCardMovingDialog()
                is CardAddingFragmentDestination -> {
                    navigateToCardAdditionDialog(interimDeckId = destination.sourceDeckId)
                }
                is CardDeletingDialogDestination -> {
                    navigateToCardDeletingDialog(cardQuantity = destination.cardQuantity)
                }
                CardTransferringFragment -> {
                    navController.popBackStack()
                }
                is CardEditingFragment -> {
                    navigateToCardEditingFragment(
                        cardId = destination.cardId,
                        deckId = destination.deckId
                    )
                }
            }
        }
    }

    private fun observeEventMessage(view: View) {
        viewModel.eventMessage.collectWhenStarted(lifecycleOwner = viewLifecycleOwner) { message ->
            view.showSnackBar(messageId = message.resId)
        }
    }

    private fun navigateToCardMovingDialog() {
        navController.navigate(R.id.action_interimDeckFragment_to_cardMovingDialogFragment)
    }

    private fun navigateToCardAdditionDialog(interimDeckId: Int) {
        CardTransferringFragmentDirections.actionInterimDeckFragmentToCardAdditionFragment(
            deckId = interimDeckId
        ).also { navController.navigate(directions = it) }
    }

    private fun navigateToCardDeletingDialog(cardQuantity: Int) {
        CardTransferringFragmentDirections.actionInterimDeckFragmentToCardDeletingDialogFragment(
            cardQuantity = cardQuantity
        ).also { navController.navigate(directions = it) }
    }

    private fun navigateToCardEditingFragment(cardId: Int, deckId: Int) {
        navController.navigate(
            directions =
            CardTransferringFragmentDirections.actionCardTransferringFragmentToCardEditingFragment(
                cardId = cardId,
                deckId = deckId,
            )
        )
    }
}