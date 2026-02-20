package com.kuts.klaf.common

import com.kuts.domain.common.ICoroutineContextProvider
import kotlinx.coroutines.Dispatchers
import kotlin.coroutines.CoroutineContext

class CoroutineContextProvider : ICoroutineContextProvider {

    override val io: CoroutineContext = Dispatchers.IO
}
