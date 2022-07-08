package com.example.klaf.presentation.repeatDeck

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.klaf.R
import com.example.klaf.databinding.FragmentRepeatBinding
import com.example.klaf.domain.common.CardRepetitionOrder
import com.example.klaf.domain.common.CardSide
import com.example.klaf.domain.entities.Deck
import com.example.klaf.domain.enums.DifficultyRecallingLevel
import com.example.klaf.domain.ipa.IpaProcessor
import com.example.klaf.presentation.adapters.IpaPromptAdapter
import com.example.klaf.presentation.auxiliary.RepeatTimer
import com.example.klaf.presentation.common.applyTextColor
import com.example.klaf.presentation.common.collectWhenStarted
import com.example.klaf.presentation.common.showToast
import com.example.klaf.presentation.repeatDeck.RepetitionScreenState.*
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class RepeatFragment : Fragment() {

    private var _binding: FragmentRepeatBinding? = null
    private val binding get() = _binding!!

    private val args by navArgs<RepeatFragmentArgs>()
    private val timer by viewModels<RepeatTimer>() // TODO rid viewModel implementation from Timer

    @Inject
    lateinit var assistedFactory: RepetitionViewModelAssistedFactory
    private val viewModel: RepetitionViewModel by viewModels {
        RepetitionViewModelFactory(assistedFactory = assistedFactory, deckId = args.deckId)
    }

    private val ipaPromptAdapter: IpaPromptAdapter = IpaPromptAdapter()

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

        initIpaRecyclerView()
        setObservers()
        setListeners()
    }

    override fun onDestroy() {
        binding.ipaRecyclerView.adapter = null
        _binding = null
        super.onDestroy()
    }


    private fun initIpaRecyclerView() {
        binding.ipaRecyclerView.apply {
            layoutManager = LinearLayoutManager(
                requireContext(),
                LinearLayoutManager.HORIZONTAL,
                false
            )
            adapter = ipaPromptAdapter
        }
    }

    private fun setObservers() {
        setEventMessageObserver()
        setScreenStateObserver()
        setDeckObserver()
//        setCurrentCardObserver()
        setCardRepetitionSateObserver()
    }

    private fun setEventMessageObserver() {
        viewModel.eventMessage.collectWhenStarted(lifecycleScope) { eventMessage ->
            requireContext().showToast(messageId = eventMessage.resId)
        }
    }

    private fun setScreenStateObserver() {
        viewModel.screenState.collectWhenStarted(lifecycleScope) { screenState ->
            when (screenState) {
                StartState -> {
                    binding.startRepetitionButton.isVisible = true
                    binding.turnButton.isVisible = false
                    binding.easyButton.isVisible = false
                    binding.goodButton.isVisible = false
                    binding.hardButton.isVisible = false
                }
                RepetitionState -> {
                    binding.startRepetitionButton.isVisible = false
                    binding.turnButton.isVisible = true
                    binding.easyButton.isVisible = true
                    binding.goodButton.isVisible = true
                    binding.hardButton.isVisible = true
                }
                FinishState -> {
                    binding.startRepetitionButton.isVisible = true
                    binding.turnButton.isVisible = false
                    binding.easyButton.isVisible = false
                    binding.goodButton.isVisible = false
                    binding.hardButton.isVisible = false
                }
            }
        }
    }

    private fun setDeckObserver() {
        viewModel.deck.collectWhenStarted(lifecycleScope) { deck: Deck? ->
            if (deck == null) {
                requireContext().showToast(messageId = R.string.problem_with_fetching_deck)
            } else {
                binding.repeatDeckNameTextView.text = deck.name
            }
        }
    }

    private fun setTimerObserver() {
//        viewModel.time.observe(viewLifecycleOwner) { time: String ->
//            binding.repeatTimerTextView.text = time
//        }
    }

