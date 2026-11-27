package com.bitzicx.doodlemaster

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.util.Log
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class GameRepository @Inject constructor(@param:ApplicationContext private val context: Context) {
    private val session = GameSession()
    val gameState = session.state
    private val networkManager = NetworkManager { event ->
        try { session.handle(event) }
        catch (error: RuntimeException) { Log.w("GameRepository", "Invalid ${event.type} payload", error) }
    }
    val connectionState = networkManager.connectionState
    val connectionError = networkManager.connectionError
    private val _isOnline = MutableStateFlow(false)
    val isOnline = _isOnline.asStateFlow()

    init {
        val connectivity = context.getSystemService(ConnectivityManager::class.java)
        fun refreshConnectivity() {
            val capabilities = connectivity.getNetworkCapabilities(connectivity.activeNetwork)
            _isOnline.value = capabilities?.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED) == true
        }
        refreshConnectivity()
        connectivity.registerDefaultNetworkCallback(object : ConnectivityManager.NetworkCallback() {
            override fun onAvailable(network: Network) = refreshConnectivity()
            override fun onCapabilitiesChanged(network: Network, capabilities: NetworkCapabilities) = refreshConnectivity()
            override fun onLost(network: Network) = refreshConnectivity()
        })
    }

    suspend fun createRoom(username: String, playerId: String) = networkManager.createRoom(username, playerId)
    suspend fun quickJoinRoom(playerId: String, username: String) = networkManager.quickJoinRoom(playerId, username)
    suspend fun canJoin(roomId: String, username: String, playerId: String) = networkManager.canJoin(roomId, username, playerId)

    fun connectToRoom(roomId: String, username: String, playerId: String) {
        networkManager.disconnect()
        session.reset()
        networkManager.connect(roomId, username, playerId)
    }

    fun disconnect() {
        networkManager.disconnect()
        session.reset()
    }

    fun sendEvent(event: GameEvent) = networkManager.sendEvent(event) {
        // The backend echoes undo/clear, but excludes the drawer from strokes.
        // Keep local strokes in the same session so rotation cannot erase them.
        if (event.type == EventType.DRAW) session.handle(event)
    }
}
