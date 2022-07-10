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
import com.example.klaf.presentation.auxiliary.TimerCountingState
import com.example.klaf.presentation.auxiliary.TimerCountingState.*
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

        viewLifecycleOwner.lifecycle.addObserver(viewModel.timer)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        viewLifecycleOwner.lifecycle.removeObserver(viewModel.timer)
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
        setCardRepetitionStateObserver()
        setTimerObserver()
    }

    private fun setEventMessageObserver() {
        viewModel.eventMessage.collectWhenStarted(lifecycleScope) { eventMessage ->
            requireContext().showToast(messageId = eventMessage.resId)
        }
    }

    private fun setScreenStateObserver() {
        viewModel.screenState.collectWhenStarted(lifecycleScope) { screenState ->
            val cardRepetitionControlButtons = setOf(
                binding.turnButton,
                binding.easyButton,
                binding.goodButton,
                binding.hardButton
            )

            when (screenState) {
                StartState -> {
                    binding.startRepetitionButton.isVisible = true
                    cardRepetitionControlButtons.onEach { it.isVisible = false }
                }
                RepetitionState -> {
                    binding.startRepetitionButton.isVisible = false
                    cardRepetitionControlButtons.onEach { it.isVisible = true }
                }
                FinishState -> {
                    binding.startRepetitionButton.isVisible = true
                    cardRepetitionControlButtons.onEach { it.isVisible = false }
                    requireContext().showToast(message = "************************")
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
                if (deck.cardQuantity == 0) {
                    requireContext().showToast(messageId = R.string.deck_is_empty)
                }
            }
        }
    }

    private fun setTimerObserver() {
        viewModel.timer.timerState.collectWhenStarted(
            lifecycleScope = viewLifecycleOwner.lifecycleScope,
        ) { timerState ->
            binding.repeatTimerTextView.text = timerState.time
            setTimerColorByTimerState(countingState = timerState.countingState)
        }
    }

    private fun setCardRepetitionStateObserver() {
        viewModel.cardState.collectWhenStarted(lifecycleScope) { cardRepetitionState ->
            if (cardRepetitionState.card == null) return@collectWhenStarted

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
            setRepetitionOrderSwitchPosition(order = cardRepetitionState.repetitionOrder)

            binding.repeatCardEditingActionButton.setOnClickListener {
                navigateToCardEditingFragment(cardId = cardRepetitionState.card.id)
            }

            binding.repeatCardRemovingActionButton.setOnClickListener {
                navigateToCArdRemovingDialogFragment(cardId = cardRepetitionState.card.id)
            }
        }
    }

    private fun setListeners() {
        binding.apply {
            repeatCardAdditionButton.setOnClickListener { navigateToAdditionFragment() }

            startRepetitionButton.setOnClickListener { startRepetition() }

            repeatOrderSwitch.setOnClickListener { changeRepetitionOrder() }

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

    private fun navigateToCardEditingFragment(cardId: Int) {
        RepeatFragmentDirections.actionRepeatFragmentToCardEditingFragment(
            cardId = cardId,
            deckId = args.deckId
        ).also { findNavController().navigate(it) }
    }

    private fun navigateToAdditionFragment() {
        RepeatFragmentDirections.actionRepeatFragmentToCardAdditionFragment(
            deckId = args.deckId
        ).also { findNavController().navigate(it) }
    }

    private fun navigateToCArdRemovingDialogFragment(cardId: Int) {
        RepeatFragmentDirections.actionRepeatFragmentToCardRemovingDialogFragment(
            deckId = args.deckId,
            cardId = cardId
        ).also { findNavController().navigate(it) }
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

    private fun setRepetitionOrderSwitchPosition(order: CardRepetitionOrder) {
        when (order) {
            CardRepetitionOrder.FOREIGN_TO_NATIVE -> {
                binding.repeatOrderSwitch.isChecked = true
            }
            CardRepetitionOrder.NATIVE_TO_FOREIGN -> {
                binding.repeatOrderSwitch.isChecked = false
            }
        }
    }

    private fun setTimerColorByTimerState(countingState: TimerCountingState) {
        val colorId = when (countingState) {
            STOPED, PAUSED -> R.color.timer_is_not_running
            RUN -> R.color.timer_is_running
        }

        binding.repeatTimerTextView.applyTextColor(colorId)
    }

    private fun onClickEasyButton() {
        moveCardByDifficultyRecallingLevel(level = DifficultyRecallingLevel.EASY)
    }

    private fun onClickGoodButton() {
        moveCardByDifficultyRecallingLevel(level = DifficultyRecallingLevel.GOOD)
    }

    private fun onHardButtonClick() {
        moveCardByDifficultyRecallingLevel(level = DifficultyRecallingLevel.HARD)
    }

    private fun moveCardByDifficultyRecallingLevel(level: DifficultyRecallingLevel) {
        viewModel.moveCardByDifficultyRecallingLevel(level = level)
    }
}