package com.example.klaf.presentation.common

import android.app.Dialog
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import androidx.annotation.LayoutRes
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.activityViewModels

open class TransparentDialogFragment(
    @LayoutRes layoutId: Int,
) : DialogFragment(layoutId), SharedViewModelHolder {

    override val sharedViewModel: BaseMainViewModel by activityViewModels<MainViewModel>()

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        return super.onCreateDialog(savedInstanceState).apply {
            window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val parentAttributes = requireActivity().window?.attributes ?: return
        val parentWidth = parentAttributes.width

        dialog?.window?.setLayout(parentWidth, ViewGroup.LayoutParams.WRAP_CONTENT)
    }
}