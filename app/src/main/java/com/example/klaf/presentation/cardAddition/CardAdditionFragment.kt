package com.example.klaf.presentation.cardAddition

import android.os.Bundle
import android.view.*
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.material.Surface
import androidx.compose.material.Text
import androidx.compose.material.TextField
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.ComposeView
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.navArgs
import com.example.klaf.R
import com.example.klaf.databinding.FragmentCardAdditionBinding
import com.example.klaf.domain.common.generateLetterInfos
import com.example.klaf.domain.entities.Deck
import com.example.klaf.presentation.adapters.LetterBarAdapter
import com.example.klaf.presentation.common.collectWhenStarted
import com.example.klaf.presentation.common.showToast
import com.example.klaf.presentation.theme.MainTheme
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class CardAdditionFragment : Fragment(R.layout.fragment_card_addition) {

    private val args by navArgs<CardAdditionFragmentArgs>()

    @Inject
    lateinit var cardAdditionAssistedFactory: CardAdditionViewModelAssistedFactory
    private val viewModel: CardAdditionViewModel by viewModels {
        CardAdditionViewModelFactory(
            assistedFactory = cardAdditionAssistedFactory,
            deckId = args.deckId
        )
    }

//    private val letterBarAdapter = LetterBarAdapter(onUpdate = ::setIpaText)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

//        viewModel.cardAdditionState.collectWhenStarted(
//        lifecycleScope = viewLifecycleOwner.lifecycleScope
//        ) { cardAdditionState ->
//            when (cardAdditionState) {
//                CardAdditionState.NOT_ADDED -> {}
//                CardAdditionState.ADDED -> {
////                    viewModel.resetCardAdditionState()
//                }
//            }
//        }

        view.findViewById<ComposeView>(R.id.card_addition_view).setContent {

            MainTheme() {
                Surface() {
                    CardAdditionFragmentView(viewModel = viewModel)
                }
            }
        }

//        initLetterBarRecyclerView()
//        setObservers()
//        setListeners()
    }

//    override fun onDestroy() {
//        super.onDestroy()
//        binding.letterBarRecyclerView.adapter = null
//        _binding = null
//    }

    private fun setIpaText(uncompletedIpaCouples: String?) {
//        binding.ipaEditText.setText(uncompletedIpaCouples)
    }

    private fun initLetterBarRecyclerView() {
//        binding.letterBarRecyclerView.apply {
//            layoutManager = LinearLayoutManager(
//                requireContext(),
//                LinearLayoutManager.HORIZONTAL,
//                false
//            )
//
//            binding.letterBarRecyclerView.adapter = letterBarAdapter
//        }
    }

    private fun setObservers() {
        setEventMessageObserver()
//        setDeckObserver()
    }

    private fun setEventMessageObserver() {
        viewModel.eventMessage.collectWhenStarted(lifecycleScope) { eventMessage ->
            requireContext().showToast(messageId = eventMessage.resId)
        }
    }

    private fun setDeckObserver(observer: (deck: Deck) -> Unit) {
        viewModel.deck.collectWhenStarted(lifecycleScope) { deck: Deck? ->
            deck?.let {
                observer(it)
//                binding.cardAdditionDeckName.text = deck.name
//                binding.cardQuantityTextView.text = deck.cardQuantity.toString()

//                clearEditTextCardFields()
            }
        }
    }

    private fun setListeners() {
//        binding.applyCardAdditionButton.setOnClickListener { confirmCardAddition() }
//        binding.foreignWordEditText.doOnTextChanged { text, _, _, _ ->
//            setLetterBarAdapterData(text)
//        }
    }

    private fun confirmCardAddition() {
//        with(binding) {
//            viewModel.addNewCard(
//                deckId = args.deckId,
//                nativeWord = nativeWordEditText.textAsString.trim(),
//                foreignWord = foreignWordEditText.textAsString.trim(),
//                letterInfos = letterBarAdapter.letterInfos,
//                ipaTemplate = ipaEditText.textAsString
//            )
//        }
    }

    private fun clearEditTextCardFields() {
//        binding.apply {
//            nativeWordEditText.setText("")
//            foreignWordEditText.setText("")
//            ipaEditText.setText("")
//        }
    }

    private fun setLetterBarAdapterData(text: CharSequence?) {
//        letterBarAdapter.setData(letters = text.generateLetterInfos())
    }
}