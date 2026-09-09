package com.kuts.klaf.networking.agentDriver

/** Supplies platform runtime state without making the shared Agent Driver session Android-aware. */
fun interface AgentDriverRuntimeDiagnostics {

    fun snapshot(): String

    companion object {

        val None = AgentDriverRuntimeDiagnostics { "runtime=unavailable" }
    }
}
