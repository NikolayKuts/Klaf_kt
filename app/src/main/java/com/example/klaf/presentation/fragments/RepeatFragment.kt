package com.example.klaf.presentation.fragments

import android.content.Context
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.klaf.R
import com.example.klaf.databinding.FragmentRepeatBinding
import com.example.klaf.domain.ipa.IpaProcessor
import com.example.klaf.domain.ipa.LetterInfo
import com.example.klaf.domain.pojo.Card
import com.example.klaf.domain.pojo.Deck
import com.example.klaf.domain.update
import com.example.klaf.presentation.adapters.IpaPromptAdapter
import com.example.klaf.presentation.auxiliary.RepeatTimer
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
    private var timer: RepeatTimer? = null

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
        Log.i("klaf_log", "onViewCreated: ")
        activity?.let { activity ->
            with(binding) {

                initializeViewModel()

                timer = RepeatTimer(binding.repeatTimerTextView, activity.applicationContext)

                ipaRecyclerView.layoutManager = LinearLayoutManager(
                    activity.applicationContext,
                    LinearLayoutManager.HORIZONTAL,
                    false
                )

                ipaRecyclerView.adapter = adapter

                viewModel?.cardSource?.observe(viewLifecycleOwner) { receivedCards ->
                    cards.update(receivedCards)
                    setCardViewContent()
                    setCardContentColor()
                    setIpaPromptContent()
                }

                viewModel?.onGetDeck(args.deckId) { deck: Deck? ->
                    if (deck != null) {
                        repeatDeckNameTextView.text = deck.name
                    }
                }

                setButtonVisibilities(false)

                repeatCardAdditionButton.setOnClickListener { navigateToAdditionFragment() }
                repeatCardEditingActionButton.setOnClickListener { onClickCardEditingButton() }
                repeatCardRemovingActionButton.setOnClickListener { onClickCardRemovingButton() }
                startRepetitionButton.setOnClickListener { onClickStartButton() }
                turnButton.setOnClickListener { onClickTurnButton() }
                repeatOrderSwitch.setOnCheckedChangeListener { _, isChecked ->
                    onRepeatOrderSwitchCheckedChanged(isChecked)
                }
                timer?.let { lifecycle.addObserver(it) }


            }
        }
    }

    override fun onStart() {
        super.onStart()
        Log.i("klaf_log", "onStart: ")
    }

    override fun onResume() {
        super.onResume()
//        if (timer.isPaused) {
//            timer.runCounting()
//        }
        Log.i("klaf_log", "onResume: ")
    }

    override fun onPause() {
        super.onPause()
//        if (timer.isRunning) {
//            timer.pauseCounting()
//        }
        Log.i("klaf_log", "onPause: ")
    }

    override fun onStop() {
        super.onStop()
        Log.i("klaf_log", "onStop: ")
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        Log.i("klaf_log", "onSaveInstanceState: ")
    }


    override fun onDestroy() {
        super.onDestroy()
        _binding = null
        Log.i("klaf_log", "onDestroy: ")
    }

    private fun initializeViewModel() {
        activity?.let { activity ->
            viewModel = ViewModelProvider(
                owner = this,
                factory = RepetitionViewModelFactory(
                    context = activity.applicationContext,
                    deckId = args.deckId)
            )[RepetitionViewModel::class.java]
        }
    }

    private fun navigateToAdditionFragment() {
        RepeatFragmentDirections.actionRepeatFragmentToCardAdditionFragment(
            deckId = args.deckId
        )
            .also { findNavController().navigate(it) }
    }

    private fun onClickCardEditingButton() {
        context?.let {
            if (cards.isNotEmpty()) {
                RepeatFragmentDirections.actionRepeatFragmentToCardEditingFragment(
                    cardId = cards[0].id,
                    deckId = args.deckId
                )
                    .also { findNavController().navigate(it) }
            } else {
                Toast.makeText(context, "There is nothing to change", Toast.LENGTH_SHORT)
                    .show()
            }
        }
    }

    private fun onClickCardRemovingButton() {
        if (cards.isNotEmpty()) {
            RepeatFragmentDirections.actionRepeatFragmentToCardRemovingDialogFragment(
                deckId = args.deckId,
                cardId = cards[0].id
            )
                .also { findNavController().navigate(it) }
        }
    }

    private fun setIpaPromptContent() {
        if (cards.isNotEmpty()) {
            val card = cards[0]
            val ipaProcessor = IpaProcessor()

            if (binding.repeatOrderSwitch.isChecked) {
                when (isFrontSide) {
                    true -> ipaPrompts.update(ipaProcessor.getIpaPrompts(card.ipa))
                    else -> ipaPrompts.clear()
                }
            } else {
                when (isFrontSide) {
                    true -> ipaPrompts.clear()
                    else -> ipaPrompts.update(ipaProcessor.getIpaPrompts(card.ipa))
                }
            }
        }
        adapter.setData(ipaPrompts)
    }

    private fun onClickStartButton() {
        if (cards.isNotEmpty()) {
            setButtonVisibilities(true)
            timer?.runCounting()
        }
    }

    private fun onClickTurnButton() {
        isFrontSide = !isFrontSide
        setCardViewContent()
        setCardContentColor()
        setIpaPromptContent()
    }

    private fun setCardContentColor() {
        if (cards.isNotEmpty()) {
            setCardContentColorBySide()
        } else {
            binding.cardSideTextView.setTextColor(
                ContextCompat.getColor(
                    binding.cardSideTextView.context,
                    R.color.empty_card_content_color
                )
            )
        }
    }

    private fun setCardContentColorBySide() {
        with(binding) {
            if (isFrontSide) {
                cardSideTextView.setTextColor(
                    ContextCompat.getColor(
                        cardSideTextView.context,
                        R.color.front_card_content_color
                    )
                )
            } else {
                cardSideTextView.setTextColor(
                    ContextCompat.getColor(
                        cardSideTextView.context,
                        R.color.back_card_content_color
                    )
                )
            }
        }
    }

    private fun setCardViewContent() {
        with(binding) {
            if (cards.isEmpty()) {
                cardSideTextView.text = "The deck is empty"
            } else {
                val card = cards[0]
                if (repeatOrderSwitch.isChecked) {
                    when (isFrontSide) {
                        true -> cardSideTextView.text = card.foreignWord
                        else -> cardSideTextView.text = card.nativeWord
                    }
                } else {
                    when (isFrontSide) {
                        true -> cardSideTextView.text = card.nativeWord
                        else -> cardSideTextView.text = card.foreignWord
                    }
                }
            }
        }
    }

    private fun setButtonVisibilities(visible: Boolean) {
        with(binding) {
            val visibility = if (visible) {
                startRepetitionButton.visibility = View.INVISIBLE
                View.VISIBLE
            } else {
                startRepetitionButton.visibility = View.VISIBLE
                View.INVISIBLE
            }

            easyButton.visibility = visibility
            goodButton.visibility = visibility
            hardButton.visibility = visibility
            turnButton.visibility = visibility
        }
    }

    private fun onRepeatOrderSwitchCheckedChanged(isChecked: Boolean) {
        setCardViewContent()
        setCardContentColor()
        setIpaPromptContent()
        setLessonOrderPointers(isChecked)
//            binding.cardSideTextView.isClickable = isChecked
    }

    private fun setLessonOrderPointers(isChecked: Boolean) {
        with(binding) {
            if (isChecked) {
                frontSidePointerTextView.text = "F"
                backSidePointerTextView.text = "N"
            } else {
                frontSidePointerTextView.text = "N"
                backSidePointerTextView.text = "F"
            }
        }
    }

}