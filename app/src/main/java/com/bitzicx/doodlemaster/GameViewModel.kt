package com.bitzicx.doodlemaster

import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel

class GameViewModel: ViewModel() {

    var currentScreen = mutableStateOf("LOBBY")
    var chatMessages = mutableListOf<String>()


    private val networkManager = NetworkManager{incomingEvent ->
        handleServerEvent(incomingEvent)
    }

    fun connectToGame(roomId: String, username: String, playerId: String){
        networkManager.connect(roomId, username, playerId)
    }

//    fun sendChatMessage(text: String){
//        val event = GameEvent(type = EventType.CHAT, payload = text)
//        networkManager.sendEvent(event)
//    }

    private fun handleServerEvent(event: GameEvent){
        when(event.type){
            EventType.STATE_UPDATE ->{
                currentScreen.value = "GAME"
            }

            EventType.CHAT ->{
                chatMessages.add(event.payload.toString())
            }

            EventType.DRAW ->{
                // later for later
            }

        }
    }
}




