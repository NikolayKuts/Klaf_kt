package com.example.klaf.data.common

import com.example.domain.entities.Deck
import io.mockk.every
import io.mockk.mockk
import org.junit.Assert.assertEquals
import org.junit.Test

class DateAssistantTest {

    @Test
    fun `return 0 if repetition quantity less then 5`() {
        repeat(5) { indext ->
            val deck = mockk<Deck>() {
                every { repetitionQuantity } returns indext
                every { lastRepetitionIterationDuration } returns 30000
                every { scheduledDateInterval } returns 5452342345L
                every { existenceDayQuantity } returns 33
            }
            val result = deck.getNewInterval(currentIterationDuration = 1000)

            assertEquals(0L, result)
        }
    }
}