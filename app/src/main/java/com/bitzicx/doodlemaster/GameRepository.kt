package com.bitzicx.doodlemaster

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.util.Log
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.jvm.java


@Singleton
class GameRepository @Inject constructor(
    @ApplicationContext private val context: Context
){

    private val gson = Gson() // to parse the incoming events
    private val _isOnline = MutableStateFlow(true)
    val isOnline: StateFlow<Boolean> = _isOnline.asStateFlow()
    private val repositoryScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private val _gameEvents = MutableSharedFlow<GameEvent>()
    val gameEvents: SharedFlow<GameEvent> = _gameEvents.asSharedFlow()

    private val _paths = MutableSharedFlow<DrawnPath>()
    val paths: SharedFlow<DrawnPath> = _paths.asSharedFlow()


    private val _messages = MutableStateFlow<Chat?>(null)

    val messages: StateFlow<Chat?> = _messages.asStateFlow()


    private val _roomState = MutableStateFlow<BaseRoom?>(null)
    val roomState: StateFlow<BaseRoom?> = _roomState.asStateFlow()

    private val _turnStartPayload = MutableStateFlow<TurnStartPayload?>(null)
    val turnStartPayload: StateFlow<TurnStartPayload?> = _turnStartPayload.asStateFlow()

    private val _turnEndPayload = MutableStateFlow<TurnEndPayload?>(null)
    val turnEndPayload: StateFlow<TurnEndPayload?> = _turnEndPayload.asStateFlow()



    private val networkManager = NetworkManager{event ->
        repositoryScope.launch {
            handleIncomingEvent(event)
        }
    }

    var connectionState: StateFlow<ConnectionState> = networkManager.connectionState

    private val _timer = MutableStateFlow(40)
    val timer: StateFlow<Int> = _timer.asStateFlow()

    // 3. The Handler Function
    private fun handleIncomingEvent(event: GameEvent) {
        try {
            // Check the envelope type
            when (event.type) {

                "STATE_UPDATE" -> {
                    // Tell Gson to convert the raw JsonElement into a RoomStatePayload
                    val state = gson.fromJson(event.payload, BaseRoom::class.java)

                    // Update our StateFlow. Compose will instantly redraw the screen!
                    _roomState.value = state
                    Log.d("GameRepository", "Lobby updated! Players currently in room: ${state.players.size}")
                }
                "DRAW" -> {
                    val payload = event.payload

                    val pointsArray = payload.getAsJsonArray("points")
                    val points = (0 until pointsArray.size()).map { i ->
                        val p = pointsArray[i].asJsonObject
                        Offset(
                            x = p.get("x").asFloat,
                            y = p.get("y").asFloat
                        )
                    }

                    val color = Color(android.graphics.Color.parseColor(payload.get("color").asString))
                    val strokeWidth = payload.get("strokeWidth").asFloat

                    val drawnPath = DrawnPath(
                        points = points,
                        color = color,
                        strokeWidth = strokeWidth
                    )
                    repositoryScope.launch {
                        _paths.emit(drawnPath)
                    }
                }
                "CHAT" -> {
                    val chat = Chat(
                        message = event.payload.get("message").asString,
                        senderId = event.senderId
                    )
                    repositoryScope.launch {
                        _messages.emit(chat)
                    }
                }
                "TURN_START" ->{
                    // Tell Gson to convert the raw JsonElement into a RoomStatePayload
                    val payload = gson.fromJson(event.payload, TurnStartPayload::class.java)
                    _turnEndPayload.value = null
                    // Update our StateFlow. Compose will instantly redraw the screen!
                    _turnStartPayload.value = payload
                }

                "TURN_END" ->{
                    // Tell Gson to convert the raw JsonElement into a RoomStatePayload
                    val state = gson.fromJson(event.payload, TurnEndPayload::class.java)

                    // Update our StateFlow. Compose will instantly redraw the screen!
                    _turnEndPayload.value = state
                }

                "GAME_OVER" -> {
                    // Tell Gson to convert the raw JsonElement into a RoomStatePayload
//                    val scores = gson.fromJson(event.payload, object : TypeToken<Map<String, Int>>() {}.type)
                }

                "TIMER" -> {
                    val payload = gson.fromJson(event.payload, TimerPayload::class.java)
                    _timer.value = payload.remaining
                }

                else -> {
                    Log.w("GameRepository", "Received unknown event type: ${event.type}")
                }

            }
        } catch (e: Exception) {
            Log.e("GameRepository", "Failed to parse payload for type ${event.type}", e)
        }
    }

    init{
        startNetworkMonitor()
    }

    suspend fun createRoom(username: String, playerId: String): String?{
        return networkManager.createRoom(username, playerId)
    }

    fun connectToRoom(roomId: String, username: String, playerId: String){
        networkManager.connect(roomId, username, playerId)
    }
    fun disconnect(){
        networkManager.disconnect()
    }

    fun sendEvent(event: GameEvent){
        networkManager.sendEvent(event)
    }

    suspend fun quickJoinRoom(playerId: String, username: String): String?{
        return networkManager.quickJoinRoom(playerId, username)
    }

    private fun startNetworkMonitor(){

        val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager

        connectivityManager.registerDefaultNetworkCallback(object : ConnectivityManager.NetworkCallback() {
            override fun onAvailable(network: Network) {
                _isOnline.value = true
            }

            override fun onLost(network: Network) {
                _isOnline.value = false
            }
        })
    }

}