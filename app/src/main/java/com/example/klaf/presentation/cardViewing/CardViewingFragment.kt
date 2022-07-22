package com.example.klaf.presentation.cardViewing

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.navArgs
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.klaf.databinding.FragmentCardViewingBinding
import com.example.klaf.presentation.common.collectWhenStarted
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class CardViewingFragment : Fragment() {

    private var _binding: FragmentCardViewingBinding? = null
    private val binding get() = _binding!!

    private val args by navArgs<CardViewingFragmentArgs>()

    @Inject
    lateinit var assistedFactory: CardViewingViewModelFactory.CardViewingViewModelAssistedFactory
    private val viewModel: CardViewingViewModel by viewModels {
        CardViewingViewModelFactory(assistedFactory = assistedFactory, deckId = args.deckId)
    }

    private val cardAdapter = CardAdapter()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        _binding = FragmentCardViewingBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.textViewItemDeckName.text = args.deckName
        initCarRecyclerView()
        setCardObserver()
    }

    private fun initCarRecyclerView() {
        binding.cardRecyclerView.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = cardAdapter
        }
    }

    private fun setCardObserver() {
        viewModel.cards.collectWhenStarted(lifecycleScope) { cards ->
            cardAdapter.updateData(cards = cards)
        }
    }

    override fun onDestroy() {
        binding.cardRecyclerView.adapter = null
        _binding = null
        super.onDestroy()
    }
}