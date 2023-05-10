package com.example.klaf.presentation.cardTransferring.cardDeleting

import android.os.Bundle
import android.view.View
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.ComposeView
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import androidx.navigation.navGraphViewModels
import com.example.domain.common.LoadingState
import com.example.klaf.R
import com.example.klaf.presentation.cardTransferring.common.BaseCardTransferringViewModel
import com.example.klaf.presentation.common.*
import com.example.klaf.presentation.theme.MainTheme

class CardDeletingDialogFragment : TransparentDialogFragment(
    layoutId = R.layout.common_compose_layout,
) {

    private val sharedViewModel: BaseMainViewModel by activityViewModels<MainViewModel>()
    private val viewModel by navGraphViewModels<BaseCardTransferringViewModel>(
        navGraphId = R.id.cardTransferringFragment
    )
    private val args by navArgs<CardDeletingDialogFragmentArgs>()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)


        view.findViewById<ComposeView>(R.id.compose_view).setContent {
            MainTheme {
                TransparentSurface {
                    val eventMessage by sharedViewModel.eventMessage.collectAsState(initial = null)

                    CardDeletingDialogView(
                        cardQuantity = args.cardQuantity,
                        onConfirmDeleting = ::deleteCards,
                        onCancel = ::closeDialog,
                        eventMessage = eventMessage
                    )
                }
            }
        }

        observeCardDeletingState()
    }

    private fun observeCardDeletingState() {
        viewModel.cardsDeletingState.collectWhenStarted(
            lifecycleOwner = viewLifecycleOwner
        ) { deletingState ->
            when (deletingState) {
                is LoadingState.Success -> closeDialog()
                else -> {}
            }
        }
    }

    private fun deleteCards() {
        viewModel.deleteCards()
    }

    private fun closeDialog() {
        findNavController().popBackStack()
    }
}