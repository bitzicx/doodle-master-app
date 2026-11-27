package com.bitzicx.doodlemaster

import com.google.gson.Gson
import org.junit.Assert.*
import org.junit.Test

class GameSessionTest {
    private val gson = Gson()
    private fun event(type: String, payload: String, sender: String = "guest") =
        gson.fromJson("""{"type":"$type","sender_id":"$sender","payload":$payload}""", GameEvent::class.java)
    private val stroke = """{"points":[{"x":0.2,"y":0.4}],"color":"#FF112233","strokeWidth":10} """
    private val turn = """{"drawer_id":"host","drawer_name":"Host","word":"","word_mask":"_ _ _","duration":40}"""

    @Test fun strokesUndoAndClearStayInWireOrder() {
        val session = GameSession()
        repeat(2) { session.handle(event("draw", stroke)) }
        session.handle(event("undo", "{}"))
        assertEquals(1, session.state.value.paths.size)
        session.handle(event("clear", "{}"))
        session.handle(event("draw", stroke))
        assertEquals(1, session.state.value.paths.size)
        assertEquals(0.2f, session.state.value.paths.single().points.single().x)
    }

    @Test fun identicalChatMessagesAreBothRetained() {
        val session = GameSession()
        repeat(2) { session.handle(event("chat", """{"message":"hello"}""")) }
        assertEquals(listOf("hello", "hello"), session.state.value.messages.map { it.message })
    }

    @Test fun eachTurnResetsCanvasAndTimerEvenWithSameWordAndDrawer() {
        val session = GameSession()
        session.handle(event("turn_start", turn))
        session.handle(event("draw", stroke))
        session.handle(event("timer", """{"remaining":1}"""))
        session.handle(event("turn_end", """{"word":"cat","scores":{},"guessed_by":{}}"""))
        session.handle(event("turn_start", turn))
        assertEquals(2L, session.state.value.turnSequence)
        assertEquals(40, session.state.value.timer)
        assertNull(session.state.value.turnEnd)
        assertTrue(session.state.value.paths.isEmpty())
    }

    @Test fun finalScoresDoNotRequireTurnEndEvent() {
        val session = GameSession()
        session.handle(event("state_update", """{"id":"ABCDE","status":"started","players":{}}"""))
        session.handle(event("game_over", """{"host":4,"guest":3}"""))
        assertEquals(mapOf("host" to 4, "guest" to 3), session.state.value.finalScores)
        assertEquals(RoomStatus.FINISHED, session.state.value.room!!.status)
        assertNull(session.state.value.turnEnd)
    }

    @Test fun backendRoomOmitsSecretWordAndKeepsDepartedPlayerNames() {
        val session = GameSession()
        session.handle(event("state_update", """{"id":"ABCDE","players":{"host":{"id":"host","username":"Host","score":0}}}"""))
        session.handle(event("state_update", """{"id":"ABCDE","players":{}}"""))
        assertEquals("Host", session.state.value.playerNames["host"])
        assertTrue(session.state.value.room!!.scores.isEmpty())
        assertTrue(session.state.value.room!!.turnOrder.isEmpty())
    }

    @Test fun disconnectClearsAllPreviousRoomData() {
        val session = GameSession()
        session.handle(event("chat", """{"message":"old room"}"""))
        session.handle(event("turn_start", turn))
        session.handle(event("draw", stroke))
        session.reset()
        assertEquals(GameSessionState(), session.state.value)
    }

    @Test fun invalidDrawingCannotCorruptSession() {
        val session = GameSession()
        try { session.handle(event("draw", stroke.replace("10", "-10"))); fail("invalid width accepted") }
        catch (_: IllegalArgumentException) { }
        assertTrue(session.state.value.paths.isEmpty())
    }
}
