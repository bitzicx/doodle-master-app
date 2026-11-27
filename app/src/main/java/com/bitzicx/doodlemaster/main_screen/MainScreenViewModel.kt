package com.bitzicx.doodlemaster.main_screen

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bitzicx.doodlemaster.GameRepository
import com.bitzicx.doodlemaster.UserRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.Locale
import javax.inject.Inject

@HiltViewModel
class MainScreenViewModel @Inject constructor(
    private val userRepository: UserRepository,
    private val gameRepository: GameRepository
) : ViewModel() {
    val username = userRepository.username.map { it?.takeIf(String::isNotBlank) ?: "Guest" }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "Guest")
    private val navigation = Channel<String>(Channel.BUFFERED)
    val roomCreatedEvent = navigation.receiveAsFlow()
    private val _isJoining = MutableStateFlow(false)
    val isJoining = _isJoining.asStateFlow()
    private val _joinError = MutableStateFlow<String?>(null)
    val joinError = _joinError.asStateFlow()
    private var joinJob: Job? = null
    private var requestVersion = 0L

    fun createRoom() = requestRoom { name, id -> gameRepository.createRoom(name, id) }
    fun quickJoinRoom() = requestRoom { name, id -> gameRepository.quickJoinRoom(id, name) }
    fun joinRoom(code: String) = requestRoom { _, _ ->
        code.trim().uppercase(Locale.ROOT).takeIf { it.matches(Regex("[A-Z0-9]{5}")) }
    }

    private fun requestRoom(findRoom: suspend (String, String) -> String?) {
        if (_isJoining.value) return
        val version = ++requestVersion
        _isJoining.value = true
        _joinError.value = null
        joinJob = viewModelScope.launch {
            try {
                val id = userRepository.ensurePlayerIdExists()
                val name = userRepository.getUsernameOnce()?.trim()?.takeIf(String::isNotBlank) ?: "Guest"
                val roomId = findRoom(name, id)
                if (roomId == null || !gameRepository.canJoin(roomId, name, id)) {
                    _joinError.value = "Unable to join. Check the room code, room availability, and your connection."
                    return@launch
                }
                if (version != requestVersion) return@launch
                gameRepository.connectToRoom(roomId, name, id)
                navigation.send(roomId)
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (_: Exception) {
                _joinError.value = "Could not join a room. Please try again."
            } finally {
                if (version == requestVersion) _isJoining.value = false
            }
        }
    }

    fun cancelJoin() {
        requestVersion++
        joinJob?.cancel()
        joinJob = null
        _isJoining.value = false
    }
}
