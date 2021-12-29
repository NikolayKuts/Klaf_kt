package com.example.klaf.presentation.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.PopupMenu
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.navigation.NavController
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.klaf.R
import com.example.klaf.databinding.FragmentDeckListBinding
import com.example.klaf.domain.pojo.Deck
import com.example.klaf.domain.update
import com.example.klaf.presentation.adapters.DeckAdapter
import com.example.klaf.presentation.view_models.MainViewModel

class DeckListFragment : Fragment() {

    private var _binding: FragmentDeckListBinding? = null
    private val binding get() = _binding!!

    private val decks: MutableList<Deck> = ArrayList()
    private val viewModel by activityViewModels<MainViewModel>()
    private val deckAdapter = DeckAdapter(
        onClick = ::onItemClick,
        onPopupMenuClick = ::onPopupMenuClick
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
        setDeckObserver()
        setOnCreateDeckClickListener()
    }

    override fun onDestroy() {
        binding.deckRecyclerView.adapter = null
        _binding = null
        super.onDestroy()
    }

    private fun initDeckRecyclerView() {
        binding.deckRecyclerView.apply {
            layoutManager = LinearLayoutManager(requireActivity().applicationContext)
            adapter = deckAdapter
        }
    }

    private fun setDeckObserver() {
        viewModel.deckSource.observe(viewLifecycleOwner) { receivedDecks ->
            decks.update(receivedDecks)
            deckAdapter.updateData(newDecks = decks)
        }
    }

    private fun setOnCreateDeckClickListener() {
        binding.createDeckActionButton.setOnClickListener {
            findNavController().navigate(
                DeckListFragmentDirections.actionDeckListFragmentToDeckCreationDialogFragment()
            )
        }
    }

    private fun onItemClick(deck: Deck) {
        DeckListFragmentDirections.actionDeckListFragmentToRepeatFragment(
            deckId = deck.id
        ).also { findNavController().navigate(it) }
    }

    private fun onPopupMenuClick(view: View, deck: Deck) {
        showDeckPopupMenu(view, deck)
    }

    private fun showDeckPopupMenu(view: View, deck: Deck) {
        PopupMenu(view.context, view).apply {
            inflate(R.menu.deck_popup_menu)
            show()

            setOnMenuItemClickListener { item ->
                val navController = findNavController()

                when (item.itemId) {
                    R.id.item_deck_deleting -> {
                        navigateToDeckRemovingDialogFragment(deck, navController)
                        true
                    }
                    R.id.item_deck_renaming -> {
                        navigateToDeckRenamingDialogFragment(deck, navController)
                        true
                    }
                    R.id.item_card_showing -> {
                        onIsDeckNotEmptyCheck(deck, navController)
                        true
                    }
                    R.id.item_card_addition -> {
                        navigateToCardAdditionFragment(deck, navController)
                        true
                    }
                    else -> false
                }
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
        DeckListFragmentDirections
            .actionDeckListFragmentToDeckRenamingDialogFragment(deckId = deck.id)
            .also { navController.navigate(it) }
    }

    private fun onIsDeckNotEmptyCheck(deck: Deck, navController: NavController) {
        viewModel.isDeckNotEmpty(deckId = deck.id) { isNotEmpty ->
            if (isNotEmpty) {
                navigateToCardViewerFragment(deck, navController)
            } else {
                showToast(getString(R.string.there_are_no_card_in_deck))
            }
        }
    }

    private fun navigateToCardAdditionFragment(deck: Deck, navController: NavController) {
        DeckListFragmentDirections.actionDeckListFragmentToCardAdditionFragment(
            deckId = deck.id
        ).also { navController.navigate(it) }
    }

    private fun showToast(message: String) {
        Toast.makeText(
            context,
            message,
            Toast.LENGTH_SHORT
        ).show()
    }

    private fun navigateToCardViewerFragment(deck: Deck, navController: NavController) {
        DeckListFragmentDirections.actionDeckListFragmentToCardViewerFragment(
            deckId = deck.id,
            deckName = deck.name
        ).also { navController.navigate(it) }
    }
}
