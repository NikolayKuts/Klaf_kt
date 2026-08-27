package com.kuts.klaf.cardManagement.common

import androidx.compose.ui.text.input.TextFieldValue
import kotlin.test.Test
import kotlin.test.assertEquals

class MnemonicCommentSpeechInputStateTest {

    @Test
    fun `clearing the association comment leaves the image comment untouched`() {
        val state = MnemonicManagementUiState(
            requestComment = TextFieldValue(text = "association text"),
            imageRequestComment = TextFieldValue(text = "image text"),
        )

        val updatedState = state.clearComment(field = MnemonicCommentField.ASSOCIATION)

        assertEquals(expected = "", actual = updatedState.requestComment.text)
        assertEquals(expected = "image text", actual = updatedState.imageRequestComment.text)
    }

    @Test
    fun `clearing the image comment leaves the association comment untouched`() {
        val state = MnemonicManagementUiState(
            requestComment = TextFieldValue(text = "association text"),
            imageRequestComment = TextFieldValue(text = "image text"),
        )

        val updatedState = state.clearComment(field = MnemonicCommentField.IMAGE)

        assertEquals(expected = "association text", actual = updatedState.requestComment.text)
        assertEquals(expected = "", actual = updatedState.imageRequestComment.text)
    }

    @Test
    fun `recognized text fills the empty association comment`() {
        val state = MnemonicManagementUiState()

        val updatedState = state.appendRecognizedText(
            field = MnemonicCommentField.ASSOCIATION,
            recognizedText = "make it funny",
        )

        assertEquals(expected = "make it funny", actual = updatedState.requestComment.text)
        assertEquals(
            expected = "make it funny".length,
            actual = updatedState.requestComment.selection.start,
        )
    }

    @Test
    fun `recognized text is appended to the association comment with a space`() {
        val state = MnemonicManagementUiState(
            requestComment = TextFieldValue(text = "make it funny"),
        )

        val updatedState = state.appendRecognizedText(
            field = MnemonicCommentField.ASSOCIATION,
            recognizedText = "and short",
        )

        assertEquals(expected = "make it funny and short", actual = updatedState.requestComment.text)
    }

    @Test
    fun `recognized text goes to the image comment when that field is the target`() {
        val state = MnemonicManagementUiState(
            requestComment = TextFieldValue(text = "association text"),
            imageRequestComment = TextFieldValue(text = "bright colors"),
        )

        val updatedState = state.appendRecognizedText(
            field = MnemonicCommentField.IMAGE,
            recognizedText = "no text on the picture",
        )

        assertEquals(expected = "association text", actual = updatedState.requestComment.text)
        assertEquals(
            expected = "bright colors no text on the picture",
            actual = updatedState.imageRequestComment.text,
        )
    }

    @Test
    fun `blank recognized text changes nothing`() {
        val state = MnemonicManagementUiState(
            requestComment = TextFieldValue(text = "make it funny"),
        )

        val updatedState = state.appendRecognizedText(
            field = MnemonicCommentField.ASSOCIATION,
            recognizedText = "   ",
        )

        assertEquals(expected = "make it funny", actual = updatedState.requestComment.text)
    }

    @Test
    fun `a busy session only marks its own field as busy`() {
        val speechInput = MnemonicSpeechInputUiState(
            isAvailable = true,
            activeField = MnemonicCommentField.ASSOCIATION,
            phase = MnemonicSpeechInputUiState.Phase.LISTENING,
        )

        assertEquals(
            expected = true,
            actual = speechInput.isBusyFor(field = MnemonicCommentField.ASSOCIATION),
        )
        assertEquals(
            expected = false,
            actual = speechInput.isBusyFor(field = MnemonicCommentField.IMAGE),
        )
        assertEquals(expected = true, actual = speechInput.isBusy)
    }
}
