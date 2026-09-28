package com.kuts.klaf.server.contract

/** Stable across devices; the MVP account email is immutable and unique. */
fun accountInterimDeckSyncId(email: String): String {
    val account = email.trim().lowercase()
    require(account.isNotEmpty()) { "Account email is required" }
    return "account-interim:$account"
}
