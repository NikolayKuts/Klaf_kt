package com.example.klaf.presentation.fragments

import android.content.Context
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.navArgs
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.klaf.databinding.FragmentCardViewerBinding
import com.example.klaf.presentation.adapters.CardAdapter
import com.example.klaf.presentation.view_model_factories.CardViewerViewModelFactory
import com.example.klaf.presentation.view_models.CardViewerViewModel

class CardViewerFragment : Fragment() {

    private var _binding: FragmentCardViewerBinding? = null
    private val binding get() = _binding!!
    private val args by navArgs<CardViewerFragmentArgs>()
    private var viewModel: CardViewerViewModel? = null

    override fun onAttach(context: Context) {
        super.onAttach(context)
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
        activity?.let { activity ->
            viewModel = ViewModelProvider(
                owner = activity,
                factory = CardViewerViewModelFactory(activity.application, args.deckId)
            )[CardViewerViewModel::class.java]

            binding.textViewItemDeckName.text = args.deckName
            val recyclerView = binding.recyclerviewCards
            val adapter = CardAdapter()
            recyclerView.layoutManager = LinearLayoutManager(activity.applicationContext)
            recyclerView.adapter = adapter

            val deckId = args.deckId
            viewModel?.let { viewModel ->
                CardViewerViewModelFactory(activity.application, deckId)
                viewModel.carsSours.observe(viewLifecycleOwner) { cards ->
                    adapter.cards = cards
                }
            }
        }
    }

    override fun onDestroy() {
        _binding = null
        super.onDestroy()
    }
}