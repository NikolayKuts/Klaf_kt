package com.kuts.klaf.sync

import com.kuts.klaf.networking.klafServer.SyncEventChannelState
import com.kuts.klaf.networking.klafServer.SyncEventConnection
import com.kuts.klaf.networking.klafServer.SyncEventConnector
import com.kuts.klaf.networking.klafServer.SyncEventFeed
import com.kuts.klaf.server.contract.SyncEventDevice
import com.kuts.klaf.server.contract.SyncEventMessage
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout

private class FakeEventConnection : SyncEventConnection {

    private val messages = Channel<SyncEventMessage>(Channel.UNLIMITED)
    val closed = CompletableDeferred<Unit>()

    suspend fun emit(message: SyncEventMessage) {
        messages.send(message)
    }

    fun drop() {
        messages.close()
    }

    override suspend fun receive(): SyncEventMessage? = messages.receiveCatching().getOrNull()

    override suspend fun close() {
        closed.complete(Unit)
        messages.close()
    }
}

private class FakeEventConnector : SyncEventConnector {

    val requests = Channel<Pair<String, String>>(Channel.UNLIMITED)
    val connections = Channel<SyncEventConnection>(Channel.UNLIMITED)

    suspend fun nextRequest(): Pair<String, String> = withTimeout(2_000L) { requests.receive() }

    override suspend fun open(email: String, deviceId: String): SyncEventConnection {
        requests.send(email to deviceId)
        return connections.receive()
    }
}

class SyncEventFeedTest {

    @Test
    fun `guest does not create a device identity until an account is selected`() = runBlocking {
        val accounts = MutableStateFlow<String?>(null)
        val connector = FakeEventConnector()
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
        var identityCalls = 0
        val feed = SyncEventFeed(accounts, { identityCalls++; "device-a" }, connector, scope)
        try {
            feed.start()
            await { feed.state.value.channel == SyncEventChannelState.GUEST }
            assertEquals(0, identityCalls)
            assertTrue(connector.requests.tryReceive().isFailure)

            connector.connections.send(FakeEventConnection())
            accounts.value = "alice@example.test"
            assertEquals("alice@example.test" to "device-a", connector.nextRequest())
            assertEquals(1, identityCalls)
        } finally {
            feed.stop()
            scope.cancel()
        }
    }

    @Test
    fun `guest is hidden and switching accounts closes old channel without leaking state`() = runBlocking {
        val accounts = MutableStateFlow<String?>(null)
        val connector = FakeEventConnector()
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
        val feed = SyncEventFeed(accounts, "device-a", connector, scope, retryDelayMillis = 100L)
        try {
            feed.start()
            assertEquals(SyncEventChannelState.GUEST, feed.state.value.channel)
            assertTrue(connector.requests.tryReceive().isFailure)

            val alice = FakeEventConnection()
            connector.connections.send(alice)
            accounts.value = "alice@example.test"
            assertEquals("alice@example.test" to "device-a", connector.nextRequest())
            alice.emit(SyncEventMessage.State(1L, listOf(
                SyncEventDevice("device-a", "Alice phone", "ANDROID", true),
            )))
            await { feed.state.value.channel == SyncEventChannelState.CONNECTED }
            alice.emit(SyncEventMessage.RevisionChanged(2L))
            await { feed.state.value.serverRevision == 2L }

            val bob = FakeEventConnection()
            connector.connections.send(bob)
            accounts.value = "bob@example.test"
            assertEquals("bob@example.test" to "device-a", connector.nextRequest())
            withTimeout(2_000L) { alice.closed.await() }
            assertEquals("bob@example.test", feed.state.value.accountEmail)
            assertEquals(null, feed.state.value.serverRevision)
            assertEquals(emptyList(), feed.state.value.devices)
            bob.emit(SyncEventMessage.State(7L, listOf(
                SyncEventDevice("device-a", "Bob desktop", "DESKTOP", true),
            )))
            await { feed.state.value.serverRevision == 7L }
            assertEquals("Bob desktop", feed.state.value.devices.single().name)

            accounts.value = null
            withTimeout(2_000L) { bob.closed.await() }
            await { feed.state.value.channel == SyncEventChannelState.GUEST }
            assertEquals(null, feed.state.value.serverRevision)
        } finally {
            feed.stop()
            scope.cancel()
        }
    }

