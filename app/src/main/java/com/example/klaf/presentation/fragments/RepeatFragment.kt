package com.example.klaf.presentation.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.klaf.R
import com.example.klaf.databinding.FragmentRepeatBinding
import com.example.klaf.domain.auxiliary.DateAssistant
import com.example.klaf.domain.ipa.IpaProcessor
import com.example.klaf.domain.ipa.LetterInfo
import com.example.klaf.domain.pojo.Card
import com.example.klaf.domain.pojo.Deck
import com.example.klaf.domain.update
import com.example.klaf.presentation.adapters.IpaPromptAdapter
import com.example.klaf.presentation.auxiliary.RepeatTimer
import com.example.klaf.presentation.view_model_factories.RepetitionViewModelFactory
import com.example.klaf.presentation.view_models.RepetitionViewModel
import kotlin.collections.ArrayList

private const val EASY_DIFFICULTY_LEVEL_MEMORIES = 0
private const val GOOD_DIFFICULTY_LEVEL_MEMORIES = 1
private const val HARD_DIFFICULTY_LEVEL_MEMORIES = 2

class RepeatFragment : Fragment() {

    private var _binding: FragmentRepeatBinding? = null
    private val binding get() = _binding!!

    private val args by navArgs<RepeatFragmentArgs>()
    private var viewModel: RepetitionViewModel? = null
    private val cards: MutableList<Card> = ArrayList()
    private var isFrontSide: Boolean = true
    private val ipaPrompts = ArrayList<LetterInfo>()
    private val adapter: IpaPromptAdapter by lazy { IpaPromptAdapter() }
    private val timer by viewModels<RepeatTimer>()
    private var repeatDeck: Deck? = null

