package com.example.klaf.presentation.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import com.example.klaf.R
import com.example.klaf.databinding.DialogDeckRemovingBinding
import com.example.klaf.presentation.view_models.MainViewModel

class DeckRemovingDialogFragment : DialogFragment() {

    private var _binding: DialogDeckRemovingBinding? = null
    private val binding get() = _binding!!
    private val viewModel by activityViewModels<MainViewModel>()
    private val args by navArgs<DeckRemovingDialogFragmentArgs>()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        _binding = DialogDeckRemovingBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.deckRemovingDialogTitleTextView.text =
            "Would you like to remove the deck \"${args.deckName}\"?"

        binding.buttonCancelDeckRemoving.setOnClickListener {
            findNavController().navigate(R.id.action_deckRemovingDialogFragment_to_deckListFragment)
        }

        binding.buttonConfirmDeckRemoving.setOnClickListener {
            viewModel.onRemoveDeck(args.deckId)
            findNavController().navigate(R.id.action_deckRemovingDialogFragment_to_deckListFragment)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        _binding = null
    }
}