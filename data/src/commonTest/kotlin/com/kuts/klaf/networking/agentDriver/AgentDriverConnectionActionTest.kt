package com.kuts.klaf.networking.agentDriver

import kotlin.test.Test
import kotlin.test.assertEquals

class AgentDriverConnectionActionTest {

    @Test
    fun disconnectedSessionConnectsOnlyWhenAutomaticRecoveryIsInactive() {
        assertEquals(
            expected = AgentDriverConnectionAction.Connect,
            actual = AgentDriverConnectionStatus.Disconnected.nextAction(
                automaticReconnectActive = false,
            ),
        )
        assertEquals(
            expected = AgentDriverConnectionAction.AwaitRecovery,
            actual = AgentDriverConnectionStatus.Disconnected.nextAction(
                automaticReconnectActive = true,
            ),
        )
    }

    @Test
    fun resumableSessionResumesOnlyWhenAutomaticRecoveryIsInactive() {
        assertEquals(
            expected = AgentDriverConnectionAction.Resume,
            actual = AgentDriverConnectionStatus.ResumeAvailable.nextAction(
                automaticReconnectActive = false,
            ),
        )
        assertEquals(
            expected = AgentDriverConnectionAction.AwaitRecovery,
            actual = AgentDriverConnectionStatus.ResumeAvailable.nextAction(
                automaticReconnectActive = true,
            ),
        )
    }

    @Test
    fun connectedSessionUsesCurrentConnection() {
        assertEquals(
            expected = AgentDriverConnectionAction.UseCurrent,
            actual = AgentDriverConnectionStatus.Connected.nextAction(),
        )
    }

    @Test
    fun connectingSessionWaitsForCurrentAttempt() {
        assertEquals(
            expected = AgentDriverConnectionAction.AwaitRecovery,
            actual = AgentDriverConnectionStatus.Connecting.nextAction(),
        )
    }
}