//    private fun setCurrentCardObserver() {
//        viewModel.currentCard.collectWhenStarted(lifecycleScope) { currentCard ->
////            setCardViewContent(card = currentCard)
//            binding.turnButton.setOnClickListener { turnCard(card = currentCard) }
//        }
//    }

    private fun setCardRepetitionSateObserver() {
        viewModel.cardState.collectWhenStarted(lifecycleScope) { cardRepetitionState ->
            when (cardRepetitionState.repetitionOrder) {
                CardRepetitionOrder.NATIVE_TO_FOREIGN -> {
                    if (cardRepetitionState.side == CardSide.FRONT) {
                        binding.cardSideTextView.text = cardRepetitionState.card.nativeWord
                        ipaPromptAdapter.setData(letterInfos = emptyList())
                    } else {
                        binding.cardSideTextView.text = cardRepetitionState.card.foreignWord
                        ipaPromptAdapter.setData(
                            letterInfos = IpaProcessor.getLetterInfos(cardRepetitionState.card.ipa)
                        )
                    }
                }
                CardRepetitionOrder.FOREIGN_TO_NATIVE -> {
                    if (cardRepetitionState.side == CardSide.FRONT) {
                        binding.cardSideTextView.text = cardRepetitionState.card.foreignWord
                        ipaPromptAdapter.setData(
                            letterInfos = IpaProcessor.getLetterInfos(cardRepetitionState.card.ipa)
                        )
                    } else {
                        binding.cardSideTextView.text = cardRepetitionState.card.nativeWord
                        ipaPromptAdapter.setData(letterInfos = emptyList())
                    }
                }
            }
            setRepetitionOrderPointers(order = cardRepetitionState.repetitionOrder)
        }
    }

    private fun setListeners() {
        binding.apply {
            repeatCardAdditionButton.setOnClickListener { navigateToAdditionFragment() }
            repeatCardEditingActionButton.setOnClickListener { onClickCardEditingButton() }
            repeatCardRemovingActionButton.setOnClickListener { onClickCardRemovingButton() }

            startRepetitionButton.setOnClickListener { startRepetition() }

            repeatOrderSwitch.setOnCheckedChangeListener { _, _ -> changeRepetitionOrder() }

            turnButton.setOnClickListener { turnCard() }

            easyButton.setOnClickListener { onClickEasyButton() }
            goodButton.setOnClickListener { onClickGoodButton() }
            hardButton.setOnClickListener { onHardButtonClick() }
        }
    }

    private fun startRepetition() {
        viewModel.startRepeating()
    }

    private fun turnCard() {
        viewModel.turnCard()
    }

    private fun onClickCardEditingButton() {
//        if (cards.isEmpty()) {
//            requireContext().showToast(messageId = R.string.there_is_nothing_to_change)
//        } else {
//            navigateToCardEditingFragment()
//        }
    }

    private fun onClickCardRemovingButton() {
//        if (cards.isEmpty()) {
//            requireContext().showToast(messageId = R.string.there_are_no_cards_to_remove)
//        } else {
//            navigateToCArdRemovingDialogFragment()
//        }
    }

    private fun navigateToAdditionFragment() {
        RepeatFragmentDirections.actionRepeatFragmentToCardAdditionFragment(
            deckId = args.deckId
        ).also { findNavController().navigate(it) }
    }

    private fun navigateToCardEditingFragment() {
//        RepeatFragmentDirections.actionRepeatFragmentToCardEditingFragment(
//            cardId = cards[0].id,
//            deckId = args.deckId
//        ).also { findNavController().navigate(it) }
    }

    private fun navigateToCArdRemovingDialogFragment() {
//        RepeatFragmentDirections.actionRepeatFragmentToCardRemovingDialogFragment(
//            deckId = args.deckId,
//            cardId = cards[0].id
//        ).also { findNavController().navigate(it) }
    }

    private fun setIpaPromptContent() {
    }

    private fun setCardContentColor() {
//        if (cards.isNotEmpty()) {
//            setCardContentColorBySide()
//        } else {
//            binding.cardSideTextView.applyTextColor(colorId = R.color.empty_card_content_color)
//        }
    }

    private fun changeRepetitionOrder() {
        viewModel.changeRepetitionOrder()
    }

    private fun setRepetitionOrderPointers(order: CardRepetitionOrder) {
        val native = getString(R.string.pointer_native)
        val foreign = getString(R.string.pointer_foreign)

        when (order) {
            CardRepetitionOrder.NATIVE_TO_FOREIGN -> {
                binding.frontSidePointerTextView.text = native
                binding.backSidePointerTextView.text = foreign
            }
            CardRepetitionOrder.FOREIGN_TO_NATIVE -> {
                binding.frontSidePointerTextView.text = foreign
                binding.backSidePointerTextView.text = native
            }
        }
    }

    private fun setTimerColorByTimerState(isRunning: Boolean) {
        val colorId = if (isRunning) R.color.timer_is_running else R.color.timer_is_not_running
        binding.repeatTimerTextView.applyTextColor(colorId)
    }

    private fun onClickEasyButton() {

    }

    private fun onClickGoodButton() {

    }

    private fun onHardButtonClick() {

    }

    private fun moveCardByDifficultyRecallingLevel(level: DifficultyRecallingLevel) {
//        val cardForMoving = cards[0]
//        cards.removeAt(0)
//
//        val newPosition = when (level) {
//            EASY -> cards.size
//            GOOD -> cards.size * 3 / 4
//            HARD -> cards.size / 4
//        }
//        cards.add(newPosition, cardForMoving)
    }


    private fun onRepetitionFinished() {
        // TODO: 11/17/2021 implement repetition scheduling
    }


    private fun getUpdatedDesk(): Deck? {
        TODO()
//        return repeatDeck?.let { repeatDeck ->
//
//            val updatedLastRepetitionDate: Long
//            val currentRepetitionDuration: Long
//            val updatedLastSucceededRepetition: Boolean
//
//            if (repeatDeck.repeatQuantity % 2 != 0) {
//                updatedLastRepetitionDate = DateAssistant.getCurrentDateAsLong()
//                currentRepetitionDuration = timer.savedTotalTime
//                updatedLastSucceededRepetition =
//                    DateAssistant.isRepetitionSucceeded(repeatDeck, currentRepetitionDuration)
//            } else {
//                updatedLastRepetitionDate = repeatDeck.lastRepeatDate
//                currentRepetitionDuration = repeatDeck.lastRepeatDuration
//                updatedLastSucceededRepetition = repeatDeck.isLastRepetitionSucceeded
//            }
//
//            val newScheduledDate =
//                DateAssistant.getNextScheduledRepeatDate(repeatDeck, currentRepetitionDuration)
//
//            Deck(
//                name = repeatDeck.name,
//                creationDate = repeatDeck.creationDate,
//                id = repeatDeck.id,
//                cardQuantity = repeatDeck.cardQuantity,
//                repeatDay = DateAssistant.getUpdatedRepeatDay(repeatDeck),
//                scheduledDate = newScheduledDate,
//                lastRepeatDate = updatedLastRepetitionDate,
//                repeatQuantity = repeatDeck.repeatQuantity + 1,
//                lastRepeatDuration = currentRepetitionDuration,
//                isLastRepetitionSucceeded = updatedLastSucceededRepetition
//            )
//        }
    }
}