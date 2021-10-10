package com.example.klaf.presentation.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.navArgs
import com.example.klaf.databinding.FragmentCardAdditionBinding
import com.example.klaf.domain.pojo.Card
import com.example.klaf.presentation.view_model_factories.CardAdditionViewModelFactory
import com.example.klaf.presentation.view_models.CardAdditionViewModel

class CardAdditionFragment : Fragment() {

    private var _binding: FragmentCardAdditionBinding? = null
    private val binding get() = _binding!!
    private val args by navArgs<CardAdditionFragmentArgs>()
    private var viewModel: CardAdditionViewModel? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        _binding = FragmentCardAdditionBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        activity?.let { activity ->
            viewModel = ViewModelProvider(
                owner = activity,
                factory = CardAdditionViewModelFactory(
                    context = activity.applicationContext,
                    deckId = args.deckId
                )
            )[CardAdditionViewModel::class.java]

        }
        with(binding) {
            viewModel?.cardQuantity?.observe(viewLifecycleOwner) { quantity ->
                cardQuantityTextView.text = quantity.toString()
            }
            cardAdditionDeckName.text = args.deckName
            applyCardAdditionButton.setOnClickListener {
                onConfirmCardAddition()
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        _binding = null
    }

    private fun onConfirmCardAddition() {
        val newCard = getCardForAddition()
        when {
            newCard != null -> {
                viewModel?.let { viewModel ->
                    viewModel.onAddNewCard(newCard)
                    clearEditTextFields()
                    showToast("the card has been added")
                }
            }
            else -> showToast("native and foreign words must be filled")
        }
    }

    private fun clearEditTextFields() {
        with(binding) {
            nativeWordEditText.setText("")
            foreignWordEditText.setText("")
            ipaEditText.setText("")
        }
    }

    private fun getCardForAddition(): Card? {
        with(binding) {
            val nativeWord = nativeWordEditText.text.toString().trim()
            val foreignWord = foreignWordEditText.text.toString().trim()
            return if (nativeWord.isNotEmpty() && foreignWord.isNotEmpty()) {
                Card(
                    deckId = args.deckId,
                    nativeWord = nativeWord,
                    foreignWord = foreignWord,
                    ipa = ipaEditText.text.toString().trim()
                )
            } else null
        }
    }

    private fun showToast(message: String) {
        Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
    }

}