package com.kuts.klaf.di

import com.cambridge.dictionary.client.CambridgeClient
import com.cambridge.dictionary.core.Meaning
import com.cambridge.dictionary.core.PartsOfSpeech
import com.cambridge.dictionary.core.Phrase
import com.cambridge.dictionary.core.Word
import com.kuts.klaf.cardManagement.common.CambridgeMeaning
import com.kuts.klaf.cardManagement.common.CambridgePartOfSpeech
import com.kuts.klaf.cardManagement.common.CambridgePhrase
import com.kuts.klaf.cardManagement.common.CambridgeWordData
import com.kuts.klaf.cardManagement.common.ICambridgeWordDataProvider

internal class CambridgeWordDataProvider(
    private val client: CambridgeClient,
) : ICambridgeWordDataProvider {

    override suspend fun fetchWordData(word: String): CambridgeWordData? {
        return client.fetchWordData(word = word)?.toCambridgeWordData()
    }
}

private fun Word.toCambridgeWordData(): CambridgeWordData {
    return CambridgeWordData(
        text = text,
        partsOfSpeech = partsOfSpeech.map { it.toCambridgePartOfSpeech() },
    )
}

private fun PartsOfSpeech.toCambridgePartOfSpeech(): CambridgePartOfSpeech {
    return CambridgePartOfSpeech(
        text = text,
        label = label,
        ipas = ipas,
        meanings = meanings.map { it.toCambridgeMeaning() },
        phrases = phrases.map { it.toCambridgePhrase() },
    )
}

private fun Meaning.toCambridgeMeaning(): CambridgeMeaning {
    return CambridgeMeaning(
        explanation = explanation,
        translation = translation,
        examples = examples,
    )
}

private fun Phrase.toCambridgePhrase(): CambridgePhrase {
    return CambridgePhrase(
        text = text,
        translation = translation,
        examples = examples,
    )
}
