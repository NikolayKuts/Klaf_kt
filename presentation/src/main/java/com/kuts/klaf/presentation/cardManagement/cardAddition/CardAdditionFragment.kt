package com.kuts.klaf.presentation.cardManagement.cardAddition

import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.compose.ui.platform.ComposeView
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.navArgs
import com.kuts.domain.common.ifTrue
import com.kuts.klaf.presentation.R
import com.kuts.klaf.presentation.cardManagement.common.BaseCardManagementViewModel
import com.kuts.klaf.presentation.common.BaseFragment
import com.kuts.klaf.presentation.common.TransparentSurface
import com.kuts.klaf.presentation.common.collectWhenStarted
import com.kuts.klaf.presentation.theme.MainTheme
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

private const val MIME_TYPE_TEXT_PLAIN = "text/plain"

@AndroidEntryPoint
class CardAdditionFragment : BaseFragment(layoutId = R.layout.common_compose_layout) {

    private val args by navArgs<CardAdditionFragmentArgs>()

    @Inject
    lateinit var cardAdditionAssistedFactory: ICardAdditionViewModelAssistedFactory
    private val viewModel: BaseCardManagementViewModel by viewModels {
        CardAdditionViewModelFactory(
            assistedFactory = cardAdditionAssistedFactory,
            deckId = args.deckId,
            smartSelectedWord = retrieveSmartSelectedWord()
        )
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

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
                    CardManagementScreen(viewModel = viewModel)
                }
            }
        }
    }

    override fun onDestroy() {
        viewModel.audioPlayer.onDestroy()
        super.onDestroy()
    }

    private fun observeEventMessage() {
        viewModel.eventMessage.collectWhenStarted(
            lifecycleOwner = viewLifecycleOwner,
            onEach = sharedViewModel::notify
        )
    }

    private fun retrieveSmartSelectedWord(): String? = activity?.intent?.run {
        type?.startsWith(MIME_TYPE_TEXT_PLAIN)
            ?.ifTrue { this.getCharSequenceExtra(Intent.EXTRA_PROCESS_TEXT).toString() }
    }
}
