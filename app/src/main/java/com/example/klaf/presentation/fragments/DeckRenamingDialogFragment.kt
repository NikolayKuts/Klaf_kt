package com.example.klaf.presentation.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.Toast
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import com.example.klaf.R
import com.example.klaf.databinding.DialogDeckRenamingBinding
import com.example.klaf.domain.pojo.Deck
import com.example.klaf.presentation.view_models.MainViewModel

class DeckRenamingDialogFragment : DialogFragment() {

    private var _binding: DialogDeckRenamingBinding? = null
    private val binding get() = _binding!!
    private val viewModel by activityViewModels<MainViewModel>()
    private val args by navArgs<DeckRenamingDialogFragmentArgs>()

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
        val navController = findNavController()
        var oldDeck: Deck? = null
        viewModel.onGetDeckById(args.deckId) { deck ->
            oldDeck = deck
            binding.editTextDeckRenamingField.setText(deck.name)
        }

        binding.buttonCancelDeckRenaming.setOnClickListener {
            navController.navigate(R.id.action_deckRenamingDialogFragment_to_deckListFragment)
        }

        binding.buttonConfirmDeckRenaming.setOnClickListener {
            oldDeck?.let { deck ->
                when (val newName = binding.editTextDeckRenamingField.text.toString().trim()) {
                    "" -> {
                        Toast.makeText(context, "Type a new name", Toast.LENGTH_SHORT).show()
                    }
                    deck.name -> {
                        Toast.makeText(context, "The name isn't changed", Toast.LENGTH_SHORT).show()
                    }
                    else -> {
                        val newDeck = deck.copy(name = newName)
                        viewModel.addNewDeck(newDeck)
                        navController.navigate(R.id.action_deckRenamingDialogFragment_to_deckListFragment)
                    }
                }
            }
        }
    }

    override fun onDestroy() {
        _binding = null
        super.onDestroy()
    }
}