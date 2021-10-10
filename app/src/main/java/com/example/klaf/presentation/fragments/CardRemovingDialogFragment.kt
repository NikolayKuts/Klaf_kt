package com.example.klaf.presentation.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.DialogFragment
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import com.example.klaf.R
import com.example.klaf.databinding.DialogCardRemovingBinding
import com.example.klaf.presentation.view_models.RepetitionViewModel

class CardRemovingDialogFragment : DialogFragment() {

    private var _binding: DialogCardRemovingBinding? = null
    private val binding get() = _binding!!
    private var viewModel: RepetitionViewModel? = null
    private val args by navArgs<CardRemovingDialogFragmentArgs>()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = DialogCardRemovingBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        activity?.let { activity ->
            viewModel = ViewModelProvider(owner = activity)[RepetitionViewModel::class.java]

            binding.cancelCardRemovingButton.setOnClickListener {
                findNavController().navigate(
                    R.id.action_cardRemovingDialogFragment_to_repeatFragment
                )
            }

            binding.confirmCardRemovingButton.setOnClickListener {

                viewModel?.removeCard(args.cardId)
                findNavController().navigate(
                    R.id.action_cardRemovingDialogFragment_to_repeatFragment
                )
            }
        }

    }

    override fun onDestroy() {
        super.onDestroy()
        _binding = null
    }
}