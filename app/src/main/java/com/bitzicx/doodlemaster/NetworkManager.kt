package com.bitzicx.doodlemaster

import com.google.gson.Gson
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.HttpUrl
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import java.io.IOException

class NetworkManager(
    private val client: OkHttpClient = OkHttpClient(),
    private val baseUrl: HttpUrl = "${if (BuildConfig.USE_TLS) "https" else "http"}://${BuildConfig.SERVER_HOST}/".toHttpUrl(),
    private val scope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.IO),
    private val retryDelayMillis: (Int) -> Long = { attempt -> minOf(1_000L shl attempt.coerceAtMost(5), 30_000L) },
    private val onEventReceived: (GameEvent) -> Unit
) {
    private data class Target(val roomId: String, val username: String, val playerId: String)
    private val gson = Gson()
    private var webSocket: WebSocket? = null
    private var target: Target? = null
    private var generation = 0L
    private var reconnectJob: Job? = null
    private var reconnectAttempts = 0
    private val _connectionState = MutableStateFlow(ConnectionState.DISCONNECTED)
    val connectionState = _connectionState.asStateFlow()
    private val _connectionError = MutableStateFlow<String?>(null)
    val connectionError = _connectionError.asStateFlow()

    suspend fun createRoom(username: String, playerId: String): String? = requestRoom("create", username, playerId)
    suspend fun quickJoinRoom(playerId: String, username: String): String? = requestRoom("quickjoin", username, playerId)

    private suspend fun requestRoom(endpoint: String, username: String, playerId: String): String? = withContext(Dispatchers.IO) {
        try {
            client.newCall(roomRequest(endpoint, mapOf("username" to username, "playerId" to playerId))).execute().use { response ->
                if (!response.isSuccessful) return@withContext null
                response.body.string().trim().takeIf { it.matches(Regex("[A-Z0-9]{5}")) }
            }
        } catch (_: IOException) { null }
    }

    suspend fun canJoin(roomId: String, username: String, playerId: String): Boolean =
        joinStatus(Target(roomId, username, playerId)) in 200..299

    private fun roomRequest(endpoint: String, fields: Map<String, String>): Request = Request.Builder()
        .url(baseUrl.newBuilder().addPathSegment(endpoint).build())
        .post(gson.toJson(fields).toRequestBody("application/json; charset=utf-8".toMediaType()))
        .build()

    private suspend fun joinStatus(destination: Target): Int? = withContext(Dispatchers.IO) {
        try {
            client.newCall(roomRequest("join", mapOf("roomId" to destination.roomId,
                "username" to destination.username, "playerId" to destination.playerId))).execute().use { it.code }
        } catch (_: IOException) { null }
    }

    @Synchronized
    fun connect(roomId: String, username: String, playerId: String) {
        disconnect()
        target = Target(roomId, username, playerId)
        reconnectAttempts = 0
        _connectionError.value = null
        openSocket(ConnectionState.CONNECTING)
    }

    // All socket callbacks and session changes share this monitor. A callback
    // from an old connection must not change state or deliver old room events.
    private fun openSocket(state: ConnectionState) {
        val destination = target ?: return
        val epoch = ++generation
        reconnectJob = null
        _connectionState.value = state
        val request = Request.Builder().url(baseUrl.newBuilder().addPathSegment("ws")
            .addQueryParameter("roomId", destination.roomId)
            .addQueryParameter("username", destination.username)
            .addQueryParameter("playerId", destination.playerId).build()).build()
        webSocket = client.newWebSocket(request, object : WebSocketListener() {
            override fun onOpen(webSocket: WebSocket, response: Response) = synchronized(this@NetworkManager) {
                if (epoch != generation) { webSocket.cancel(); return@synchronized }
                reconnectAttempts = 0
                _connectionState.value = ConnectionState.CONNECTED
            }

            override fun onMessage(webSocket: WebSocket, text: String) = synchronized(this@NetworkManager) {
                if (epoch != generation) return@synchronized
                val event = try { gson.fromJson(text, GameEvent::class.java) } catch (_: RuntimeException) { null }
                if (event?.type != null) onEventReceived(event)
            }

            override fun onClosing(webSocket: WebSocket, code: Int, reason: String) {
                webSocket.close(code, reason)
            }

            override fun onClosed(webSocket: WebSocket, code: Int, reason: String) = synchronized(this@NetworkManager) {
                if (epoch != generation) return@synchronized
                this@NetworkManager.webSocket = null
                if (code == 1000 || code == 1008) stopReconnecting() else scheduleReconnect()
            }

            override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) = synchronized(this@NetworkManager) {
                if (epoch != generation) return@synchronized
                this@NetworkManager.webSocket = null
                if (response?.code in listOf(400, 404, 409)) stopReconnecting() else scheduleReconnect()
            }
        })
    }

    private fun stopReconnecting() {
        target = null
        reconnectJob?.cancel()
        reconnectJob = null
        _connectionState.value = ConnectionState.DISCONNECTED
        _connectionError.value = "This room is full or no longer available. Leave and join another room."
    }

    private fun scheduleReconnect() {
        val destination = target ?: return
        if (reconnectJob?.isActive == true) return
        val epoch = generation
        val wait = retryDelayMillis(reconnectAttempts++)
        _connectionState.value = ConnectionState.RECONNECTING
        reconnectJob = scope.launch {
            delay(wait)
            val status = joinStatus(destination)
            synchronized(this@NetworkManager) {
                if (epoch != generation || target != destination) return@synchronized
                if (status in listOf(400, 404, 409)) stopReconnecting()
                else openSocket(ConnectionState.RECONNECTING)
            }
        }
    }

    @Synchronized
    fun sendEvent(event: GameEvent, onSent: () -> Unit = {}): Boolean {
        val sent = webSocket?.send(gson.toJson(event)) == true
        if (sent) onSent()
        return sent
    }

    @Synchronized
    fun disconnect() {
        generation++
        target = null
        reconnectJob?.cancel()
        reconnectJob = null
        val old = webSocket
        webSocket = null
        old?.close(1000, "User disconnected")
        old?.cancel()
        _connectionState.value = ConnectionState.DISCONNECTED
        _connectionError.value = null
    }
}
