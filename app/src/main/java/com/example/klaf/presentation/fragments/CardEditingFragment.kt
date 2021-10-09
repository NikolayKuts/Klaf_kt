package com.example.klaf.presentation.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import com.example.klaf.R
import com.example.klaf.databinding.FragmentCardEditingBinding
import com.example.klaf.domain.pojo.Card
import com.example.klaf.presentation.view_model_factories.CardEditingViewModelFactory
import com.example.klaf.presentation.view_models.CardEditingViewModel

class CardEditingFragment : Fragment() {

    private var _binding: FragmentCardEditingBinding? = null
    private val binding get() = _binding!!
    private val args by navArgs<CardEditingFragmentArgs>()
    private var viewModel: CardEditingViewModel? = null
    private var cardForChanging: Card? = null

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
            viewModel = ViewModelProvider(
                owner = activity,
                factory = CardEditingViewModelFactory(
                    context = activity.applicationContext,
                    cardId = args.cardId
                )
            )[CardEditingViewModel::class.java]

            viewModel?.onGetCardById(args.cardId) { card ->
                cardForChanging = card
                setContentToEditTexts(card)
            }

            binding.applyCardChangesButton.setOnClickListener {
                cardForChanging?.let {
                    val changedCard = getChangedCard()
                    when {
                        changedCard.nativeWord.isEmpty() || changedCard.foreignWord.isEmpty() -> {
                            showToast(
                                "The \"native word\" and \"foreign word\" fields must be filled"
                            )
                        }
                        changedCard == cardForChanging -> {
                            showToast("The card hasn't been changed")
                        }
                        else -> {
                            viewModel?.insertChangedCard(changedCard)

                            findNavController().navigate(
                                R.id.action_cardEditingFragment_to_repeatFragment
                            )
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
                ipa = ipaEditText.text.trim().toString(),
                id = args.cardId
            )
        }
    }

    private fun setContentToEditTexts(card: Card) {
        with(binding) {
            cardEditingDeckNameTextView.text = args.deckName
            nativeWordEditText.setText(card.nativeWord)
            foreignWordEditText.setText(card.foreignWord)
            ipaEditText.setText(card.ipa)
        }
    }

    private fun showToast(message: String) {
        Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
    }
}