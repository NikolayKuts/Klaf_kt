package com.example.klaf.presentation.deckRepetition

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import androidx.navigation.navGraphViewModels
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.klaf.R
import com.example.klaf.databinding.FragmentRepeatBinding
import com.example.klaf.domain.common.CardRepetitionOrder
import com.example.klaf.domain.common.CardRepetitionOrder.FOREIGN_TO_NATIVE
import com.example.klaf.domain.common.CardRepetitionOrder.NATIVE_TO_FOREIGN
import com.example.klaf.domain.common.CardSide
import com.example.klaf.domain.common.CardSide.BACK
import com.example.klaf.domain.common.CardSide.FRONT
import com.example.klaf.domain.entities.Deck
import com.example.klaf.domain.enums.DifficultyRecallingLevel
import com.example.klaf.domain.ipa.IpaProcessor
import com.example.klaf.presentation.adapters.IpaPromptAdapter
import com.example.klaf.presentation.common.TimerCountingState
import com.example.klaf.presentation.common.TimerCountingState.*
import com.example.klaf.presentation.common.applyTextColor
import com.example.klaf.presentation.common.collectWhenStarted
import com.example.klaf.presentation.common.showToast
import com.example.klaf.presentation.deckRepetition.RepetitionScreenState.*
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class RepeatFragment : Fragment() {

    private var _binding: FragmentRepeatBinding? = null
    private val binding get() = _binding!!

    private val args by navArgs<RepeatFragmentArgs>()

    @Inject
    lateinit var assistedFactory: RepetitionViewModelAssistedFactory
    private val viewModel: RepetitionViewModel by navGraphViewModels(R.id.repeatFragment) {
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
        subscribeObservers()
        setListeners()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        viewLifecycleOwner.lifecycle.removeObserver(viewModel.timer)
    }

    override fun onDestroy() {
        super.onDestroy()
        binding.ipaRecyclerView.adapter = null
        _binding = null
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

    private fun subscribeObservers() {
        setEventMessageObserver()
        setScreenStateObserver()
        setDeckObserver()
        setCardRepetitionStateObserver()
        setTimerObserver()

        viewLifecycleOwner.lifecycle.addObserver(viewModel.timer)
    }

    private fun setEventMessageObserver() {
        viewModel.eventMessage.collectWhenStarted(viewLifecycleOwner.lifecycleScope) { eventMessage ->
            requireContext().showToast(messageId = eventMessage.resId)
        }
    }

    private fun setScreenStateObserver() {
        viewModel.screenState.collectWhenStarted(viewLifecycleOwner.lifecycleScope) { screenState ->
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
                is FinishState -> {
                    binding.startRepetitionButton.isVisible = true
                    cardRepetitionControlButtons.onEach { it.isVisible = false }

                    with(screenState) {
                        navigateToDeckRepetitionInfoDialogFragment(
                            currentDuration = currentDuration,
                            lastDuration = previousDuration,
                            scheduledDate = scheduledDate,
                            previousScheduledDate = previousScheduledDate,
                            lastRepetitionIterationDate = lastRepetitionIterationDate,
                            repetitionQuantity = repetitionQuantity,
                            lastSuccessMark = lastSuccessMark
                        )
                    }
                }
            }
        }
    }

    private fun setDeckObserver() {
        viewModel.deck.collectWhenStarted(viewLifecycleOwner.lifecycleScope) { deck: Deck? ->
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
        viewModel.cardState.collectWhenStarted(
            viewLifecycleOwner.lifecycleScope
        ) { cardRepetitionState ->
            if (cardRepetitionState.card == null) return@collectWhenStarted

            when (cardRepetitionState.repetitionOrder) {
                NATIVE_TO_FOREIGN -> {
                    if (cardRepetitionState.side == FRONT) {
                        binding.cardSideTextView.text = cardRepetitionState.card.nativeWord
                        ipaPromptAdapter.setData(letterInfos = emptyList())
                    } else {
                        binding.cardSideTextView.text = cardRepetitionState.card.foreignWord
                        ipaPromptAdapter.setData(
                            letterInfos = IpaProcessor.getLetterInfos(cardRepetitionState.card.ipa)
                        )
                    }
                }
                FOREIGN_TO_NATIVE -> {
                    if (cardRepetitionState.side == FRONT) {
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

            setCardContentColor(cardSide = cardRepetitionState.side)

            binding.repeatCardEditingActionButton.setOnClickListener {
                navigateToCardEditingFragment(cardId = cardRepetitionState.card.id)
            }

            binding.repeatCardRemovingActionButton.setOnClickListener {
                navigateToCardRemovingDialogFragment(cardId = cardRepetitionState.card.id)
            }
        }
    }

    private fun setListeners() {
        binding.apply {
            repeatCardAdditionButton.setOnClickListener { navigateToAdditionFragment() }

            startRepetitionButton.setOnClickListener { startRepetition() }

            repeatOrderSwitch.setOnClickListener { changeRepetitionOrder() }

            turnButton.setOnClickListener { turnCard() }

            cardSideTextView.setOnClickListener { viewModel.pronounce() }

            easyButton.setOnClickListener { onClickEasyButton() }
            goodButton.setOnClickListener { onClickGoodButton() }
            hardButton.setOnClickListener { onHardButtonClick() }

//            setDeckRepetitionInfoDialogResultListener()
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

    private fun navigateToCardRemovingDialogFragment(cardId: Int) {
        RepeatFragmentDirections.actionRepeatFragmentToCardRemovingDialogFragment(
            deckId = args.deckId,
            cardId = cardId
        ).also { findNavController().navigate(it) }
    }

    private fun navigateToDeckRepetitionInfoDialogFragment(
        currentDuration: String,
        lastDuration: String,
        scheduledDate: Long,
        previousScheduledDate: Long,
        lastRepetitionIterationDate: String?,
        repetitionQuantity: String,
        lastSuccessMark: String,
    ) {
        RepeatFragmentDirections.actionRepeatFragmentToDeckRepetitionInfoDialogFragment(
            currentDuration = currentDuration,
            lastDuration = lastDuration,
            scheduledDate = scheduledDate,
            previusScheduledDate = previousScheduledDate,
            lastRepetitionIterationDate = lastRepetitionIterationDate,
            repetitionQuantity = repetitionQuantity,
            lastSuccessMark = lastSuccessMark
        ).also { findNavController().navigate(directions = it) }
    }

    private fun setIpaPromptContent() {
    }

    private fun setCardContentColor(cardSide: CardSide) {
        val colorId = when (cardSide) {
            BACK -> R.color.back_card_content_color
            FRONT -> R.color.front_card_content_color
        }

        binding.cardSideTextView.applyTextColor(colorId = colorId)
    }

    private fun changeRepetitionOrder() {
        viewModel.changeRepetitionOrder()
    }

    private fun setRepetitionOrderPointers(order: CardRepetitionOrder) {
        val native = getString(R.string.pointer_native)
        val foreign = getString(R.string.pointer_foreign)

        when (order) {
            NATIVE_TO_FOREIGN -> {
                binding.frontSidePointerTextView.text = native
                binding.backSidePointerTextView.text = foreign
            }
            FOREIGN_TO_NATIVE -> {
                binding.frontSidePointerTextView.text = foreign
                binding.backSidePointerTextView.text = native
            }
        }
    }

    private fun setRepetitionOrderSwitchPosition(order: CardRepetitionOrder) {
        when (order) {
            FOREIGN_TO_NATIVE -> binding.repeatOrderSwitch.isChecked = true
            NATIVE_TO_FOREIGN -> binding.repeatOrderSwitch.isChecked = false
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