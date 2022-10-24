package com.example.klaf.common

import androidx.annotation.StringRes
import app.cash.turbine.test
import com.example.klaf.presentation.common.EventMessageSource
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import org.junit.Assert

@ExperimentalCoroutinesApi
fun TestScope.launchEventMassageIdEqualsTest(
    viewModel: EventMessageSource,
    @StringRes expectedMassageId: Int,
): Job {
    return launch {
        viewModel.testEventMassageIdEquals(expectedMassageId = expectedMassageId)
    }
}

suspend fun EventMessageSource.testEventMassageIdEquals(
    @StringRes expectedMassageId: Int,
) {
    this.eventMessage.test {
        val receivedMessageId = awaitItem().resId

        Assert.assertEquals(expectedMassageId, receivedMessageId)
    }
}