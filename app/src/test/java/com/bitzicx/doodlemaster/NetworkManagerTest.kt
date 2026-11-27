package com.bitzicx.doodlemaster

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

class NetworkManagerTest {
    private lateinit var server: MockWebServer
    private lateinit var network: NetworkManager
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    @Before fun setUp() {
        server = MockWebServer()
        server.start()
        network = NetworkManager(baseUrl = server.url("/"), scope = scope, retryDelayMillis = { 20 }) { }
    }
    @After fun tearDown() { network.disconnect(); scope.cancel(); server.shutdown() }

    private fun awaitState(state: ConnectionState) = runBlocking {
        withTimeout(5_000) { network.connectionState.first { it == state } }
    }

    @Test fun usernameAndIdsAreEncodedAsQueryParameters() {
        server.enqueue(MockResponse().withWebSocketUpgrade(object : WebSocketListener() {}))
        network.connect("ABCDE", "A & B + 😀", "id&other=bad")
        awaitState(ConnectionState.CONNECTED)
        val request = server.takeRequest(3, TimeUnit.SECONDS)!!
        assertEquals("A & B + 😀", request.requestUrl!!.queryParameter("username"))
        assertEquals("id&other=bad", request.requestUrl!!.queryParameter("playerId"))
        assertNull(request.requestUrl!!.queryParameter("other"))
    }

    @Test fun missingRoomDoesNotRetryForever() {
        server.enqueue(MockResponse().setResponseCode(404))
        network.connect("GONE1", "Guest", "guest")
        runBlocking { withTimeout(5_000) { network.connectionError.first { it != null } } }
        assertEquals(ConnectionState.DISCONNECTED, network.connectionState.value)
        server.takeRequest(3, TimeUnit.SECONDS)
        assertNull(server.takeRequest(150, TimeUnit.MILLISECONDS))
        assertEquals(1, server.requestCount)
    }

    @Test fun queuedRetryIsCancelledOnDisconnect() {
        val closed = CountDownLatch(1)
        network = NetworkManager(baseUrl = server.url("/"), scope = scope, retryDelayMillis = { 200 }) { }
        server.enqueue(MockResponse().withWebSocketUpgrade(object : WebSocketListener() {
            override fun onOpen(webSocket: WebSocket, response: okhttp3.Response) { webSocket.close(1001, "restart") }
            override fun onClosed(webSocket: WebSocket, code: Int, reason: String) { closed.countDown() }
        }))
        network.connect("ABCDE", "Guest", "guest")
        awaitState(ConnectionState.RECONNECTING)
        network.disconnect()
        assertTrue(closed.await(3, TimeUnit.SECONDS))
        server.takeRequest(3, TimeUnit.SECONDS)
        assertNull(server.takeRequest(350, TimeUnit.MILLISECONDS))
        assertEquals(ConnectionState.DISCONNECTED, network.connectionState.value)
    }

    @Test fun retryChecksRoomBeforeOpeningAnotherSocket() {
        server.enqueue(MockResponse().withWebSocketUpgrade(object : WebSocketListener() {
            override fun onOpen(webSocket: WebSocket, response: okhttp3.Response) { webSocket.close(1001, "restart") }
        }))
        server.enqueue(MockResponse().setResponseCode(404))
        network.connect("ABCDE", "Guest", "guest")
        runBlocking { withTimeout(5_000) { network.connectionError.first { it != null } } }
        assertEquals("/ws", server.takeRequest(3, TimeUnit.SECONDS)!!.requestUrl!!.encodedPath)
        assertEquals("/join", server.takeRequest(3, TimeUnit.SECONDS)!!.requestUrl!!.encodedPath)
        assertEquals(2, server.requestCount)
        assertEquals(ConnectionState.DISCONNECTED, network.connectionState.value)
    }

    @Test fun policyRejectionDoesNotReconnect() {
        server.enqueue(MockResponse().withWebSocketUpgrade(object : WebSocketListener() {
            override fun onOpen(webSocket: WebSocket, response: okhttp3.Response) { webSocket.close(1008, "full") }
        }))
        network.connect("ABCDE", "Guest", "guest")
        runBlocking { withTimeout(5_000) { network.connectionError.first { it != null } } }
        server.takeRequest(3, TimeUnit.SECONDS)
        assertNull(server.takeRequest(150, TimeUnit.MILLISECONDS))
        assertEquals(ConnectionState.DISCONNECTED, network.connectionState.value)
    }

    @Test fun httpRequestsUseBackendFieldNamesAndHandleFailure() = runBlocking {
        server.enqueue(MockResponse().setBody("ABCDE"))
        server.enqueue(MockResponse().setResponseCode(500).setBody("failed"))
        assertEquals("ABCDE", network.createRoom("Guest", "guest"))
        assertNull(network.quickJoinRoom("guest", "Guest"))
        val request = server.takeRequest(3, TimeUnit.SECONDS)!!
        assertEquals("/create", request.path)
        val json = com.google.gson.JsonParser.parseString(request.body.readUtf8()).asJsonObject
        assertEquals("guest", json["playerId"].asString)
        assertEquals("Guest", json["username"].asString)
    }
}
