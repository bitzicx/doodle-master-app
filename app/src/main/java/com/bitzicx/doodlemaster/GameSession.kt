package com.bitzicx.doodlemaster

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

// Immutable room data keeps socket ordering independent of Compose collectors.
data class GameSessionState(
    val room: BaseRoom? = null,
    val turnStart: TurnStartPayload? = null,
    val turnEnd: TurnEndPayload? = null,
    val timer: Int = 40,
    val turnSequence: Long = 0,
    val paths: List<DrawnPath> = emptyList(),
    val messages: List<Chat> = emptyList(),
    val finalScores: Map<String, Int>? = null,
    val playerNames: Map<String, String> = emptyMap()
)

class GameSession {
    private val gson = Gson()
    private val _state = MutableStateFlow(GameSessionState())
    val state = _state.asStateFlow()

    fun reset() { _state.value = GameSessionState() }

    fun handle(event: GameEvent) {
        _state.update { state ->
            when (event.type) {
                EventType.UPDATE -> {
                    val room = gson.fromJson(event.payload, BaseRoom::class.java)
                    state.copy(room = room, playerNames = state.playerNames + room.players.mapValues { it.value.username })
                }
                EventType.DRAW -> {
                    val payload = event.payload
                    val points = payload.getAsJsonArray("points").map {
                        Offset(it.asJsonObject["x"].asFloat, it.asJsonObject["y"].asFloat)
                    }
                    require(points.all { it.x.isFinite() && it.y.isFinite() })
                    val hex = payload["color"].asString.removePrefix("#")
                    require(hex.length == 6 || hex.length == 8)
                    val argb = if (hex.length == 6) hex.toLong(16) or 0xFF000000 else hex.toLong(16)
                    val width = payload["strokeWidth"].asFloat
                    require(width.isFinite() && width > 0)
                    state.copy(paths = state.paths + DrawnPath(points, Color(argb.toInt()), width))
                }
                EventType.CHAT -> state.copy(messages = state.messages + Chat(event.senderId, event.payload["message"].asString))
                EventType.TURN_START -> {
                    val turn = gson.fromJson(event.payload, TurnStartPayload::class.java)
                    state.copy(turnStart = turn, turnEnd = null, timer = turn.duration.coerceAtLeast(0),
                        paths = emptyList(), turnSequence = state.turnSequence + 1)
                }
                EventType.TURN_END -> state.copy(turnEnd = gson.fromJson(event.payload, TurnEndPayload::class.java))
                EventType.GAME_OVER -> state.copy(
                    finalScores = gson.fromJson(event.payload, object : TypeToken<Map<String, Int>>() {}.type),
                    room = state.room?.copy(status = RoomStatus.FINISHED)
                )
                EventType.TIMER -> state.copy(timer = event.payload["remaining"].asInt.coerceAtLeast(0))
                EventType.UNDO -> state.copy(paths = state.paths.dropLast(1))
                EventType.CLEAR -> state.copy(paths = emptyList())
                else -> state
            }
        }
    }
}
