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
    private val decks = ArrayList<Deck>()
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

            viewModel.deckSours.observe(viewLifecycleOwner) { deckList ->
                decks.clear()
                decks.addAll(deckList)
                adapter.decks = decks
            }

            val navController = findNavController()
            binding.createDeckActionButton.setOnClickListener {
                navController.navigate(
                    DeckListFragmentDirections.actionDeckListFragmentToDeckCreationDialogFragment()
                )
            }
            adapter.onClick = {
                navController.navigate(R.id.action_deckListFragment_to_repeatFragment)
                Toast.makeText(context, "onClick", Toast.LENGTH_SHORT).show()
            }

            adapter.onLongClick = { view, deck -> showDeckPopupMenu(view, deck) }
        }
    }

    override fun onResume() {
        super.onResume()
        viewModel.updateData()
    }

    override fun onDestroy() {
        _binding = null
        super.onDestroy()
    }

    private fun showDeckPopupMenu(view: View, deck: Deck) {
        PopupMenu(view.context, view).apply{
            inflate(R.menu.deck_popup_menu)
            show()
            setOnMenuItemClickListener { item ->
                val navController = findNavController()
                when (item.itemId) {
                    R.id.item_deck_deleting -> {
                        DeckListFragmentDirections
                            .actionDeckListFragmentToDeckRemovingDialogFragment(deckId = deck.id)
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
                        true
                    }
                    R.id.item_card_addition -> {
                        true
                    }
                    else -> false
                }
            }
        }
    }
}
