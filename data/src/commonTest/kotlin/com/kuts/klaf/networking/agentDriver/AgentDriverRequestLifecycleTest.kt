package com.kuts.klaf.networking.agentDriver

import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class AgentDriverRequestLifecycleTest {

    @Test
    fun idleSessionIsDisconnectedOnlyWhileApplicationIsInBackground() {
        val lifecycle = AgentDriverRequestLifecycle()

        lifecycle.onApplicationBackgrounded()
        assertTrue(lifecycle.shouldDisconnectIdleSession(isSwitchedOn = true))

        val shouldRefreshConnection = lifecycle.onApplicationForegrounded()
        assertTrue(shouldRefreshConnection)
        assertFalse(lifecycle.shouldDisconnectIdleSession(isSwitchedOn = true))
    }

    @Test
    fun activeRequestKeepsSessionConnectedUntilRequestFinishes() {
        val lifecycle = AgentDriverRequestLifecycle()
        lifecycle.onRequestStarted()

        lifecycle.onApplicationBackgrounded()
        assertFalse(lifecycle.shouldDisconnectIdleSession(isSwitchedOn = true))
        assertFalse(lifecycle.onApplicationForegrounded())

        lifecycle.onApplicationBackgrounded()
        lifecycle.onRequestFinished()
        assertTrue(lifecycle.shouldDisconnectIdleSession(isSwitchedOn = true))
    }

    @Test
    fun switchedOffAssistantDoesNotNeedBackgroundDisconnect() {
        val lifecycle = AgentDriverRequestLifecycle()

        lifecycle.onApplicationBackgrounded()

        assertFalse(lifecycle.shouldDisconnectIdleSession(isSwitchedOn = false))
    }
}
