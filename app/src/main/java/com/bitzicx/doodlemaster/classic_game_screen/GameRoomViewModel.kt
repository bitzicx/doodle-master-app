package com.bitzicx.doodlemaster.classic_game_screen

import androidx.compose.runtime.mutableStateListOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bitzicx.doodlemaster.BaseRoom
import com.bitzicx.doodlemaster.Chat
import com.bitzicx.doodlemaster.DrawnPath
import com.bitzicx.doodlemaster.EventType
import com.bitzicx.doodlemaster.GameEvent
import com.bitzicx.doodlemaster.GameRepository
import com.bitzicx.doodlemaster.Player
import com.bitzicx.doodlemaster.RoomStatus
import com.bitzicx.doodlemaster.TurnEndPayload
import com.bitzicx.doodlemaster.TurnStartPayload
import com.bitzicx.doodlemaster.UserRepository
import com.bitzicx.doodlemaster.toJsonObject
import com.google.gson.JsonObject
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class GameRoomScreenViewModel @Inject constructor(
   private val userRepository: UserRepository,
    private val gameRepository: GameRepository
): ViewModel(){

    val connectionState = gameRepository.connectionState
    val turnStartPayload: StateFlow<TurnStartPayload?> = gameRepository.turnStartPayload
    val turnEndPayload: StateFlow<TurnEndPayload?> = gameRepository.turnEndPayload
    val timer: StateFlow<Int> = gameRepository.timer
    val roomState: StateFlow<BaseRoom?> = gameRepository.roomState



    fun sendEvent(event: GameEvent) {
        gameRepository.sendEvent(event)
    }
    private val _paths = mutableStateListOf<DrawnPath>()
    val paths: List<DrawnPath> = _paths

    val messages = mutableStateListOf<Chat>()

    init {
        collectPaths()
        collectMessages()
    }

    fun clearPath(){
        _paths.clear()
    }



    val roomStatus: StateFlow<RoomStatus> = gameRepository.roomState
        .map { room ->
            room?.status ?: RoomStatus.WAITING
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = RoomStatus.WAITING
        )



    val playerId: StateFlow<String> = userRepository.playerId
        .map { id ->
            id ?: ""
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = ""
        )

    val players: StateFlow<List<Player>> = gameRepository.roomState
        .map { room->
            room?.players?.values?.toList() ?: emptyList()
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    fun startGame() {
        // We send an empty JsonObject because the server just needs to see the "type"
        val event = GameEvent(
            type = EventType.START_GAME,
            payload = JsonObject()
        )
        gameRepository.sendEvent(event)
    }

    fun disconnect(){
        gameRepository.disconnect()
    }

    private fun collectMessages(){
        viewModelScope.launch {
            gameRepository.messages.collect {message ->
                if (message != null) {
                    messages.add(message)
                }

            }
        }
    }
    private fun collectPaths() {
        viewModelScope.launch {
            gameRepository.paths.collect { drawnPath ->
                _paths.add(drawnPath)   // every time repo emits, add to list
            }
        }
    }

}

