package com.kuts.klaf.server.contract

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

fun requestClientSessionId(requestId: String): String = requestId.substringBeforeLast('-', "")
    .substringAfterLast('-', "")

@Serializable
@SerialName("clientSession.end")
data class ClientSessionEndRequest(
    override val requestId: String,
    val clientSessionId: String,
) : KlafServerClientMessage
