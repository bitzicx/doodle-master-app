package com.bitzicx.doodlemaster

import android.util.Log
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import com.google.gson.Gson
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response
import okio.IOException
import kotlin.math.pow
import kotlin.time.Duration.Companion.milliseconds


class NetworkManager(private val onEventRaceived: (GameEvent) -> Unit): WebSocketListener(){

    private var webSocket: WebSocket? = null
    private val gson = Gson()

    private val client = OkHttpClient()
    private val serverUrl = "doodlemaster-backend.onrender.com"


    private val _connectionState = MutableStateFlow(ConnectionState.DISCONNECTED)
    val connectionState : StateFlow<ConnectionState> = _connectionState

    var reconnectAttempts = 0

    private var lastRoomId = ""
    private var lastPlayerId = ""
    private var lastUsername = ""
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    // creating room,
    // the server will automatically general the room ID , 4-6 char string
    suspend fun createRoom(username: String, playerId: String): String?{
        return withContext(Dispatchers.IO){

            val requestUrl = "https://$serverUrl/create"


            val jsonBody = gson.toJson(mapOf("username" to username, "playerId" to playerId))
            val requestBody = jsonBody.toRequestBody("application/json; charset=utf-8".toMediaType())

            val request = Request.Builder()
                .url(requestUrl)
                .post(requestBody)
                .build()

            try {
                val response = client.newCall(request).execute()
                if(response.isSuccessful) {

                    val generatedRoomId = response.body.string()

                    Log.d("Network Manger", "server generated rom ID: $generatedRoomId")
                    return@withContext generatedRoomId
                }
                else {
                    Log.e("network Manger", "Failed to create room. Code : ${response.code})")
                    return@withContext null
                }
            }catch (e: IOException){
                Log.e("Network Manger", "HTTP error createing room", e)
                return@withContext null
            }
        }
    }


    // open the connection
// Add playerId to the parameters
    fun connect(roomId: String, username: String, playerId: String){

        lastRoomId = roomId
        lastPlayerId = playerId
        lastUsername = username
        // Attach &playerId=$playerId to the end of the URL
        val url = "wss://$serverUrl/ws?roomId=$roomId&username=$username&playerId=$playerId"

        val request = Request.Builder().url(url).build()

        webSocket = client.newWebSocket(request, this)


        _connectionState.value = ConnectionState.CONNECTING

        Log.d("Network Manager", "connecting to $url ...")
    }

    // send data to server
    fun sendEvent(event: GameEvent){
        val jsonString = gson.toJson(event)
        webSocket?.send(jsonString)
    }

    // receive data from go server
    override fun onMessage(webSocket: WebSocket, text: String) {
        Log.d("Network Manager", "Raw json received $text")
        try {
            val event = gson.fromJson(text,GameEvent::class.java)
            onEventRaceived(event)
        }catch(e: Exception){
            Log.e("Network Manager", "Failed to parse JsON $text", e)
        }
    }

    override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
        _connectionState.value = ConnectionState.DISCONNECTED

        Log.e("Network Manager", "Weboscket Error:", t)

        reconnectWithDelay()

    }

    fun reconnectWithDelay(){
        if(lastRoomId.isEmpty()) return

        val delaySeconds = minOf(2.0.pow(reconnectAttempts).toLong(), 30L)
        reconnectAttempts++
        Log.d("NetworkMANAGER,", "Reconnecting in ${delaySeconds}s (attemps $reconnectAttempts)")
        _connectionState.value = ConnectionState.CONNECTING

        scope.launch{
            delay((delaySeconds*1000).milliseconds)
            val checkRoomExists = checkRoomExists(lastRoomId)
            connect(lastRoomId, lastUsername, lastPlayerId )
        }
    }

    private suspend fun checkRoomExists(roomId: String): Boolean {
        return withContext(Dispatchers.IO) {
            try {
                val jsonBody = gson.toJson(mapOf(
                    "roomId" to roomId,
                    "username" to lastUsername,
                    "playerId" to lastPlayerId
                ))
                val request = Request.Builder()
                    .url("https://$serverUrl/join")
                    .post(jsonBody.toRequestBody("application/json".toMediaType()))
                    .build()
                val response = client.newCall(request).execute()
                response.isSuccessful
            } catch (e: Exception) {
                false
            }
        }
    }

    override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
        Log.e("Network Managerr", "Connection closed: $reason")
        _connectionState.value = ConnectionState.DISCONNECTED
        if(code != 1000){
            reconnectWithDelay()
        }
    }

    override fun onOpen(webSocket: WebSocket, response: Response) {
        _connectionState.value = ConnectionState.CONNECTED
        reconnectAttempts = 0
        Log.d("NEtworkManager", "Connected")
    }

    fun disconnect() {
        try {
            // 1000 indicates a normal, intentional closure
            val closed = webSocket?.close(1000, "User disconnected") ?: false
            if (!closed) {
                webSocket?.cancel()
            }
        } catch (e: Exception) {
            Log.e("Network Manager", "Error while closing WebSocket", e)
            webSocket?.cancel()
        } finally {
            webSocket = null
        }
    }

    suspend fun quickJoinRoom(playerId: String, username: String):String? {
        return withContext(Dispatchers.IO){

            val requestUrl = "http://$serverUrl/quickjoin"


            val jsonBody = gson.toJson(mapOf("username" to username, "playerId" to playerId))
            val requestBody = jsonBody.toRequestBody("application/json; charset=utf-8".toMediaType())

            val request = Request.Builder()
                .url(requestUrl)
                .post(requestBody)
                .build()

            try {
                val response = client.newCall(request).execute()
                if(response.isSuccessful) {

                    val generatedRoomId = response.body.string()

                    Log.d("Network Manger", "server generated rom ID: $generatedRoomId")
                    return@withContext generatedRoomId
                }
                else {
                    Log.e("network Manger", "Failed to create room. Code : ${response.code})")
                    return@withContext null
                }
            }catch (e: IOException){
                Log.e("Network Manger", "HTTP error createing room", e)
                return@withContext null
            }
        }

    }


}