package com.example.klaf.presentation.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import com.example.klaf.R
import com.example.klaf.databinding.DialogDeckCreationBinding
import com.example.klaf.domain.auxiliary.DateAssistant
import com.example.klaf.domain.pojo.Deck
import com.example.klaf.domain.showToast
import com.example.klaf.presentation.view_models.MainViewModel

class DeckCreationDialogFragment : DialogFragment() {

    private var _binding: DialogDeckCreationBinding? = null
    private val binding get() = _binding!!

    private val viewModel by activityViewModels<MainViewModel>()

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
        binding.buttonCancelDeckCreation.setOnClickListener { navigateToDeckListFragment() }
        binding.buttonConfirmDeckCreation.setOnClickListener { onConfirmDeckCreation() }
    }

    private fun navigateToDeckListFragment() {
        findNavController().navigate(R.id.action_deckCreationDialogFragment_to_deckListFragment)
    }

    private fun onConfirmDeckCreation() {
        viewModel.deckSource.observe(viewLifecycleOwner) { receivedDecks ->
            val deckName = binding.editTextDeckName.text.toString().trim()
            val deckNames = receivedDecks.map { deck -> deck.name }

            when {
                deckNames.contains(deckName) -> {
                    getString(R.string.such_name_is_already_exist).showToast(requireContext())
                }
                else -> onAddNewDeck(deckName)
            }
        }
    }

    private fun onAddNewDeck(deckName: String) {
        addNewDeck(deckName)
        navigateToDeckListFragment()
        getString(R.string.deck_has_been_created).showToast(requireContext())
        // TODO: 12/29/2021 to translate toast shoeing to DeckListFragment
    }

    private fun addNewDeck(deckName: String) {
        viewModel.addNewDeck(
            Deck(name = deckName, creationDate = DateAssistant().getCurrentDateAsLong())
        )
    }
}