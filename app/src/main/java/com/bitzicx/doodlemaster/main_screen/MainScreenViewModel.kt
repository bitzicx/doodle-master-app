package com.bitzicx.doodlemaster.main_screen

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bitzicx.doodlemaster.GameRepository
import com.bitzicx.doodlemaster.UserRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class MainScreenViewModel @Inject constructor(
    private val userRepository: UserRepository,
    private val gameRepository: GameRepository
) : ViewModel() {


    val username: StateFlow<String> = userRepository.username
        .map { it ?: "Guest" }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = "Loading..."
        )

    var playerId: String = ""
        private set

    private val _roomCreatedEvent = MutableSharedFlow<String?>()
    val roomCreatedEvent: SharedFlow<String?> = _roomCreatedEvent.asSharedFlow()

    init {
        viewModelScope.launch {
            playerId = userRepository.playerId.firstOrNull() ?: ""
        }
    }

    fun createRoom() {
        viewModelScope.launch {
            val validPlayerId = if (playerId.isNotEmpty()) {
                playerId
            } else {
                userRepository.playerId.firstOrNull() ?: ""
            }

            if (validPlayerId.isNotEmpty()) {
                val roomId = gameRepository.createRoom(username.value, validPlayerId)
                Log.d("MainScreenViewModel", "Room created: $roomId for user: ${username.value}")
                _roomCreatedEvent.emit(roomId)
            } else {
                Log.e("MainScreenViewModel", "Failed: PlayerId is empty")
                _roomCreatedEvent.emit(null)
            }
        }
    }

    fun quickJoinRoom(){
        viewModelScope.launch {
            val validPlayerId = playerId.ifEmpty {
                userRepository.playerId.firstOrNull() ?: ""
            }

            if (validPlayerId.isNotEmpty()) {
                val roomId = gameRepository.quickJoinRoom(validPlayerId, username.value)
                Log.d("MainScreenViewModel", "Room found: $roomId for user: ${username.value}")
                _roomCreatedEvent.emit(roomId)
            } else {
                Log.e("MainScreenViewModel", "Failed: PlayerId is empty")
                _roomCreatedEvent.emit(null)
            }

        }
    }

    fun joinRoom(roomId: String) {
        viewModelScope.launch {
            val validPlayerId = playerId.ifEmpty { userRepository.playerId.firstOrNull() ?: "" }
            gameRepository.connectToRoom(roomId, username.value, validPlayerId)
        }
    }
}