package com.kuts.klaf.presentation.cardManagement.cardAddition

import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.view.View
import androidx.compose.material.Surface
import androidx.compose.ui.platform.ComposeView
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.navArgs
import com.kuts.domain.common.ifTrue
import com.kuts.klaf.R
import com.kuts.klaf.data.common.MIME_TYPE_TEXT_PLAIN
import com.kuts.klaf.presentation.common.BaseFragment
import com.kuts.klaf.presentation.common.collectWhenStarted
import com.kuts.klaf.presentation.theme.MainTheme
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class CardAdditionFragment : BaseFragment(layoutId = R.layout.common_compose_layout) {

    private val args by navArgs<CardAdditionFragmentArgs>()

    @Inject
    lateinit var cardAdditionAssistedFactory: CardAdditionViewModelAssistedFactory
    private val viewModel: BaseCardAdditionViewModel by viewModels {
        CardAdditionViewModelFactory(
            assistedFactory = cardAdditionAssistedFactory,
            deckId = args.deckId,
            smartSelectedWord = retrieveSmartSelectedWord()
        )
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        subscribeAudioPlayerObserver()
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        observeEventMessage()

        view.findViewById<ComposeView>(R.id.compose_view).setContent {
            MainTheme {
                Surface {
                    CardAdditionScreen(viewModel = viewModel)
                }
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()

        lifecycle.removeObserver(viewModel.audioPlayer)
    }

    private fun observeEventMessage() {
        viewModel.eventMessage.collectWhenStarted(
            lifecycleOwner = viewLifecycleOwner,
            onEach = sharedViewModel::notify
        )
    }

    private fun subscribeAudioPlayerObserver() {
        lifecycle.addObserver(viewModel.audioPlayer)
    }

    private fun retrieveSmartSelectedWord(): String? {
        return when {
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.M -> {
                activity?.intent?.run {
                    type?.startsWith(MIME_TYPE_TEXT_PLAIN)
                        ?.ifTrue { this.getCharSequenceExtra(Intent.EXTRA_PROCESS_TEXT).toString() }
                }
            }
            else -> null
        }
    }
}