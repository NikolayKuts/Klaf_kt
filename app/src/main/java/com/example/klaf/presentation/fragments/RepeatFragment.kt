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
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.klaf.databinding.FragmentRepeatBinding
import com.example.klaf.domain.ipa.IpaProcessor
import com.example.klaf.domain.ipa.LetterInfo
import com.example.klaf.domain.pojo.Card
import com.example.klaf.domain.pojo.Deck
import com.example.klaf.domain.update
import com.example.klaf.presentation.adapters.IpaPromptAdapter
import com.example.klaf.presentation.view_model_factories.RepetitionViewModelFactory
import com.example.klaf.presentation.view_models.RepetitionViewModel

class RepeatFragment : Fragment() {

    private var _binding: FragmentRepeatBinding? = null
    private val binding get() = _binding!!
    private val args by navArgs<RepeatFragmentArgs>()
    private var viewModel: RepetitionViewModel? = null
    private val cards: MutableList<Card> = ArrayList()
    private var isFrontSide: Boolean = true
    private val ipaPrompts = ArrayList<LetterInfo>()
    private val adapter: IpaPromptAdapter by lazy { IpaPromptAdapter() }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        _binding = FragmentRepeatBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        activity?.let { activity ->
            initializeViewModel()

//            val adapter = IpaPromptAdapter()

            binding.ipaRecyclerView.layoutManager = LinearLayoutManager(
                activity.applicationContext,
                LinearLayoutManager.HORIZONTAL,
                false
            )

            binding.ipaRecyclerView.adapter = adapter

            viewModel?.cardSource?.observe(viewLifecycleOwner) { receivedCards ->
                cards.update(receivedCards)
                when {
                    cards.isNotEmpty() -> {
                        binding.cardSideTextView.text = cards[0].nativeWord
                        setIpaPromptAdapterContent()
                    }
                    else -> {
                        binding.cardSideTextView.text = "empty"
                    }
                }
            }
        }

        viewModel?.onGetDeck(args.deckId) { deck: Deck? ->
            if (deck != null) {
                binding.repeatDeckNameTextView.text = deck.name
            }
        }

        setOnClickListenerOnCArdAdditionButton()
        setOnClickListenerOnCardEditionButton()
        setOnClickListenerOnCardRemovingButton()
    }

    override fun onDestroy() {
        super.onDestroy()
        _binding = null
    }

    private fun initializeViewModel() {
        activity?.let { activity ->
            viewModel = ViewModelProvider(
                owner = activity,
                factory = RepetitionViewModelFactory(
                    context = activity.applicationContext,
                    deckId = args.deckId)
            )[RepetitionViewModel::class.java]
        }
    }

    private fun setOnClickListenerOnCArdAdditionButton() {
        binding.repeatCardAdditionButton.setOnClickListener {
            RepeatFragmentDirections.actionRepeatFragmentToCardAdditionFragment(
                deckId = args.deckId,
                deckName = args.deckName
            )
                .also { findNavController().navigate(it) }
        }
    }

    private fun setOnClickListenerOnCardEditionButton() {
        context?.let {
            binding.repeatCardEditingActionButton.setOnClickListener {
                if (cards.isNotEmpty()) {
                    RepeatFragmentDirections.actionRepeatFragmentToCardEditingFragment(
                        cardId = cards[0].id,
                        deckId = args.deckId,
                        deckName = args.deckName
                    )
                        .also { findNavController().navigate(it) }
                } else {
                    Toast.makeText(
                        context,
                        "There is nothing to change",
                        Toast.LENGTH_SHORT
                    )
                        .show()
                }
            }
        }
    }

    private fun setOnClickListenerOnCardRemovingButton() {
        binding.repeatCardRemovingActionButton.setOnClickListener {
            if (cards.isNotEmpty()) {
                RepeatFragmentDirections.actionRepeatFragmentToCardRemovingDialogFragment(
                    deckId = args.deckId,
                    cardId = cards[0].id
                )
                    .also { findNavController().navigate(it) }
            }
        }
    }

    private fun setIpaPromptAdapterContent() {
        if (cards.isNotEmpty()) {
            val card = cards[0]
            val ipaProcessor = IpaProcessor()

            when {
                (binding.repeatOrderSwitch.isChecked) -> {
                    when {
                        isFrontSide -> ipaPrompts.update(ipaProcessor.getIpaPrompts(card.ipa))
                        else -> ipaPrompts.clear()
                    }
                }
                else -> {
                    when {
                        isFrontSide -> ipaPrompts.clear()
                        else -> ipaPrompts.update(ipaProcessor.getIpaPrompts(card.ipa))
                    }
                }
            }
            adapter.setData(ipaPrompts)
        }
    }

}