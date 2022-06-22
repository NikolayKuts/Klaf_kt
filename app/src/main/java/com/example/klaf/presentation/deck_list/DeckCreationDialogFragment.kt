package com.example.klaf.presentation.deck_list

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import com.example.klaf.databinding.DialogDeckCreationBinding

class DeckCreationDialogFragment : DialogFragment() {

    private var _binding: DialogDeckCreationBinding? = null
    private val binding get() = _binding!!

    private val viewModel by activityViewModels<DeckListViewModel>()

    private val namController by lazy { findNavController() }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        _binding = DialogDeckCreationBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setListeners()
    }

    override fun onDestroy() {
        _binding = null
        super.onDestroy()
    }

    private fun setListeners() {
        binding.buttonCancelDeckCreation.setOnClickListener { namController.popBackStack() }
        binding.buttonConfirmDeckCreation.setOnClickListener { confirmDeckCreation() }
    }

    private fun confirmDeckCreation() {
        val deckName = binding.editTextDeckName.text.toString().trim()

        viewModel.createNewDeck(deckName = deckName)
        namController.popBackStack()
    }
}