    private var startElement: Card? = null
    private var goodElement: Card? = null
    private var hardElement: Card? = null

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
            with(binding) {

                initializeViewModel()

                timer.onAction = { setColorTimerByTimerState(timer.isRunning) }
                lifecycle.addObserver(timer)
                timer.time.observe(viewLifecycleOwner) { time: String ->
                    repeatTimerTextView.text = time
                }

                ipaRecyclerView.layoutManager = LinearLayoutManager(
                    activity.applicationContext,
                    LinearLayoutManager.HORIZONTAL,
                    false
                )
                ipaRecyclerView.adapter = adapter

                viewModel?.let { viewModel ->

                    viewModel.cardSource.observe(viewLifecycleOwner) { receivedCards ->
                        cards.update(receivedCards)

                        if (cards.isEmpty()) {
                            viewModel.clearProgress()
                            startElement = null
                            goodElement = null
                            hardElement = null
                            timer.stopCounting()
                        }

                        if (viewModel.savedProgressCards.isNotEmpty()) {
                            cards.update(viewModel.getCardsByProgress(receivedCards))
                            viewModel.saveRepetitionProgress(cards)
                        }

                        setCardViewContent()
                        setCardContentColor()
                        setIpaPromptContent()
                    }

                    viewModel.onGetDeck(args.deckId) { deck: Deck? ->
                        if (deck != null) {
                            repeatDeck = deck
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

                    easyButton.setOnClickListener { onClickEasyButton() }
                    goodButton.setOnClickListener { onClickGoodButton() }
                    hardButton.setOnClickListener { onHardButtonClick() }
                }
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        _binding = null
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
            timer.runCounting()
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

    private fun setColorTimerByTimerState(isRunning: Boolean) {
        with(binding) {
            val context = repeatTimerTextView.context
            if (isRunning) {
                repeatTimerTextView.setTextColor(ContextCompat.getColor(context,
                    R.color.timer_is_running))
            } else {
                repeatTimerTextView.setTextColor(
                    ContextCompat.getColor(context, R.color.timer_is_not_running)
                )
            }
        }
    }

    private fun onClickEasyButton() {
        if (cards.isNotEmpty()) {
            val card = cards[0]
            if (startElement == null) {
                startElement = card
            } else if (startElement?.id == card.id) {
                startElement = Card(-1, "", "", "")
            }
            if (goodElement != null && goodElement?.id == card.id) {
                goodElement = null
            }
            if (hardElement != null && hardElement?.id == card.id) {
                hardElement = null
            }

            moveCardByDifficultyLevelMemories(EASY_DIFFICULTY_LEVEL_MEMORIES)
            viewModel?.saveRepetitionProgress(cards)
            onRepetitionFinished()        // TODO: 11/8/2021  implement onFinishLesson()
            isFrontSide = true
            setCardViewContent()
            setIpaPromptContent()
            // TODO: 11/8/2021 implement setOnTextViewWordClickable(switchRepetitionOrder.isChecked())
        }
        // TODO: 11/8/2021 implement closeFloatingButtonIfOpened()
    }

    private fun onClickGoodButton() {
        if (cards.isNotEmpty()) {
            goodElement = cards[0]
            moveCardByDifficultyLevelMemories(GOOD_DIFFICULTY_LEVEL_MEMORIES)
            viewModel?.saveRepetitionProgress(cards)
            isFrontSide = true
            setCardViewContent()
            setIpaPromptContent()
//            setOnTextViewWordClickable(switchRepetitionOrder.isChecked())
        }
//        closeFloatingButtonIfOpened()
    }

    private fun onHardButtonClick() {
        if (cards.isNotEmpty()) {
            hardElement = cards[0]
            moveCardByDifficultyLevelMemories(HARD_DIFFICULTY_LEVEL_MEMORIES)
            viewModel?.saveRepetitionProgress(cards)
            isFrontSide = true
            setCardViewContent()
            setIpaPromptContent()
//            setOnTextViewWordClickable(switchRepetitionOrder.isChecked())
        }
//        closeFloatingButtonIfOpened()
    }

    private fun moveCardByDifficultyLevelMemories(difficultyLevelMemories: Int) {
        val cardForMoving = cards[0]
        cards.removeAt(0)

        when (difficultyLevelMemories) {
            EASY_DIFFICULTY_LEVEL_MEMORIES -> {
                cards.add(cardForMoving)
            }
            GOOD_DIFFICULTY_LEVEL_MEMORIES -> {
                val newPosition = cards.size * 3 / 4
                cards.add(newPosition, cardForMoving)
            }
            HARD_DIFFICULTY_LEVEL_MEMORIES -> {
                val newPosition = cards.size / 4
                cards.add(newPosition, cardForMoving)
            }
        }
    }


    private fun onRepetitionFinished() {
        startElement?.let { _startElement ->

            if (
                (_startElement.id == cards[0].id || _startElement.id == -1)
                && goodElement == null && hardElement == null
            ) {

                timer.stopCounting()
                val updatedDeck = getUpdatedDesk()
                if (updatedDeck != null) {

                    setButtonVisibilities(false)
                    startElement = null

                    repeatDeck?.let { repeatDeck ->
                        if (
                            updatedDeck.scheduledDate > DateAssistant().getCurrentDateAsLong()
                            && repeatDeck.repeatQuantity > 5
                            && repeatDeck.repeatQuantity % 2 == 0
                        ) {
                            // TODO: 11/17/2021 implement repetition scheduling
                        }
                    }

                } else {

                }
            }
        }
    }


    private fun getUpdatedDesk(): Deck? {
        return repeatDeck?.let { repeatDeck ->
            val dateAssistant = DateAssistant()

            val updatedLastRepetitionDate: Long
            val currentRepetitionDuration: Long
            val updatedLastSucceededRepetition: Boolean

            if (repeatDeck.repeatQuantity % 2 != 0) {
                updatedLastRepetitionDate = dateAssistant.getCurrentDateAsLong()
                currentRepetitionDuration = timer.savedTotalTime
                updatedLastSucceededRepetition =
                    dateAssistant.isRepetitionSucceeded(repeatDeck, currentRepetitionDuration)
            } else {
                updatedLastRepetitionDate = repeatDeck.lastRepeatDate
                currentRepetitionDuration = repeatDeck.lastRepeatDuration
                updatedLastSucceededRepetition = repeatDeck.isLastRepetitionSucceeded
            }

            val newScheduledDate =
                dateAssistant.getNextScheduledRepeatDate(repeatDeck, currentRepetitionDuration)

            Deck(
                name = repeatDeck.name,
                creationDate = repeatDeck.creationDate,
                id = repeatDeck.id,
                cardQuantity = repeatDeck.cardQuantity,
                repeatDay = dateAssistant.getUpdatedRepeatDay(repeatDeck),
                scheduledDate = newScheduledDate,
                lastRepeatDate = updatedLastRepetitionDate,
                repeatQuantity = repeatDeck.repeatQuantity + 1,
                lastRepeatDuration = currentRepetitionDuration,
                isLastRepetitionSucceeded = updatedLastSucceededRepetition
            )
        }
    }
}