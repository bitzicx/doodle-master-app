package com.bitzicx.doodlemaster

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import com.google.gson.JsonObject
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import okhttp3.HttpUrl.Companion.toHttpUrl
import org.junit.Assert.*
import org.junit.Assume.assumeTrue
import org.junit.Test
import java.util.UUID

/** Runs against the real Go backend; CI starts the v0.1.0 server for this test. */
class BackendCompatibilityTest {
    @Test fun roomGameplayAndReconnectMatchBackend() = runBlocking {
        val url = System.getProperty("backendUrl").orEmpty()
        assumeTrue("Set -PbackendUrl to run the live backend contract test", url.isNotBlank())
        val hostState = GameSession()
        val guestState = GameSession()
        val host = NetworkManager(baseUrl = url.toHttpUrl(), onEventReceived = hostState::handle)
        val guest = NetworkManager(baseUrl = url.toHttpUrl(), onEventReceived = guestState::handle)
        val hostId = "a-${UUID.randomUUID()}"
        val guestId = "z-${UUID.randomUUID()}"
        suspend fun await(session: GameSession, predicate: (GameSessionState) -> Boolean) =
            withTimeout(10_000) { session.state.first(predicate) }
        try {
            val roomId = host.createRoom("Host & friends + 😀", hostId)!!
            assertTrue(guest.canJoin(roomId, "Guest", guestId))
            host.connect(roomId, "Host & friends + 😀", hostId)
            guest.connect(roomId, "Guest", guestId)
            val lobby = await(hostState) { it.room?.players?.size == 2 }
            assertEquals("Host & friends + 😀", lobby.room!!.players[hostId]!!.username)
            repeat(2) { host.sendEvent(GameEvent(EventType.CHAT, payload = "hello".toJsonObject())) }
            await(guestState) { it.messages.count { chat -> chat.message == "hello" } == 2 }
            guest.sendEvent(GameEvent(EventType.START_GAME, payload = JsonObject()))
            guest.sendEvent(GameEvent(EventType.CHAT, payload = "barrier".toJsonObject()))
            assertEquals(RoomStatus.WAITING, await(hostState) { it.messages.lastOrNull()?.message == "barrier" }.room!!.status)
            host.sendEvent(GameEvent(EventType.START_GAME, payload = JsonObject()))
            val hostTurn = await(hostState) { it.turnStart != null }.turnStart!!
            val guestTurn = await(guestState) { it.turnStart != null }.turnStart!!
            assertEquals(hostId, hostTurn.drawerId)
            assertTrue(hostTurn.word.isNotBlank())
            assertEquals("", guestTurn.word)
            val stroke = DrawnPath(listOf(Offset(0.2f, 0.4f), Offset(0.3f, 0.5f)), Color.Black, 10f)
            val drawEvent = GameEvent(EventType.DRAW, payload = stroke.toJsonObject())
            host.sendEvent(drawEvent) { hostState.handle(drawEvent) }
            assertEquals(1, hostState.state.value.paths.size)
            await(guestState) { it.paths.size == 1 }
            host.sendEvent(GameEvent(EventType.UNDO, payload = JsonObject()))
            await(guestState) { it.paths.isEmpty() }
            await(hostState) { it.paths.isEmpty() }
            host.sendEvent(drawEvent) { hostState.handle(drawEvent) }
            await(guestState) { it.paths.size == 1 }
            host.sendEvent(GameEvent(EventType.CLEAR, payload = JsonObject()))
            await(guestState) { it.paths.isEmpty() }
            guest.sendEvent(GameEvent(EventType.CHAT, payload = hostTurn.word.toJsonObject()))
            val result = await(guestState) { it.turnEnd != null }.turnEnd!!
            assertEquals(1, result.scores[hostId])
            assertEquals(1, result.scores[guestId])
            guest.disconnect()
            assertTrue(guest.canJoin(roomId, "Guest", guestId))
            guestState.reset()
            guest.connect(roomId, "Guest", guestId)
            val restored = await(guestState) { it.room?.players?.get(guestId)?.score == 1 }
            assertEquals(1, restored.room!!.scores[guestId])
        } finally { host.disconnect(); guest.disconnect() }
    }
}
