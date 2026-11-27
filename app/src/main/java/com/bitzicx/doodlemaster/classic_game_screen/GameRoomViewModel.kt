package com.bitzicx.doodlemaster.classic_game_screen

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bitzicx.doodlemaster.EventType
import com.bitzicx.doodlemaster.GameEvent
import com.bitzicx.doodlemaster.GameRepository
import com.bitzicx.doodlemaster.UserRepository
import com.google.gson.JsonObject
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

@HiltViewModel
class GameRoomScreenViewModel @Inject constructor(
    userRepository: UserRepository,
    private val gameRepository: GameRepository
) : ViewModel() {
    val gameState = gameRepository.gameState
    val connectionState = gameRepository.connectionState
    val connectionError = gameRepository.connectionError
    val playerId = userRepository.playerId.map { it.orEmpty() }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "")

    fun sendEvent(event: GameEvent) { gameRepository.sendEvent(event) }
    fun startGame() { sendEvent(GameEvent(type = EventType.START_GAME, payload = JsonObject())) }
    fun disconnect() { gameRepository.disconnect() }
}
