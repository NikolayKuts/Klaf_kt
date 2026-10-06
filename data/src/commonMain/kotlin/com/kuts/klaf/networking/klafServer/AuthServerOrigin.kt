package com.kuts.klaf.networking.klafServer

private val HTTPS_AUTH_ORIGIN = Regex("https://[A-Za-z0-9.-]+(?::[0-9]{1,5})?")
private val LOOPBACK_AUTH_ORIGIN = Regex("http://(?:localhost|127\\.0\\.0\\.1)(?::[0-9]{1,5})?")

/** Auth endpoints use an origin only: no userinfo, paths, queries or remote plaintext HTTP. */
internal fun requireSafeAuthOrigin(origin: String) {
    require(HTTPS_AUTH_ORIGIN.matches(origin) || LOOPBACK_AUTH_ORIGIN.matches(origin)) {
        "Authentication requires a canonical HTTPS origin or same-host HTTP loopback"
    }
}
