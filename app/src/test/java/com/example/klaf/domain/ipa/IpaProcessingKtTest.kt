package com.example.klaf.domain.ipa

import com.example.klaf.presentation.common.log
import org.junit.Test
import org.junit.Assert.*

internal class IpaProcessingKtTest {

    @Test
    fun convertToUncompletedIpa1() {
        val letterInfo = listOf(
            LetterInfo(letter = "s", isChecked = false),
            LetterInfo(letter = "o", isChecked = true),
            LetterInfo(letter = "m", isChecked = false),
            LetterInfo(letter = "e", isChecked = false),
            LetterInfo(letter = "", isChecked = false),
            LetterInfo(letter = "t", isChecked = true),
            LetterInfo(letter = "h", isChecked = true),
            LetterInfo(letter = "i", isChecked = false),
            LetterInfo(letter = "n", isChecked = false),
            LetterInfo(letter = "g", isChecked = false),
        )

        val expected = "o = \nth = "
        val actual = letterInfo.convertToUncompletedIpa()
        assertEquals(expected, actual)
    }

    @Test
    fun convertToUncompletedIpa2() {
        val letterInfo = listOf(
            LetterInfo(letter = "s", isChecked = true),
            LetterInfo(letter = "o", isChecked = true),
            LetterInfo(letter = "m", isChecked = true),
            LetterInfo(letter = "e", isChecked = true),
            LetterInfo(letter = " ", isChecked = true),
            LetterInfo(letter = "t", isChecked = true),
            LetterInfo(letter = "h", isChecked = true),
            LetterInfo(letter = "i", isChecked = true),
            LetterInfo(letter = "n", isChecked = true),
            LetterInfo(letter = "g", isChecked = true),
        )

        val expected = "some thing = "
        val actual = letterInfo.convertToUncompletedIpa()

        assertEquals(expected, actual)
    }

    @Test
    fun convertToUncompletedIpa3() {
        val letterInfo = listOf(
            LetterInfo(letter = "s", isChecked = false),
            LetterInfo(letter = "o", isChecked = false),
            LetterInfo(letter = "m", isChecked = true),
            LetterInfo(letter = "e", isChecked = true),
            LetterInfo(letter = " ", isChecked = true),
            LetterInfo(letter = "t", isChecked = true),
            LetterInfo(letter = "h", isChecked = true),
            LetterInfo(letter = "i", isChecked = true),
            LetterInfo(letter = "n", isChecked = false),
            LetterInfo(letter = "g", isChecked = false),
        )

        val expected = "me thi = "
        val actual = letterInfo.convertToUncompletedIpa()
        assertEquals(expected, actual)
    }

    @Test
    fun convertToUncompletedIpa4() {
        val letterInfo = listOf(
            LetterInfo(letter = "s", isChecked = true),
            LetterInfo(letter = "o", isChecked = false),
            LetterInfo(letter = "m", isChecked = true),
            LetterInfo(letter = "e", isChecked = true),
            LetterInfo(letter = " ", isChecked = true),
            LetterInfo(letter = "t", isChecked = true),
            LetterInfo(letter = "h", isChecked = true),
            LetterInfo(letter = "i", isChecked = true),
            LetterInfo(letter = "n", isChecked = false),
            LetterInfo(letter = "g", isChecked = true),
        )

        val expected = "s = \nme thi = \ng = "
        val actual = letterInfo.convertToUncompletedIpa()
        assertEquals(expected, actual)
    }

    @Test
    fun convertToUncompletedIpa5() {
        val letterInfo = listOf(
            LetterInfo(letter = "s", isChecked = true),
            LetterInfo(letter = "o", isChecked = false),
            LetterInfo(letter = "m", isChecked = true),
            LetterInfo(letter = "e", isChecked = false),
            LetterInfo(letter = " ", isChecked = false),
            LetterInfo(letter = "t", isChecked = false),
            LetterInfo(letter = "h", isChecked = true),
            LetterInfo(letter = "i", isChecked = false),
            LetterInfo(letter = "n", isChecked = true),
            LetterInfo(letter = "g", isChecked = false),
        )

        val expected = "s = \nm = \nh = \nn = "
        val actual = letterInfo.convertToUncompletedIpa()
        assertEquals(expected, actual)
    }
}