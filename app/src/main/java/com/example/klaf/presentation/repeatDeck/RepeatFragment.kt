package com.example.klaf.presentation.repeatDeck

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.annotation.ColorRes
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.klaf.R
import com.example.klaf.databinding.FragmentRepeatBinding
import com.example.klaf.domain.auxiliary.DateAssistant
import com.example.klaf.domain.common.update
import com.example.klaf.domain.entities.Card
import com.example.klaf.domain.entities.Deck
import com.example.klaf.domain.enums.DifficultyRecallingLevel
import com.example.klaf.domain.enums.DifficultyRecallingLevel.*
import com.example.klaf.domain.ipa.IpaProcessor
import com.example.klaf.domain.ipa.LetterInfo
import com.example.klaf.presentation.adapters.IpaPromptAdapter
import com.example.klaf.presentation.auxiliary.RepeatTimer
import com.example.klaf.presentation.common.collectWhenStarted
import com.example.klaf.presentation.common.showToast
import dagger.hilt.android.AndroidEntryPoint
import java.util.*
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

    //    private val cards = LinkedList<Card>()
    private val ipaPrompts = mutableListOf<LetterInfo>()

    private var isFrontSide: Boolean = true
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

        setButtonVisibilities(false)
        initIpaRecyclerView()
        lifecycle.addObserver(timer)
        timer.onAction = { setTimerColorByTimerState(timer.isRunning) }
        observeDeck() //onGetDeck()
        onTimeObserving()
        onCardObserving()
