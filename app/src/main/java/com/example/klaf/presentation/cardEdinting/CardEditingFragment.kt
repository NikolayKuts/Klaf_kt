package com.example.klaf.presentation.cardEdinting

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.widget.doOnTextChanged
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.klaf.R
import com.example.klaf.databinding.FragmentCardEditingBinding
import com.example.klaf.domain.common.generateLetterInfos
import com.example.klaf.domain.entities.Card
import com.example.klaf.domain.entities.Deck
import com.example.klaf.domain.ipa.IpaProcessor
import com.example.klaf.presentation.adapters.LetterBarAdapter
import com.example.klaf.presentation.common.collectWhenStarted
import com.example.klaf.presentation.common.showToast
import com.example.klaf.presentation.common.textAsString
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class CardEditingFragment : Fragment() {

    private var _binding: FragmentCardEditingBinding? = null
    private val binding get() = _binding!!

    private val args by navArgs<CardEditingFragmentArgs>()

    @Inject
    lateinit var cardEditingAssistedViewModelFactory: CardEditingAssistedViewModelFactory
    private val viewModel: CardEditingViewModel by viewModels {
        CardEditingViewModelFactory(
            assistedFactory = cardEditingAssistedViewModelFactory,
            deckId = args.deckId,
            cardId = args.cardId
        )
    }

    private val letterBarAdapter = LetterBarAdapter(onUpdate = ::setIpaText)

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        _binding = FragmentCardEditingBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        initLetterBarRecyclerView()
        setObserves()
        setListeners()
    }

    override fun onDestroy() {
        binding.letterBarRecyclerView.adapter = null
        _binding = null
        super.onDestroy()
    }

    private fun setObserves() {
        setEventMessageObserver()
        setDeckObserver()
        setCardObserver()
    }

    private fun setEventMessageObserver() {
        viewModel.eventMessage.collectWhenStarted(lifecycleScope) { eventMessage ->
            requireContext().showToast(messageId = eventMessage.resId)
        }
    }

    private fun setDeckObserver() {
        viewModel.deck.collectWhenStarted(lifecycleScope) { deck: Deck? ->
            binding.cardEditingDeckNameTextView.text = deck?.name
                ?: getString(R.string.deck_is_not_found)
        }
    }

    private fun setCardObserver() {
        viewModel.card.collectWhenStarted(lifecycleScope) { card: Card? ->
            if (card == null) {
                requireContext().showToast(messageId = R.string.card_is_not_found)
            } else {
                setContentToEditTexts(card)
                letterBarAdapter.setData(letters = IpaProcessor.getLetterInfos(card.ipa))
            }
        }
    }

    private fun initLetterBarRecyclerView() {
        binding.letterBarRecyclerView.apply {
            layoutManager = LinearLayoutManager(
                requireContext(),
                LinearLayoutManager.HORIZONTAL,
                false
            )

            binding.letterBarRecyclerView.adapter = letterBarAdapter
        }
    }

    private fun setIpaText(uncompletedIpaCouples: String?) {
        binding.ipaEditText.setText(uncompletedIpaCouples)
    }

    private fun setContentToEditTexts(card: Card) {
        binding.apply {
            cardEditingDeckNameTextView.text = args.deckName
            nativeWordEditText.setText(card.nativeWord)
            foreignWordEditText.setText(card.foreignWord)
            ipaEditText.setText(IpaProcessor.getDecodedIpa(encodedIpa = card.ipa))
        }
    }

    private fun setListeners() {
        binding.applyCardChangesButton.setOnClickListener { confirmCardChange() }
        binding.foreignWordEditText.doOnTextChanged { text, _, _, _ ->
            setLetterBarAdapterData(text)
        }
    }

    private fun confirmCardChange() {
        with(binding) {
            viewModel.updateCard(
                deckId = args.deckId,
                nativeWord = nativeWordEditText.textAsString.trim(),
                foreignWord = foreignWordEditText.textAsString.trim(),
                letterInfos = letterBarAdapter.letterInfos,
                ipaTemplate = ipaEditText.textAsString.trim(),
                id = args.cardId,
                onFinish = { findNavController().popBackStack() }
            )
        }
    }

    private fun setLetterBarAdapterData(text: CharSequence?) {
        letterBarAdapter.setData(text.generateLetterInfos())
    }
}