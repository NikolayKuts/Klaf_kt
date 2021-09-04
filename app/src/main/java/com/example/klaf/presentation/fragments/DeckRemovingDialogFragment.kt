package com.example.klaf.presentation.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.DialogFragment
import androidx.navigation.fragment.findNavController
import com.example.klaf.R
import com.example.klaf.databinding.DialogDeckRemovingBinding

class DeckRemovingDialogFragment : DialogFragment() {

    private var _binding: DialogDeckRemovingBinding? = null
    private val binding get() = _binding!!
    private val navController = findNavController()

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
        binding.buttonCancelDeckRemoving.setOnClickListener {
            findNavController().navigate(R.id.action_deckRemovingDialogFragment_to_deckListFragment)
        }

        binding.buttonConfirmDeckRemoving.setOnClickListener { TODO() }
    }

    override fun onDestroy() {
        _binding = null
        super.onDestroy()
    }
}