//        setListeners()

        ipaPromptAdapter.setData(listOf(LetterInfo("L", true)))
    }

    override fun onDestroy() {
        binding.ipaRecyclerView.adapter = null
        _binding = null
        super.onDestroy()
    }

    private fun setButtonVisibilities(visible: Boolean) {
        binding.apply {
            startRepetitionButton.isVisible = !visible
            easyButton.isVisible = visible
            goodButton.isVisible = visible
            hardButton.isVisible = visible
            turnButton.isVisible = visible
        }
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

    private fun observeDeck() {
        viewModel.deck.collectWhenStarted(lifecycleScope) { deck: Deck? ->
            if (deck == null) {
                TODO("show toast that there is a problem with fetching deck")
            } else {
                repeatDeck = deck
                binding.repeatDeckNameTextView.text = deck.name
            }

        }
//        viewModel.onGetDeck(args.deckId) getDeck@{ deck: Deck? ->
//            deck ?: return@getDeck
//
//        }
    }

    private fun onTimeObserving() {
        timer.time.observe(viewLifecycleOwner) { time: String ->
            binding.repeatTimerTextView.text = time
        }
    }

    private fun onCardObserving() {
        viewModel.cardSource.collectWhenStarted(lifecycleScope) { receivedCards ->
            if (receivedCards.isEmpty()) {
                resetRepetitionProgress()
            }

            if (viewModel.savedProgressCards.isNotEmpty()) {
//                cards.update(viewModel.getCardsByProgress(receivedCards))
                viewModel.saveRepetitionProgress(receivedCards) // TODO encapsulate in the viewModel
            }

            setCardViewContent(cards = receivedCards)
            setCardContentColor(cards = receivedCards)
            setIpaPromptContent(cards = receivedCards)
            setListeners(cards = receivedCards)
        }

//        viewModel.cardSource.collectWhenStarted(lifecycleScope) { receivedCards ->
//            cards.update(receivedCards)
//            if (cards.isEmpty()) {
//                resetRepetitionProgress()
//            }
//
//            if (viewModel.savedProgressCards.isNotEmpty()) {
//                cards.update(viewModel.getCardsByProgress(receivedCards))
//                viewModel.saveRepetitionProgress(cards)
//            }
//
//            setCardViewContent()
//            setCardContentColor()
//            setIpaPromptContent()
//        }
    }

    private fun setListeners(cards: List<Card>) {
        binding.apply {
            repeatCardAdditionButton.setOnClickListener { navigateToAdditionFragment() }
            repeatCardEditingActionButton.setOnClickListener { onClickCardEditingButton(cards = cards) }
            repeatCardRemovingActionButton.setOnClickListener { onClickCardRemovingButton(cards = cards) }
            startRepetitionButton.setOnClickListener { onClickStartButton(cards = cards) }
            turnButton.setOnClickListener { onClickTurnButton(cards = cards) }
            repeatOrderSwitch.setOnCheckedChangeListener { _, isChecked ->
                onRepeatOrderSwitchCheckedChanged(isChecked, cards = cards)
            }

            easyButton.setOnClickListener { onClickEasyButton(cards = cards) }
            goodButton.setOnClickListener { onClickGoodButton(cards = cards) }
            hardButton.setOnClickListener { onHardButtonClick(cards = cards) }
        }
    }

    private fun resetRepetitionProgress() {
        viewModel.clearProgress()
        startElement = null
        goodElement = null
        hardElement = null
        timer.stopCounting()
    }

//    private fun getRepetitionViewModel(): RepetitionViewModel {
//        return ViewModelProvider(
//            owner = this,
////            factory = RepetitionViewModelFactory(
////                context = requireActivity().applicationContext,
////                deckId = args.deckId
////            )
//        )[RepetitionViewModel::class.java]
//    }

    private fun onClickStartButton(cards: List<Card>) {
        if (cards.isEmpty()) return
        setButtonVisibilities(true)
        timer.runCounting()
    }

    private fun onClickTurnButton(cards: List<Card>) {
        isFrontSide = !isFrontSide
        setCardViewContent(cards = cards)
        setCardContentColor(cards = cards)
        setIpaPromptContent(cards = cards)
    }

    private fun onClickCardEditingButton(cards: List<Card>) {
        if (cards.isEmpty()) {
            requireContext().showToast(message = getString(R.string.there_is_nothing_to_change))
        } else {
            navigateToCardEditingFragment(cards = cards)
        }
    }

    private fun onClickCardRemovingButton(cards: List<Card>) {
        if (cards.isEmpty()) {
            requireContext().showToast(message = getString(R.string.there_are_no_cards_to_remove))
        } else {
            navigateToCArdRemovingDialogFragment(cards = cards)
        }
    }

    private fun navigateToAdditionFragment() {
        RepeatFragmentDirections.actionRepeatFragmentToCardAdditionFragment(
            deckId = args.deckId
        ).also { findNavController().navigate(it) }
    }

    private fun navigateToCardEditingFragment(cards: List<Card>) {
        RepeatFragmentDirections.actionRepeatFragmentToCardEditingFragment(
            cardId = cards[0].id,
            deckId = args.deckId
        ).also { findNavController().navigate(it) }
    }

    private fun navigateToCArdRemovingDialogFragment(cards: List<Card>) {
        RepeatFragmentDirections.actionRepeatFragmentToCardRemovingDialogFragment(
            deckId = args.deckId,
            cardId = cards[0].id
        ).also { findNavController().navigate(it) }
    }

    private fun setIpaPromptContent(cards: List<Card>) {
        if (cards.isNotEmpty()) {
            val card = cards[0]
            val ipaProcessor = IpaProcessor()

            if (isFrontSide) {
                Log.i("app_log", "setIpaPromptContent: $card")
                ipaPrompts.update(ipaProcessor.getIpaPrompts(card.ipa))
            } else {
                ipaPrompts.clear()
            }
//
//            if (binding.repeatOrderSwitch.isChecked) {
//                when (isFrontSide) {
//                    true -> ipaPrompts.update(ipaProcessor.getIpaPrompts(card.ipa))
//                    else -> ipaPrompts.clear()
//                }
//            } else {
//                when (isFrontSide) {
//                    true -> ipaPrompts.clear()
//                    else -> ipaPrompts.update(ipaProcessor.getIpaPrompts(card.ipa))
//                }
//            }
        }
        ipaPromptAdapter.setData(ipaPrompts)
    }

    private fun setCardContentColor(cards: List<Card>) {
        if (cards.isNotEmpty()) {
            setCardContentColorBySide()
        } else {
            binding.cardSideTextView.applyTextColor(colorId = R.color.empty_card_content_color)
        }
    }

    private fun setCardContentColorBySide() {
        val colorId = if (isFrontSide) {
            R.color.front_card_content_color
        } else {
            R.color.back_card_content_color
        }
        binding.cardSideTextView.applyTextColor(colorId)
    }

    private fun TextView.applyTextColor(@ColorRes colorId: Int) {
        setTextColor(ContextCompat.getColor(context, colorId))
    }

    private fun setCardViewContent(cards: List<Card>) {
        if (cards.isEmpty()) {
            binding.cardSideTextView.text = getString(R.string.deck_is_empty)
        } else {
            binding.cardSideTextView.text = getCardContentBySide(cards[0])
        }
    }

    private fun getCardContentBySide(card: Card): String {
        return if (isFrontSide) card.nativeWord else card.foreignWord
    }

    private fun onRepeatOrderSwitchCheckedChanged(isChecked: Boolean, cards: List<Card>) {
        isFrontSide = !isFrontSide
        setCardViewContent(cards = cards)
        setCardContentColor(cards = cards)
        setIpaPromptContent(cards = cards)
        setLessonOrderPointers(isChecked)
//            binding.cardSideTextView.isClickable = isChecked
    }

    private fun setLessonOrderPointers(isChecked: Boolean) {
        val native = getString(R.string.pointer_native)
        val foreign = getString(R.string.pointer_foreign)

        binding.frontSidePointerTextView.text = if (isChecked) foreign else native
        binding.backSidePointerTextView.text = if (isChecked) native else foreign
    }

    private fun setTimerColorByTimerState(isRunning: Boolean) {
        val colorId = if (isRunning) R.color.timer_is_running else R.color.timer_is_not_running
        binding.repeatTimerTextView.applyTextColor(colorId)
    }

    private fun onClickEasyButton(cards: List<Card>) {
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

            moveCardByDifficultyRecallingLevel(EASY)
            viewModel.saveRepetitionProgress(cards)
            onRepetitionFinished(cards = cards)        // TODO: 11/8/2021  implement onFinishLesson()
            isFrontSide = true
            setCardViewContent(cards = cards)
            setIpaPromptContent(cards = cards)
            // TODO: 11/8/2021 implement setOnTextViewWordClickable(switchRepetitionOrder.isChecked())
        }
        // TODO: 11/8/2021 implement closeFloatingButtonIfOpened()
    }

    private fun onClickGoodButton(cards: List<Card>) {
        if (cards.isEmpty()) return
        goodElement = cards[0]
        moveCardByDifficultyRecallingLevel(GOOD)
        viewModel.saveRepetitionProgress(cards)
        isFrontSide = true
        setCardViewContent(cards = cards)
        setIpaPromptContent(cards = cards)
//            setOnTextViewWordClickable(switchRepetitionOrder.isChecked())
//        closeFloatingButtonIfOpened()
    }

    private fun onHardButtonClick(cards: List<Card>) {
        if (cards.isEmpty()) return
        hardElement = cards[0]
        moveCardByDifficultyRecallingLevel(HARD)
        viewModel.saveRepetitionProgress(cards)
        isFrontSide = true
        setCardViewContent(cards = cards)
        setIpaPromptContent(cards = cards)
//            setOnTextViewWordClickable(switchRepetitionOrder.isChecked())
//        closeFloatingButtonIfOpened()
    }

    private fun moveCardByDifficultyRecallingLevel(
//        cards: List<Card>,
        level: DifficultyRecallingLevel
    ) {
//        val cardForMoving = cards[0]
        viewModel.moveCardByDifficultyRecallingLevel(level = level)
//        cards.removeAt(0)
//
//        val newPosition = when (level) {
//            EASY -> cards.size
//            GOOD -> cards.size * 3 / 4
//            HARD -> cards.size / 4
//        }
//        cards.add(newPosition, cardForMoving)
    }


    private fun onRepetitionFinished(cards: List<Card>) {
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
                            updatedDeck.scheduledDate > DateAssistant.getCurrentDateAsLong()
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
            val dateAssistant = DateAssistant

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