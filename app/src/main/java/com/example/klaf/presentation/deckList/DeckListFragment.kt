package com.example.klaf.presentation.deckList

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.PopupMenu
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.NavController
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.klaf.R
import com.example.klaf.databinding.FragmentDeckListBinding
import com.example.klaf.domain.entities.Deck
import com.example.klaf.presentation.common.collectWhenStarted
import com.example.klaf.presentation.common.showToast
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class DeckListFragment : Fragment() {

    private var _binding: FragmentDeckListBinding? = null
    private val binding get() = _binding!!

    private val navController by lazy { findNavController() }

    private val viewModel by activityViewModels<DeckListViewModel>()

    private val deckAdapter = DeckAdapter(
        onClick = ::navigateToRepeatFragment,
        onItemMenuClick = ::showDeckPopupMenu
    )

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        _binding = FragmentDeckListBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        initDeckRecyclerView()
        subscribeObservers()
        setOnCreateDeckClickListener()
    }

    override fun onDestroy() {
        binding.deckRecyclerView.adapter = null
        _binding = null
        super.onDestroy()
    }

    private fun initDeckRecyclerView() {
        binding.deckRecyclerView.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = deckAdapter
        }
    }

    private fun subscribeObservers() {
        subscribeDeckObserver()
        subscribeEventMessageObserver()
    }

    private fun subscribeDeckObserver() {
        viewModel.deckSource.collectWhenStarted(viewLifecycleOwner.lifecycleScope) { decks ->
            // TODO: 12/29/2021 implement toast showing on deleting and creating deck
            deckAdapter.updateData(newDecks = decks)
        }
    }

    private fun subscribeEventMessageObserver() {
        viewModel.eventMessage.collectWhenStarted(viewLifecycleOwner.lifecycleScope) { eventMessage ->
            requireContext().showToast(messageId = eventMessage.resId)
        }
    }

    private fun setOnCreateDeckClickListener() {
        binding.createDeckActionButton.setOnClickListener {
            DeckListFragmentDirections.actionDeckListFragmentToDeckCreationDialogFragment()
                .also { navController.navigate(directions = it) }
        }
    }

    private fun navigateToRepeatFragment(deck: Deck) {
        DeckListFragmentDirections.actionDeckListFragmentToRepeatFragment(
            deckId = deck.id
        ).also { navController.navigate(it) }
    }

    private fun showDeckPopupMenu(view: View, deck: Deck) {
        PopupMenu(view.context, view).apply {
            inflate(R.menu.deck_popup_menu)
            show()

            setOnMenuItemClickListener { item ->

                when (item.itemId) {
                    R.id.item_deleting_deck -> {
                        navigateToDeckRemovingDialogFragment(deck, navController)
                    }
                    R.id.item_renaming_deck -> {
                        navigateToDeckRenamingDialogFragment(deck, navController)
                    }
                    R.id.item_browsing_deck_cards -> {
                        performAccordingToDeckListState(deck, navController)
                    }
                    R.id.item_card_addition -> {
                        navigateToCardAdditionFragment(deck, navController)
                    }
                    else -> return@setOnMenuItemClickListener false
                }

                true
            }
        }
    }

    private fun navigateToDeckRemovingDialogFragment(deck: Deck, navController: NavController) {
        DeckListFragmentDirections.actionDeckListFragmentToDeckRemovingDialogFragment(
            deckId = deck.id,
            deckName = deck.name
        ).also { navController.navigate(it) }
    }

    private fun navigateToDeckRenamingDialogFragment(deck: Deck, navController: NavController) {
        DeckListFragmentDirections.actionDeckListFragmentToDeckRenamingDialogFragment(
            deckId = deck.id
        ).also { navController.navigate(it) }
    }

    private fun performAccordingToDeckListState(deck: Deck, navController: NavController) {
        if (deck.cardQuantity > 0) {
            navigateToCardViewerFragment(deck, navController)
        } else {
            requireContext().showToast(message = getString(R.string.there_are_no_card_in_deck))
        }
    }

    private fun navigateToCardAdditionFragment(deck: Deck, navController: NavController) {
        DeckListFragmentDirections.actionDeckListFragmentToCardAdditionFragment(
            deckId = deck.id
        ).also { navController.navigate(it) }
    }

    private fun navigateToCardViewerFragment(deck: Deck, navController: NavController) {
        DeckListFragmentDirections.actionDeckListFragmentToCardViewerFragment(
            deckId = deck.id,
            deckName = deck.name
        ).also { navController.navigate(it) }
    }
}
