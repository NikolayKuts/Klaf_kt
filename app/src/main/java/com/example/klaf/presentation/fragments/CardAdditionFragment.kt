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
    private var viewModel: CardAdditionViewModel? = null
    private val letterInfos: MutableList<LetterInfo> = ArrayList()

    // TODO: 10/10/2021 implement card passing as args but not only a card id

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
        activity?.let { activity ->
            with(binding) {

                letterBarRecyclerView.layoutManager = LinearLayoutManager(
                    activity.applicationContext,
                    LinearLayoutManager.HORIZONTAL,
                    false
                )

                val adapter = LetterBarAdapter(letterInfos = letterInfos) { uncompletedIpaCouples ->
                    ipaEditText.setText(uncompletedIpaCouples)
                }

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
                viewModel = ViewModelProvider(
                    owner = activity,
                    factory = CardAdditionViewModelFactory(
                        context = activity.applicationContext,
                        deckId = args.deckId
                    )
                )[CardAdditionViewModel::class.java]

                viewModel?.deck?.observe(viewLifecycleOwner) { deck ->
                    deck?.let {
                        cardAdditionDeckName.text = deck.name
                        cardQuantityTextView.text = deck.cardQuantity.toString()
                    }
                }

                applyCardAdditionButton.setOnClickListener { onConfirmCardAddition() }
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        _binding = null
    }

    private fun onConfirmCardAddition() {
        val newCard = getCardForAddition()
        when {
            newCard != null -> {
                viewModel?.let { viewModel ->
                    viewModel.onAddNewCard(newCard)
                    clearEditTextFields()
                    showToast(message = "the card has been added")
                }
            }
            else -> showToast(message = "native and foreign words must be filled")
        }
    }

    private fun clearEditTextFields() {
        with(binding) {
            nativeWordEditText.setText("")
            foreignWordEditText.setText("")
            ipaEditText.setText("")
        }
    }

    private fun getCardForAddition(): Card? {
        with(binding) {
            val nativeWord = nativeWordEditText.text.toString().trim()
            val foreignWord = foreignWordEditText.text.toString().trim()
            return if (nativeWord.isNotEmpty() && foreignWord.isNotEmpty()) {
                val ipaProcessor = IpaProcessor()
                Card(
                    deckId = args.deckId,
                    nativeWord = nativeWord,
                    foreignWord = foreignWord,
                    ipa = ipaProcessor.getEncodedIpa(
                        letterInfos = letterInfos,
                        ipaTemplate = ipaEditText.text.toString()
                    )
                )
            } else null
        }
    }

    private fun showToast(message: String) {
        Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
    }

}