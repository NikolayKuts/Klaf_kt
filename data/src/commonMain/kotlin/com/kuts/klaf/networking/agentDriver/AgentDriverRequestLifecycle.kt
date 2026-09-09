package com.kuts.klaf.networking.agentDriver

/** Request/background state guarded by [AgentDriverSession]'s connection mutex. */
internal class AgentDriverRequestLifecycle {

    private var activeRequestCount = 0
    private var isApplicationInBackground = false

    fun onRequestStarted() {
        activeRequestCount++
    }

    fun onRequestFinished() {
        check(activeRequestCount > 0) { "An Agent Driver request finished without starting." }
        activeRequestCount--
    }

    fun onApplicationBackgrounded() {
        isApplicationInBackground = true
    }

    fun onApplicationForegrounded(): Boolean {
        isApplicationInBackground = false
        return activeRequestCount == 0
    }

    fun shouldDisconnectIdleSession(isSwitchedOn: Boolean): Boolean {
        return isSwitchedOn && isApplicationInBackground && activeRequestCount == 0
    }

    fun diagnosticDescription(): String {
        return "activeRequests=$activeRequestCount, appInBackground=$isApplicationInBackground"
    }
}
