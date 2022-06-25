package com.example.klaf.presentation.cardRemovingDialog

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.DialogFragment
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.navArgs
import com.example.klaf.R
import com.example.klaf.databinding.DialogCardRemovingBinding
import com.example.klaf.presentation.repeatDeck.RepetitionViewModel

class CardRemovingDialogFragment : DialogFragment() {

    private var _binding: DialogCardRemovingBinding? = null
    private val binding get() = _binding!!

    private val args by navArgs<CardRemovingDialogFragmentArgs>()
    private val viewModel: RepetitionViewModel by lazy { getRepetitionViewModel() }

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

        setListeners()
    }

    override fun onDestroy() {
        super.onDestroy()
        _binding = null
    }

    private fun getRepetitionViewModel(): RepetitionViewModel {
        return ViewModelProvider(
            owner = this,
//            factory = RepetitionViewModelFactory(
//                context = requireActivity().applicationContext,
//                deckId = args.deckId)
        )[RepetitionViewModel::class.java]
    }

    private fun setListeners() {
        binding.cancelCardRemovingButton.setOnClickListener { dismiss() }
        binding.confirmCardRemovingButton.setOnClickListener { onConfirmCardRemoving() }
    }

    private fun onConfirmCardRemoving() {
        viewModel.deleteCard(cardId = args.cardId)
        dismiss()

        Toast.makeText(
            context,
            getString(R.string.card_has_been_deleted),
            Toast.LENGTH_SHORT
        ).show()
    }
}