package com.example.klaf.presentation.deckRepetitionInfo

import android.app.Dialog
import android.content.DialogInterface
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.view.View
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Card
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.unit.dp
import androidx.fragment.app.DialogFragment
import androidx.navigation.fragment.navArgs
import androidx.navigation.navGraphViewModels
import com.example.klaf.R
import com.example.klaf.domain.common.calculateDetailedScheduledRange
import com.example.klaf.presentation.deckRepetition.RepetitionViewModel
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class DeckRepetitionInfoDialogFragment : DialogFragment(R.layout.dialog_deck_repetition_info) {

    private val args by navArgs<DeckRepetitionInfoDialogFragmentArgs>()

    private val viewModel by navGraphViewModels<RepetitionViewModel>(R.id.repeatFragment)


    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        return super.onCreateDialog(savedInstanceState).apply {
            window?.setBackgroundDrawable(ColorDrawable(android.graphics.Color.TRANSPARENT))
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        view.findViewById<ComposeView>(R.id.compose_view_repetition_info).setContent {
            deckRepetitionInfoView(
                currentDuration = args.currentDuration,
                lastDuration = args.lastDuration,
                newScheduledDate = args.scheduledDate.calculateDetailedScheduledRange(
                    context = requireContext()
                ),
                lastScheduledDate = args.previusScheduledDate.calculateDetailedScheduledRange(
                    context = requireContext()
                ),
                lastRepetitionIterationDate = args.lastRepetitionIterationDate,
                repetitionQuantity = args.repetitionQuantity,
                lastSuccessMark = args.lastSuccessMark
            )
        }
    }

    override fun onDismiss(dialog: DialogInterface) {
        super.onDismiss(dialog)
        viewModel.moveToStartScreenState()
    }

    @Composable
    private fun deckRepetitionInfoView(
        currentDuration: String,
        lastDuration: String,
        newScheduledDate: String,
        lastScheduledDate: String,
        lastRepetitionIterationDate: String?,
        repetitionQuantity: String,
        lastSuccessMark: String,
    ) {
        Card(
            shape = RoundedCornerShape(16.dp),
            elevation = 4.dp,
        ) {
            Column(
                modifier = Modifier
                    .width(300.dp)
                    .padding(16.dp)
            ) {
                Row(
                    modifier = Modifier
                        .background(Color(0x8DC3894A))
                        .fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween) {
                    Text(text = "deck:")
                    Text(text = "me deck")
                }

                Spacer(modifier = Modifier.height(16.dp))

                Column {
                    InfoItem(
                        textPointer = "current iteration duration:",
                        infoValue = currentDuration
                    )
                    InfoItem(textPointer = "previous iteration duration:", infoValue = lastDuration)
                    Spacer(modifier = Modifier.height(16.dp))

                    InfoItem(textPointer = "scheduled date:", infoValue = newScheduledDate)
                    InfoItem(textPointer = "previous scheduled date:",
                        infoValue = lastScheduledDate)
                    Spacer(modifier = Modifier.height(16.dp))

                    InfoItem(
                        textPointer = "last iteration date:",
                        infoValue = lastRepetitionIterationDate ?: "-- // --"
                    )
                    Spacer(modifier = Modifier.height(16.dp))

                    InfoItem(textPointer = "repetition quantity:", infoValue = repetitionQuantity)
                    Spacer(modifier = Modifier.height(16.dp))

                    InfoItem(
                        textPointer = "last iteration success mark:",
                        infoValue = lastSuccessMark
                    )
                }
            }
        }
    }

    @Composable
    private fun InfoItem(
        textPointer: String,
        infoValue: String,
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween) {
            Text(text = textPointer)
            Text(text = infoValue)
        }
    }
}