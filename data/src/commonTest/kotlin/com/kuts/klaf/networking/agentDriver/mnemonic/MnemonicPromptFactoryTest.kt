package com.kuts.klaf.networking.agentDriver.mnemonic

import kotlin.test.Test
import kotlin.test.assertContains
import kotlin.test.assertFalse

class MnemonicPromptFactoryTest {

    @Test
    fun `explicit anchor comment overrides the used-anchor exclusion`() {
        val comment = "Use the Russian word сова as the association"
        val prompt = MnemonicPromptFactory.buildTextPromptContent(
            word = "savage",
            comment = comment,
            excludedSoundAnchors = listOf("сова"),
        )

        assertContains(
            charSequence = prompt,
            other = "Highest-priority caller comment for this request:\n\"$comment\"",
        )
        assertContains(
            charSequence = prompt,
            other = "Already used primary Russian sound anchors for this word:\n\"сова\"",
        )

        assertContains(
            charSequence = prompt,
            other = "reuse that same anchor in every generation",
        )
        assertContains(
            charSequence = prompt,
            other = "Return exactly one candidate using that anchor",
        )
        assertContains(
            charSequence = prompt,
            other = "unless the highest-priority caller comment explicitly requests one of them",
        )
    }

    @Test
    fun `missing comment keeps the used-anchor novelty rule`() {
        val prompt = MnemonicPromptFactory.buildTextPromptContent(
            word = "savage",
            comment = null,
            excludedSoundAnchors = listOf("сова"),
        )

        assertFalse(
            actual = prompt.contains("Highest-priority caller comment for this request:"),
        )
        assertContains(
            charSequence = prompt,
            other = "Already used primary Russian sound anchors for this word:\n\"сова\"",
        )
        assertContains(
            charSequence = prompt,
            other = "Do not reuse any of those anchors in the new result unless",
        )
    }
}
