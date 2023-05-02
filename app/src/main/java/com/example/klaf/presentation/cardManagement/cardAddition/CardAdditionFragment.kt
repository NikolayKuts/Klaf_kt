package com.example.klaf.presentation.cardManagement.cardAddition

import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.view.View
import androidx.compose.material.Surface
import androidx.compose.ui.platform.ComposeView
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.navArgs
import com.example.domain.common.ifTrue
import com.example.klaf.R
import com.example.klaf.data.common.MIME_TYPE_TEXT_PLAIN
import com.example.klaf.presentation.common.collectWhenStarted
import com.example.klaf.presentation.common.showSnackBar
import com.example.klaf.presentation.theme.MainTheme
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class CardAdditionFragment : Fragment(R.layout.fragment_card_addition) {

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

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setEventMessageObserver(view = view)

        view.findViewById<ComposeView>(R.id.compose_view_card_addition).setContent {
            MainTheme {
                Surface {
                    CardAdditionScreen(viewModel = viewModel)
                }
            }
        }

        subscribeAudioPlayerObserver()
    }

    override fun onDestroy() {
        super.onDestroy()
        lifecycle.removeObserver(viewModel.audioPlayer)
    }

    private fun setEventMessageObserver(view: View) {
        viewModel.eventMessage.collectWhenStarted(lifecycleOwner = this) { eventMessage ->
            view.showSnackBar(messageId = eventMessage.resId)
        }
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