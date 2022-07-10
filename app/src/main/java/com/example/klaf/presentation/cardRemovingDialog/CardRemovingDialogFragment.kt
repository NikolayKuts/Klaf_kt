package com.example.klaf.presentation.cardRemovingDialog

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import com.example.klaf.databinding.DialogCardRemovingBinding
import com.example.klaf.presentation.common.collectWhenStarted
import com.example.klaf.presentation.common.showToast
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class CardRemovingDialogFragment : DialogFragment() {

    private var _binding: DialogCardRemovingBinding? = null
    private val binding get() = _binding!!

    private val args by navArgs<CardRemovingDialogFragmentArgs>()
    private val viewModel: CardRemovingDialogViewModel by viewModels()

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
        setObservers()
    }

    override fun onDestroy() {
        super.onDestroy()
        _binding = null
    }

    private fun setObservers() {
        setEventMessageObserver()
        setCardRemovingStateObserver()
    }

    private fun setEventMessageObserver() {
        viewModel.eventMessage.collectWhenStarted(viewLifecycleOwner.lifecycleScope) { eventMessage ->
            requireContext().showToast(messageId = eventMessage.resId)
        }
    }

    private fun setCardRemovingStateObserver() {
        viewModel.isCardRemoved.collectWhenStarted(viewLifecycleOwner.lifecycleScope) { isCardRemoved ->
            if (isCardRemoved) {
                closeDialog()
            }
        }
    }

    private fun setListeners() {
        binding.cancelCardRemovingButton.setOnClickListener { closeDialog() }
        binding.confirmCardRemovingButton.setOnClickListener { confirmCardRemoving() }
    }

    private fun closeDialog() {
        findNavController().popBackStack()
    }

    private fun confirmCardRemoving() {
        viewModel.removeCardFromDeck(cardId = args.cardId, deckId = args.deckId)
    }
}