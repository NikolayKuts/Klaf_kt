package com.example.klaf.presentation.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.PopupMenu
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.klaf.R
import com.example.klaf.databinding.FragmentDeckListBinding
import com.example.klaf.domain.pojo.Deck
import com.example.klaf.presentation.adapters.DeckAdapter
import com.example.klaf.presentation.view_models.MainViewModel

class DeckListFragment : Fragment() {

    private var _binding: FragmentDeckListBinding? = null
    private val binding get() = _binding!!
    private var recyclerViewDecks: RecyclerView? = null
    private val decks: MutableList<Deck> = ArrayList()
    private val viewModel by activityViewModels<MainViewModel>()
    private val adapter = DeckAdapter()

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
        activity?.let { activity ->

            recyclerViewDecks = binding.recyclerviewDecks

            recyclerViewDecks?.let { recycler ->
                recycler.layoutManager = LinearLayoutManager(activity.applicationContext)
                recycler.adapter = adapter
            }

            viewModel.deckSource.observe(viewLifecycleOwner) { receivedDecks ->
                decks.clear()
                decks.addAll(receivedDecks)
                adapter.decks = decks
            }

            setOnCreateDeckClickListener()

            adapter.onClick = { deck ->
                DeckListFragmentDirections.actionDeckListFragmentToRepeatFragment(
                    deckId = deck.id,
                    deckName = deck.name
                ).also { findNavController().navigate(it) }
            }

            adapter.onPopupMenuClick = { view, deck -> showDeckPopupMenu(view, deck) }
        }
    }

    override fun onResume() {
        super.onResume()
    }

    override fun onDestroy() {
        _binding = null
        super.onDestroy()
    }

    private fun showDeckPopupMenu(view: View, deck: Deck) {
        PopupMenu(view.context, view).apply {
            inflate(R.menu.deck_popup_menu)
            show()
            setOnMenuItemClickListener { item ->
                val navController = findNavController()

                when (item.itemId) {
                    R.id.item_deck_deleting -> {
                        DeckListFragmentDirections
                            .actionDeckListFragmentToDeckRemovingDialogFragment(
                                deckId = deck.id,
                                deckName = deck.name
                            )
                            .also { navController.navigate(it) }
                        true
                    }
                    R.id.item_deck_renaming -> {
                        DeckListFragmentDirections
                            .actionDeckListFragmentToDeckRenamingDialogFragment(deckId = deck.id)
                            .also { navController.navigate(it) }
                        true
                    }
                    R.id.item_card_showing -> {
                        viewModel.isDeckNotEmpty(deckId = deck.id) { isNotEmpty ->
                            if (isNotEmpty) {
                                DeckListFragmentDirections
                                    .actionDeckListFragmentToCardViewerFragment(
                                        deckId = deck.id,
                                        deckName = deck.name
                                    )
                                    .also { navController.navigate(it) }

                            } else {
                                Toast.makeText(
                                    context,
                                    "There are no cards in the deck",
                                    Toast.LENGTH_SHORT
                                )
                                    .show()
                            }
                        }
                        true
                    }
                    R.id.item_card_addition -> {
                        DeckListFragmentDirections.actionDeckListFragmentToCardAdditionFragment(
                            deckId = deck.id
                        )
                            .also { navController.navigate(it) }
                        true
                    }
                    else -> false
                }
            }
        }
    }

    private fun setOnCreateDeckClickListener() {
        binding.createDeckActionButton.setOnClickListener {
            findNavController().navigate(
                DeckListFragmentDirections.actionDeckListFragmentToDeckCreationDialogFragment()
            )
        }
    }
}
