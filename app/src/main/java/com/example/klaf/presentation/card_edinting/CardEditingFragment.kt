package com.example.klaf.presentation.card_edinting

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.widget.doOnTextChanged
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.klaf.R
import com.example.klaf.databinding.FragmentCardEditingBinding
import com.example.klaf.domain.ipa.IpaProcessor
import com.example.klaf.domain.ipa.LetterInfo
import com.example.klaf.domain.pojo.Card
import com.example.klaf.domain.pojo.Deck
import com.example.klaf.presentation.adapters.LetterBarAdapter

class CardEditingFragment : Fragment() {

    private var _binding: FragmentCardEditingBinding? = null
    private val binding get() = _binding!!

    private val args by navArgs<CardEditingFragmentArgs>()
    private val viewModel: CardEditingViewModel by lazy { getCardEditingViewModel() }
    private var cardForChanging: Card? = null

    private val letterBarAdapter = LetterBarAdapter(onItemClickListener = ::setIpaText)

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
        onGetDeck()
        onGetCardById()
        setListeners()
    }

    private fun onGetDeck() {
        viewModel.deck.observe(viewLifecycleOwner) { deck: Deck? ->
            binding.cardEditingDeckNameTextView.text = deck?.name
                ?: getString(R.string.deck_is_not_found)
        }
    }

    override fun onDestroy() {
        binding.letterBarRecyclerView.adapter = null
        _binding = null
        super.onDestroy()
    }

    private fun onGetCardById() {
        viewModel.onGetCardById(args.cardId) { card ->
            cardForChanging = card

            card?.let {
                setContentToEditTexts(card)
                letterBarAdapter.setData(letters = IpaProcessor().getLetterInfos(card.ipa))
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

    private fun getChangedCard(): Card = binding.run {
        Card(
            deckId = args.deckId,
            nativeWord = nativeWordEditText.text.trim().toString(),
            foreignWord = foreignWordEditText.text.trim().toString(),
            ipa = IpaProcessor().getEncodedIpa(
                letterInfos = letterBarAdapter.letterInfos,
                ipaTemplate = ipaEditText.text.trim().toString()
            ),
            id = args.cardId
        )
    }

    private fun setIpaText(uncompletedIpaCouples: String?) {
        binding.ipaEditText.setText(uncompletedIpaCouples)
    }

    private fun setContentToEditTexts(card: Card) {
        binding.apply {
            cardEditingDeckNameTextView.text = args.deckName
            nativeWordEditText.setText(card.nativeWord)
            foreignWordEditText.setText(card.foreignWord)
            ipaEditText.setText(IpaProcessor().getDecodedIpa(encodedIpa = card.ipa))
        }
    }

    private fun setListeners() {
        binding.foreignWordEditText.doOnTextChanged { text, _, _, _ ->
            setLetterBarAdapterData(text)
        }

        binding.applyCardChangesButton.setOnClickListener { onConfirmCardChange() }
    }

    private fun showToast(message: String) {
        Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
    }

    private fun getCardEditingViewModel(): CardEditingViewModel {
        return ViewModelProvider(
            owner = this,
            factory = CardEditingViewModelFactory(
                context = requireActivity().applicationContext,
                deckId = args.deckId,
                cardId = args.cardId
            )
        )[CardEditingViewModel::class.java]
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

    private fun onConfirmCardChange() {
        cardForChanging?.let {
            val changedCard = getChangedCard()

            when {
                changedCard.nativeWord.isEmpty() || changedCard.foreignWord.isEmpty() -> {
                    showToast(getString(R.string.native_and_foreign_words_must_be_filled))
                }
                changedCard == cardForChanging -> {
                    showToast(getString(R.string.card_has_not_been_changed))
                }
                else -> {
                    viewModel.insertChangedCard(changedCard)
                    findNavController().popBackStack()
                }
            }
        }
    }
}