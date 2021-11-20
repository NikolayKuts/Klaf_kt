package com.example.klaf.presentation.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import com.example.klaf.R
import com.example.klaf.databinding.DialogDeckCreationBinding
import com.example.klaf.domain.auxiliary.DateAssistant
import com.example.klaf.domain.pojo.Deck
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
        val navController = findNavController()

        with(binding) {
            buttonCancelDeckCreation.setOnClickListener {
                navController.navigate(R.id.action_deckCreationDialogFragment_to_deckListFragment)
            }

            buttonConfirmDeckCreation.setOnClickListener {
                val deckName = editTextDeckName.text.toString().trim()

                viewModel.deckSource.observe(viewLifecycleOwner) { receivedDecks ->
                    val deckNames = receivedDecks?.map { deck -> deck.name }
                    when {
                        (deckNames?.contains(deckName) == true) -> {
                            Toast.makeText(
                                context, "such name is already exist", Toast.LENGTH_SHORT
                            ).show()
                        }
                        else -> {
                            viewModel.addNewDeck(
                                Deck(
                                    name = deckName,
                                    creationDate = DateAssistant().getCurrentDateAsLong()
                                )
                            )
                            navController.navigate(
                                R.id.action_deckCreationDialogFragment_to_deckListFragment
                            )
                            Toast.makeText(context, "it's done", Toast.LENGTH_SHORT).show()
                        }
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