package com.kuts.domain.managers

/** Identifies the current device login context, not an account or authentication token. */
interface IClientSessionScope {
    val clientSessionId: String
}
