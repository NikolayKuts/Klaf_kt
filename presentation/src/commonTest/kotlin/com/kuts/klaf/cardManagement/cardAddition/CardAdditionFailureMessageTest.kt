package com.kuts.klaf.cardManagement.cardAddition

import com.kuts.domain.common.ReviewedDeckCardAdditionException
import com.kuts.klaf.presentation.resources.Res
import com.kuts.klaf.presentation.resources.card_addition_reviewed_deck
import com.kuts.klaf.presentation.resources.exception_adding_card
import kotlin.test.Test
import kotlin.test.assertEquals

class CardAdditionFailureMessageTest {

    @Test
    fun `reviewed deck rejection explains the review restriction`() {
        assertEquals(Res.string.card_addition_reviewed_deck, ReviewedDeckCardAdditionException().additionFailureMessage())
    }

    @Test
    fun `unrelated storage failure keeps the general message`() {
        assertEquals(Res.string.exception_adding_card, IllegalStateException("write failed").additionFailureMessage())
    }
}
