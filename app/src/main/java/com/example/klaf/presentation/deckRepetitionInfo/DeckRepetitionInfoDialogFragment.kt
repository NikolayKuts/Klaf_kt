package com.example.klaf.presentation.deckRepetitionInfo

import android.os.Bundle
import android.view.View
import androidx.compose.ui.platform.ComposeView
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import com.example.klaf.R
import com.example.klaf.presentation.common.*
import com.example.klaf.presentation.theme.MainTheme
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class DeckRepetitionInfoDialogFragment : TransparentDialogFragment(
    layoutId = R.layout.common_compose_layout
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

        setEventMessageObserver()

        view.findViewById<ComposeView>(R.id.compose_view).setContent {
            MainTheme {
                DeckRepetitionInfoView(
                    viewModel = viewModel,
                    deckName = args.deckName,
                    onCloseClick = ::closeDialog
                )
            }
        }
    }

    private fun setEventMessageObserver() {
        viewModel.eventMessage.collectWhenStarted(
            lifecycleOwner = viewLifecycleOwner
        ) { eventMessage ->
            sharedViewModel.notify(message = eventMessage)
            closeDialog()
        }
    }

    private fun closeDialog() {
        findNavController().popBackStack()
    }
}