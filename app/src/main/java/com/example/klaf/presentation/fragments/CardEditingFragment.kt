package com.example.klaf.presentation.fragments

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
import com.example.klaf.databinding.FragmentCardEditingBinding
import com.example.klaf.domain.ipa.IpaProcessor
import com.example.klaf.domain.ipa.LetterInfo
import com.example.klaf.domain.pojo.Card
import com.example.klaf.domain.update
import com.example.klaf.presentation.adapters.LetterBarAdapter
import com.example.klaf.presentation.view_model_factories.CardEditingViewModelFactory
import com.example.klaf.presentation.view_models.CardEditingViewModel

class CardEditingFragment : Fragment() {

    private var _binding: FragmentCardEditingBinding? = null
    private val binding get() = _binding!!
    private val args by navArgs<CardEditingFragmentArgs>()
    private var viewModel: CardEditingViewModel? = null
    private var cardForChanging: Card? = null
    private var letterInfos: MutableList<LetterInfo> = ArrayList()

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
        activity?.let { activity ->
            with(binding) {
                viewModel = ViewModelProvider(
                    owner = activity,
                    factory = CardEditingViewModelFactory(
                        context = activity.applicationContext,
                        cardId = args.cardId
                    )
                )[CardEditingViewModel::class.java]

                viewModel?.onGetCardById(args.cardId) { card ->
                    cardForChanging = card
                    if (card != null) {
                        setContentToEditTexts(card)
                        letterInfos.update(IpaProcessor().getLetterInfos(card.ipa))
                    }
                }

                val adapter = LetterBarAdapter(letterInfos = letterInfos) { uncompletedIpa ->
                    ipaEditText.setText(uncompletedIpa)
                }

                letterBarRecyclerView.layoutManager = LinearLayoutManager(
                    activity.applicationContext,
                    LinearLayoutManager.HORIZONTAL,
                    false
                )

                letterBarRecyclerView.adapter = adapter

                foreignWordEditText.doOnTextChanged { text, _, _, _ ->
                    val foreignWord = text.toString().trim()

                    val letterInfos = when {
                        foreignWord.isNotEmpty() -> {
                            foreignWord.split("")
                                .drop(1)
                                .dropLast(1)
                                .map { letter -> LetterInfo(letter = letter, isChecked = false) }
                        }
                        else -> ArrayList()
                    }
                    adapter.setData(letterInfos)
                }

                applyCardChangesButton.setOnClickListener {
                    cardForChanging?.let {
                        val changedCard = getChangedCard()
                        when {
                            changedCard.nativeWord.isEmpty()
                                    || changedCard.foreignWord.isEmpty() -> {
                                showToast(
                                    "The \"native word\" and \"foreign word\" fields must be filled"
                                )
                            }
                            changedCard == cardForChanging -> {
                                showToast("The card hasn't been changed")
                            }
                            else -> {
                                viewModel?.insertChangedCard(changedCard)

                                CardEditingFragmentDirections
                                    .actionCardEditingFragmentToRepeatFragment(deckId = args.deckId)
                                    .also { findNavController().navigate(it) }
                            }
                        }
                    }
                }
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        _binding = null
    }

    private fun getChangedCard(): Card {
        with(binding) {
            return Card(
                deckId = args.deckId,
                nativeWord = nativeWordEditText.text.trim().toString(),
                foreignWord = foreignWordEditText.text.trim().toString(),
                ipa = IpaProcessor().getEncodedIpa(
                    letterInfos = letterInfos,
                    ipaTemplate = ipaEditText.text.trim().toString()
                ),
                id = args.cardId
            )
        }
    }

    private fun setContentToEditTexts(card: Card) {
        with(binding) {
            cardEditingDeckNameTextView.text = args.deckName
            nativeWordEditText.setText(card.nativeWord)
            foreignWordEditText.setText(card.foreignWord)
            ipaEditText.setText(IpaProcessor().getDecodedIpa(encodedIpa = card.ipa))
        }
    }

    private fun showToast(message: String) {
        Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
    }
}