    @Test
    fun `dropped event channel reconnects and refreshes state without data synchronization`() = runBlocking {
        val accounts = MutableStateFlow<String?>("alice@example.test")
        val connector = FakeEventConnector()
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
        val feed = SyncEventFeed(accounts, "device-a", connector, scope, retryDelayMillis = 100L)
        try {
            val first = FakeEventConnection()
            connector.connections.send(first)
            feed.start()
            assertEquals("alice@example.test" to "device-a", connector.nextRequest())
            first.emit(SyncEventMessage.State(2L, emptyList()))
            await { feed.state.value.channel == SyncEventChannelState.CONNECTED }

            val second = FakeEventConnection()
            connector.connections.send(second)
            first.drop()
            withTimeout(2_000L) { first.closed.await() }
            await { feed.state.value.channel == SyncEventChannelState.DISCONNECTED }
            assertEquals(2L, feed.state.value.serverRevision)
            assertEquals("alice@example.test" to "device-a", connector.nextRequest())
            second.emit(SyncEventMessage.State(5L, emptyList()))
            await { feed.state.value.serverRevision == 5L }
            assertEquals(SyncEventChannelState.CONNECTED, feed.state.value.channel)
            assertTrue(connector.requests.tryReceive().isFailure)
        } finally {
            feed.stop()
            scope.cancel()
        }
    }

    @Test
    fun `connector timeout retries but stopping during connect cancels without another attempt`() = runBlocking {
        val accounts = MutableStateFlow<String?>("alice@example.test")
        val requests = Channel<Int>(Channel.UNLIMITED)
        val working = FakeEventConnection()
        var attempts = 0
        val connector = SyncEventConnector { _, _ ->
            val attempt = ++attempts
            requests.send(attempt)
            if (attempt == 1) withTimeout(30L) { delay(5_000L) }
            if (attempt >= 3) delay(5_000L)
            working
        }
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
        val feed = SyncEventFeed(accounts, "device-a", connector, scope, retryDelayMillis = 20L)
        try {
            working.emit(SyncEventMessage.State(4L, emptyList()))
            feed.start()
            assertEquals(1, withTimeout(2_000L) { requests.receive() })
            assertEquals(2, withTimeout(2_000L) { requests.receive() })
            await { feed.state.value.channel == SyncEventChannelState.CONNECTED }
            assertEquals(4L, feed.state.value.serverRevision)
            working.drop()
            assertEquals(3, withTimeout(2_000L) { requests.receive() })
            feed.stop()
            delay(80L)
            assertTrue(requests.tryReceive().isFailure)
            assertEquals(SyncEventChannelState.GUEST, feed.state.value.channel)
        } finally {
            feed.stop()
            scope.cancel()
        }
    }

    @Test
    fun `silent connection times out and retries instead of staying connected`() = runBlocking {
        val accounts = MutableStateFlow<String?>("alice@example.test")
        val connector = FakeEventConnector()
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
        val feed = SyncEventFeed(accounts, "device-a", connector, scope,
            retryDelayMillis = 20L, initialStateTimeoutMillis = 50L)
        try {
            val silent = FakeEventConnection()
            val working = FakeEventConnection()
            connector.connections.send(silent)
            connector.connections.send(working)
            feed.start()
            assertEquals("alice@example.test" to "device-a", connector.nextRequest())
            withTimeout(2_000L) { silent.closed.await() }
            assertEquals("alice@example.test" to "device-a", connector.nextRequest())
            working.emit(SyncEventMessage.State(3L, emptyList()))
            await { feed.state.value.channel == SyncEventChannelState.CONNECTED }
            assertEquals(3L, feed.state.value.serverRevision)
        } finally {
            feed.stop()
            scope.cancel()
        }
    }

    private suspend fun await(condition: () -> Boolean) {
        withTimeout(2_000L) {
            while (!condition()) delay(10L)
        }
    }
}
