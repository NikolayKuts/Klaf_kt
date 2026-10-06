package com.kuts.klaf.networking.klafServer

import com.kuts.domain.managers.AccountFailure
import com.kuts.domain.managers.AccountOperationException
import io.ktor.client.HttpClient
import io.ktor.client.request.get

/** A normal signed GET obtains the one-use DPoP nonce for the WebSocket upgrade. */
internal class KlafServerWebSocketAuthorizer(
    private val serverOrigin: String,
    private val httpClient: HttpClient,
    private val signer: KlafAuthenticatedRequestSigner,
) {
    suspend fun authorizationHeaders(email: String): AccessProofHeaders {
        val url = "${serverOrigin.trimEnd('/')}/ws"
        val initialProof = signer.headers(email, "GET", url, null)
        val challenge = httpClient.get(url) {
            headers.append("Authorization", initialProof.authorization)
            headers.append("DPoP", initialProof.dpop)
        }
        val nonce = challenge.headers["DPoP-Nonce"]
            ?.takeIf { it.length in 16..128 && it.all { character -> character.code in 33..126 } }
        if (nonce == null && challenge.status.value in 401..403) {
            throw AccountOperationException(AccountFailure.SIGN_IN_REQUIRED,
                "Klaf Server AI authorization is no longer valid.")
        }
        check(challenge.status.value == 401 && nonce != null) {
            "Klaf Server did not provide a valid WebSocket proof challenge."
        }
        return signer.headers(email, "GET", url, nonce)
    }
}
