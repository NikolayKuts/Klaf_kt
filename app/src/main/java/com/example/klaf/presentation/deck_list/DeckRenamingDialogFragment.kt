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
import com.example.klaf.databinding.DialogDeckRenamingBinding
import com.example.klaf.domain.pojo.Deck
import com.example.klaf.presentation.common.showToast

class DeckRenamingDialogFragment : DialogFragment() {

    private var _binding: DialogDeckRenamingBinding? = null
    private val binding get() = _binding!!

    private val args by navArgs<DeckRenamingDialogFragmentArgs>()
    private val navController by lazy { findNavController() }

    private val viewModel by activityViewModels<DeckListViewModel>()


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

        viewModel.getDeckById(deckId = args.deckId).let { deck: Deck? ->
            binding.editTextDeckRenamingField.setText(
                deck?.name ?: getString(R.string.deck_is_not_found)
            )

            setListeners(deck = deck)
        }
    }

    override fun onDestroy() {
        _binding = null
        super.onDestroy()
    }

    private fun setListeners(deck: Deck?) {
        binding.buttonCancelDeckRenaming.setOnClickListener { navController.popBackStack() }
        binding.buttonConfirmDeckRenaming.setOnClickListener { onConfirmDeckRenaming(deck = deck) }
    }

    private fun onConfirmDeckRenaming(deck: Deck?) {
        val newName = binding.editTextDeckRenamingField.text.toString().trim()

        viewModel.renameDeck(deck = deck, newName = newName)
        navController.popBackStack()

//        deck?.let { notNullableDeck ->
//            when (val newName = binding.editTextDeckRenamingField.text.toString().trim()) {
//
//                "" -> requireContext().showToast(message = getString(R.string.type_new_deck_name))
//
//                notNullableDeck.name -> {
//                    requireContext().showToast(message = getString(R.string.deck_name_is_not_changed))
//                }
//
//                else -> {
//                    viewModel.renameDeck(deck = deck, newName = newName)
//                    navController.popBackStack()
//                }
//            }
//        }
    }
}