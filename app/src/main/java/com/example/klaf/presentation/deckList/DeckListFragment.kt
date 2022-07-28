package com.example.klaf.presentation.deckList

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.navigation.navGraphViewModels
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.klaf.R
import com.example.klaf.databinding.FragmentDeckListBinding
import com.example.klaf.domain.entities.Deck
import com.example.klaf.presentation.common.collectWhenStarted
import com.example.klaf.presentation.common.showToast
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class DeckListFragment : Fragment() {

    private var _binding: FragmentDeckListBinding? = null
    private val binding get() = _binding!!

    private val navController by lazy { findNavController() }

    @Inject
    lateinit var assistedFactory: DeckListViewModelAssistedFactory
    private val viewModel: DeckListViewModel by navGraphViewModels(R.id.deckListFragment) {
        DeckListViewModelFactory(assistedFactory = assistedFactory)
    }

    private val deckAdapter = DeckAdapter(
        onClick = ::navigateToRepeatFragment,
        onItemMenuClick = ::navigateToDeckNavigationDialog
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

    private fun navigateToDeckNavigationDialog(deck: Deck) {
        DeckListFragmentDirections.actionDeckListFragmentToDeckNavigationDialog(
            deckId = deck.id,
            deckName = deck.name
        ).also { navController.navigate(directions = it) }
    }
}
