package com.example.klaf.presentation.deckRepetitionInfo

import android.os.Bundle
import android.view.View
import androidx.compose.ui.platform.ComposeView
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import com.example.klaf.R
import com.example.klaf.presentation.common.TransparentDialogFragment
import com.example.klaf.presentation.common.collectWhenStarted
import com.example.klaf.presentation.common.showToast
import com.example.klaf.presentation.theme.MainTheme
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class DeckRepetitionInfoDialogFragment : TransparentDialogFragment(
    R.layout.dialog_deck_repetition_info
) {

    private val args by navArgs<DeckRepetitionInfoDialogFragmentArgs>()

    @Inject
    lateinit var assistedFactory: DeckRepetitionInfoViewModelAssistedFactory
    private val viewModel by viewModels<DeckRepetitionInfoViewModel> {
        DeckRepetitionInfoViewModelFactory(
            assistedFactory = assistedFactory,
            deckId = args.deckId
        )
    }


    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        view.findViewById<ComposeView>(R.id.compose_view_repetition_info).setContent {
            MainTheme {
                DeckRepetitionInfoView(
                    viewModel = viewModel,
                    deckName = args.deckName,
                    onCloseClick = ::closeDialog
                )
            }
        }

        setEventMessageObserver()
    }

    private fun setEventMessageObserver() {
        viewModel.eventMessage.collectWhenStarted(
            lifecycleOwner = viewLifecycleOwner
        ) { eventMessage ->
            requireContext().showToast(messageId = eventMessage.resId)
        }
    }

    private fun closeDialog() {
        findNavController().popBackStack()
    }
}