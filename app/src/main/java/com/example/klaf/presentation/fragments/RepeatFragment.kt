package com.example.klaf.presentation.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import com.example.klaf.R
import com.example.klaf.databinding.FragmentRepeatBinding
import com.example.klaf.domain.pojo.Card
import com.example.klaf.presentation.view_model_factories.RepetitionViewModelFactory
import com.example.klaf.presentation.view_models.RepetitionViewModel

class RepeatFragment : Fragment() {

    private var _binding: FragmentRepeatBinding? = null
    private val binding get() = _binding!!
    private val args by navArgs<RepeatFragmentArgs>()
    private var viewModel: RepetitionViewModel? = null
    private val cards: MutableList<Card> = ArrayList()

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
            viewModel = ViewModelProvider(
                owner = activity,
                factory = RepetitionViewModelFactory(
                    context = activity.applicationContext,
                    deckId = args.deckId)
            )[RepetitionViewModel::class.java]

            viewModel?.cardSours?.observe(viewLifecycleOwner) { receivedCards ->
                cards.clear()
                cards.addAll(receivedCards)
                binding.cardSideTextView.text = cards[0].nativeWord
            }
        }
        binding.repeatDeckNameTextView.text = args.deckName

        binding.repeatCardAdditionButton.setOnClickListener {
            RepeatFragmentDirections.actionRepeatFragmentToCardAdditionFragment(
                deckId = args.deckId,
                deckName = args.deckName
            )
                .also { findNavController().navigate(it) }
        }

    }

    override fun onDestroy() {
        super.onDestroy()
        _binding = null
    }
}