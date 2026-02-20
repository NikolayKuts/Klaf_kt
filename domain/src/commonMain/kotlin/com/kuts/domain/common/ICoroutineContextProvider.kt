package com.kuts.domain.common

import kotlin.coroutines.CoroutineContext

interface ICoroutineContextProvider {

    val io: CoroutineContext
}
