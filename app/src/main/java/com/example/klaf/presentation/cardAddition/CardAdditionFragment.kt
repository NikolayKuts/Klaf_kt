package com.example.klaf.presentation.cardAddition

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.widget.doOnTextChanged
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.navArgs
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.klaf.R
import com.example.klaf.databinding.FragmentCardAdditionBinding
import com.example.klaf.domain.common.generateLetterInfos
import com.example.klaf.domain.ipa.LetterInfo
import com.example.klaf.domain.entities.Deck
import com.example.klaf.presentation.adapters.LetterBarAdapter
import com.example.klaf.presentation.common.collectWhenStarted
import com.example.klaf.presentation.common.showToast
import com.example.klaf.presentation.common.textAsString
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class CardAdditionFragment : Fragment() {

    private var _binding: FragmentCardAdditionBinding? = null
    private val binding get() = _binding!!

    private val args by navArgs<CardAdditionFragmentArgs>()

    @Inject
    lateinit var cardAdditionAssistedFactory: CardAdditionViewModelAssistedFactory
    private val viewModel: CardAdditionViewModel by viewModels {
        CardAdditionViewModelFactory(
            assistedFactory = cardAdditionAssistedFactory,
            deckId = args.deckId
        )
    }

    private val letterBarAdapter = LetterBarAdapter(onUpdate = ::setIpaText)

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        _binding = FragmentCardAdditionBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        initLetterBarRecyclerView()
        setObservers()
        setListeners()
    }

    override fun onDestroy() {
        super.onDestroy()
        binding.letterBarRecyclerView.adapter = null
        _binding = null
    }

    private fun setIpaText(uncompletedIpaCouples: String?) {
        binding.ipaEditText.setText(uncompletedIpaCouples)
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

    private fun setObservers() {
        setEventMessageObserver()
        setDeckObserver()
    }

    private fun setEventMessageObserver() {
        viewModel.eventMessage.collectWhenStarted(lifecycleScope) { eventMessage ->
            requireContext().showToast(messageId = eventMessage.resId)
        }
    }

    private fun setDeckObserver() {
        viewModel.deck.collectWhenStarted(lifecycleScope) { deck: Deck? ->
            deck?.let {
                binding.cardAdditionDeckName.text = deck.name
                binding.cardQuantityTextView.text = deck.cardQuantity.toString()

                clearEditTextCardFields()
            }
        }
    }

    private fun setListeners() {
        binding.applyCardAdditionButton.setOnClickListener { confirmCardAddition() }
        binding.foreignWordEditText.doOnTextChanged { text, _, _, _ ->
            setLetterBarAdapterData(text)
        }
    }

    private fun confirmCardAddition() {
        with(binding) {
            val nativeWord = nativeWordEditText.textAsString.trim()
            val foreignWord = foreignWordEditText.textAsString.trim()

            if (nativeWord.isEmpty() && foreignWord.isEmpty()) {
                requireContext().showToast(
                    messageId = R.string.native_and_foreign_words_must_be_filled
                )
            } else {
                viewModel.addNewCard(
                    deckId = args.deckId,
                    nativeWord = nativeWord,
                    foreignWord = foreignWord,
                    letterInfos = letterBarAdapter.letterInfos,
                    ipaTemplate = ipaEditText.textAsString
                )
            }
        }
    }

    private fun clearEditTextCardFields() {
        binding.apply {
            nativeWordEditText.setText("")
            foreignWordEditText.setText("")
            ipaEditText.setText("")
        }
    }

    private fun setLetterBarAdapterData(text: CharSequence?) {
        letterBarAdapter.setData(letters = text.generateLetterInfos())
    }
}