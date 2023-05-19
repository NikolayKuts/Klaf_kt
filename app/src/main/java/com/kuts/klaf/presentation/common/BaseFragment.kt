package com.kuts.klaf.presentation.common

import androidx.annotation.LayoutRes
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels

open class BaseFragment(@LayoutRes layoutId: Int) : Fragment(layoutId), SharedViewModelHolder {

    override val sharedViewModel: BaseMainViewModel by activityViewModels<MainViewModel>()
}