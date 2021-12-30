package com.example.klaf.presentation.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.navArgs
import com.example.klaf.R
import com.example.klaf.databinding.DialogDeckRenamingBinding
import com.example.klaf.domain.pojo.Deck
import com.example.klaf.domain.showToast
import com.example.klaf.presentation.view_models.MainViewModel

class DeckRenamingDialogFragment : DialogFragment() {

    private var _binding: DialogDeckRenamingBinding? = null
    private val binding get() = _binding!!

    private val args by navArgs<DeckRenamingDialogFragmentArgs>()
    private val viewModel by activityViewModels<MainViewModel>()

    private var deck: Deck? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        _binding = DialogDeckRenamingBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)



        viewModel.onGetDeckById(args.deckId) { receivedDeck: Deck? ->
            deck = receivedDeck
            binding.editTextDeckRenamingField.setText(
                receivedDeck?.name ?: getString(R.string.deck_is_not_found)
            )
        }

        setListeners()
    }

    override fun onDestroy() {
        _binding = null
        super.onDestroy()
    }

    private fun setListeners() {
        binding.buttonCancelDeckRenaming.setOnClickListener { dismiss() }
        binding.buttonConfirmDeckRenaming.setOnClickListener { onConfirmDeckRenaming() }
    }

    private fun onConfirmDeckRenaming() {
        deck?.let { notNullableDeck ->
            when (val newName = binding.editTextDeckRenamingField.text.toString().trim()) {

                "" -> getString(R.string.type_new_deck_name).showToast(requireContext())

                notNullableDeck.name -> {
                    getString(R.string.deck_name_is_not_changed).showToast(requireContext())
                }

                else -> {
                    viewModel.addNewDeck(notNullableDeck.copy(name = newName))
                    dismiss()
                }
            }
        }
    }
}