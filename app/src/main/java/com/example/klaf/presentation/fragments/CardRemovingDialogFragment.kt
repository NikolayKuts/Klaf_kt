package com.example.klaf.presentation.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import com.example.klaf.R
import com.example.klaf.databinding.DialogCardRemovingBinding
import com.example.klaf.presentation.view_model_factories.RepetitionViewModelFactory
import com.example.klaf.presentation.view_models.RepetitionViewModel

class CardRemovingDialogFragment : DialogFragment() {

    private var _binding: DialogCardRemovingBinding? = null
    private val binding get() = _binding!!
    private var viewModel: RepetitionViewModel? = null
    private val args by navArgs<CardRemovingDialogFragmentArgs>()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        _binding = DialogCardRemovingBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        activity?.let { activity ->

            viewModel = ViewModelProvider(
                owner = this,
                factory = RepetitionViewModelFactory(
                    context = activity.applicationContext,
                    deckId = args.deckId)
            )[RepetitionViewModel::class.java]

            binding.cancelCardRemovingButton.setOnClickListener { navigateToRepeatFragment() }

            binding.confirmCardRemovingButton.setOnClickListener {
                viewModel?.removeCard(cardId = args.cardId)
                navigateToRepeatFragment()
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        _binding = null
    }

    private fun navigateToRepeatFragment() {
//        CardRemovingDialogFragmentDirections.actionCardRemovingDialogFragmentToRepeatFragment(
//            deckId = args.deckId
//        )
//            .also { findNavController().navigate(it) }
//        findNavController().popBackStack()
        this.dismiss()
    }
}