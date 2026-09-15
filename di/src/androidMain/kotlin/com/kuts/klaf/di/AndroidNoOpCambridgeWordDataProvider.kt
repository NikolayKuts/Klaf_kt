package com.kuts.klaf.di

import com.kuts.klaf.cardManagement.common.CambridgeWordData
import com.kuts.klaf.cardManagement.common.ICambridgeWordDataProvider

/**
 * Stands in for the Cambridge dictionary on Android while its client library is out of action.
 *
 * `com.cambridge.dictionary:client` is compiled against Ktor 2, whose
 * `io.ktor.client.plugins.contentnegotiation.ContentNegotiation` class Ktor 3 no longer has -- it
 * became a top-level property. Merely having the library on the classpath crashed the app the
 * moment anything touched it, and Ktor 3 is not optional here: the KlafServer client SDK requires
 * it.
 *
 * Behaves like the desktop and iOS stands-in, which have always returned nothing: word data is
 * simply unavailable rather than wrong. Restore the real provider once the library is rebuilt
 * against Ktor 3.
 */
class AndroidNoOpCambridgeWordDataProvider : ICambridgeWordDataProvider {

    override suspend fun fetchWordData(word: String): CambridgeWordData? = null
}
