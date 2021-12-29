package com.example.klaf.presentation.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.widget.doOnTextChanged
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.navArgs
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.klaf.R
import com.example.klaf.databinding.FragmentCardAdditionBinding
import com.example.klaf.domain.ipa.IpaProcessor
import com.example.klaf.domain.ipa.LetterInfo
import com.example.klaf.domain.pojo.Card
import com.example.klaf.presentation.adapters.LetterBarAdapter
import com.example.klaf.presentation.view_model_factories.CardAdditionViewModelFactory
import com.example.klaf.presentation.view_models.CardAdditionViewModel

class CardAdditionFragment : Fragment() {

    private var _binding: FragmentCardAdditionBinding? = null
    private val binding get() = _binding!!

    private val args by navArgs<CardAdditionFragmentArgs>()
    private val viewModel: CardAdditionViewModel by lazy { getCardAdditionViewModel() }
    private val letterInfos: MutableList<LetterInfo> = ArrayList()
    private val letterBarAdapter: LetterBarAdapter by lazy {
        LetterBarAdapter { uncompletedIpaCouples ->
            binding.ipaEditText.setText(uncompletedIpaCouples)
        }
    }

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
        setDeckObserver()
        setListeners()
    }

    override fun onDestroy() {
        super.onDestroy()
        binding.letterBarRecyclerView.adapter = null
        _binding = null
    }

    private fun initLetterBarRecyclerView() {
        binding.letterBarRecyclerView.apply {
            layoutManager = LinearLayoutManager(
                requireActivity().applicationContext,
                LinearLayoutManager.HORIZONTAL,
                false
            )

            binding.letterBarRecyclerView.adapter = letterBarAdapter
        }
    }

    private fun setDeckObserver() {
        viewModel.deck.observe(viewLifecycleOwner) { deck ->
            deck?.let {
                binding.cardAdditionDeckName.text = deck.name
                binding.cardQuantityTextView.text = deck.cardQuantity.toString()
            }
        }
    }

    private fun setListeners() {
        binding.applyCardAdditionButton.setOnClickListener { onConfirmCardAddition() }

        binding.foreignWordEditText.doOnTextChanged { text, _, _, _ ->
            setLetterBarAdapterData(text)
        }
    }

    private fun onConfirmCardAddition() {
        when (val newCard = getCardForAddition()) {
            null -> showToast(message = getString(R.string.native_and_foreign_words_must_be_filled))
            else -> {
                viewModel.onAddNewCard(newCard)
                clearEditTextFields()
                showToast(message = getString(R.string.card_has_been_added))
            }
        }
    }

    private fun clearEditTextFields() {
        binding.apply {
            nativeWordEditText.setText("")
            foreignWordEditText.setText("")
            ipaEditText.setText("")
        }
    }

    private fun getCardForAddition(): Card? {
        return binding.run {
            val nativeWord = nativeWordEditText.text.toString().trim()
            val foreignWord = foreignWordEditText.text.toString().trim()

            when {
                nativeWord.isEmpty() && foreignWord.isEmpty() -> null
                else -> {
                    Card(
                        deckId = args.deckId,
                        nativeWord = nativeWord,
                        foreignWord = foreignWord,
                        ipa = IpaProcessor().getEncodedIpa(
                            letterInfos = letterInfos,
                            ipaTemplate = ipaEditText.text.toString()
                        )
                    )
                }
            }
        }
    }

    private fun showToast(message: String) {
        Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
    }

    private fun getCardAdditionViewModel(): CardAdditionViewModel {
        return ViewModelProvider(
            owner = this,
            factory = CardAdditionViewModelFactory(
                context = requireActivity().applicationContext,
                deckId = args.deckId
            )
        )[CardAdditionViewModel::class.java]
    }

    private fun setLetterBarAdapterData(text: CharSequence?) {
        letterBarAdapter.setData(getLetterInfoList(fromText = text))
    }

    private fun getLetterInfoList(fromText: CharSequence?): List<LetterInfo> {
        // TODO: 12/29/2021 there is duplicated implementation in CardAdditionFragment
        return when (fromText) {
            null -> emptyList()
            else -> {
                fromText.toString()
                    .trim()
                    .split("")
                    .drop(1)
                    .dropLast(1)
                    .map { letter -> LetterInfo(letter = letter, isChecked = false) }
            }
        }
    }
}