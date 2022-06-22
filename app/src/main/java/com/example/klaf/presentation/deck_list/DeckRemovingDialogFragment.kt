package com.example.klaf.presentation.deck_list

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

class DeckRemovingDialogFragment : DialogFragment() {

    private var _binding: DialogDeckRemovingBinding? = null
    private val binding get() = _binding!!

    private val args by navArgs<DeckRemovingDialogFragmentArgs>()
    private val navController by lazy { findNavController() }

    private val viewModel by activityViewModels<DeckListViewModel>()

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

        setDialogTitle()
        setListeners()
    }

    override fun onDestroy() {
        super.onDestroy()
        _binding = null
    }

    private fun setDialogTitle() {
        binding.deckRemovingDialogTitleTextView.text =
            getString(R.string.would_you_like_to_remove_deck, args.deckName)
    }

    private fun setListeners() {
        binding.buttonCancelDeckRemoving.setOnClickListener { navController.popBackStack() }
        binding.buttonConfirmDeckRemoving.setOnClickListener { onConfirmDeckRemoving() }
    }

    private fun onConfirmDeckRemoving() {
        viewModel.removeDeck(args.deckId)
        navController.popBackStack()
    }
}