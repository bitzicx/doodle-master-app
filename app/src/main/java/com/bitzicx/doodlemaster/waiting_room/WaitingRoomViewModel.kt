package com.bitzicx.doodlemaster.waiting_room

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bitzicx.doodlemaster.BaseRoom
import com.bitzicx.doodlemaster.GameEvent
import com.bitzicx.doodlemaster.GameRepository
import com.bitzicx.doodlemaster.Player
import com.bitzicx.doodlemaster.UserRepository
import com.google.gson.JsonObject
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject


@HiltViewModel
class WaitingRoomViewModel @Inject constructor(
    private val gameRepository: GameRepository,
    private val userRepository: UserRepository
): ViewModel()
{



}