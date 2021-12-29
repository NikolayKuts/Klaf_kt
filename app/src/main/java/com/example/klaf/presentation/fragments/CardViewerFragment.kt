package com.example.klaf.presentation.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.navArgs
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.klaf.databinding.FragmentCardViewerBinding
import com.example.klaf.presentation.adapters.CardAdapter
import com.example.klaf.presentation.view_model_factories.CardViewerViewModelFactory
import com.example.klaf.presentation.view_models.CardViewerViewModel

class CardViewerFragment : Fragment() {

    private var _binding: FragmentCardViewerBinding? = null
    private val binding get() = _binding!!

    private val args by navArgs<CardViewerFragmentArgs>()
    private val viewModel: CardViewerViewModel by lazy { getCardViewerViewModel() }
    private val cardAdapter = CardAdapter()

    private fun getCardViewerViewModel(): CardViewerViewModel {
        return ViewModelProvider(
            owner = requireActivity(),
            factory = CardViewerViewModelFactory(requireActivity().application, args.deckId)
        )[CardViewerViewModel::class.java]
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        _binding = FragmentCardViewerBinding.inflate(inflater, container, false)
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
            layoutManager = LinearLayoutManager(requireActivity().applicationContext)
            adapter = cardAdapter
        }
    }

    private fun setCardObserver() {
        viewModel.carsSours.observe(viewLifecycleOwner) { receivedCards ->
            cardAdapter.updateData(receivedCards)
        }
    }

    override fun onDestroy() {
        binding.cardRecyclerView.adapter = null
        _binding = null
        super.onDestroy()
    }
}