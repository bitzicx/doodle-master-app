package com.bitzicx.doodlemaster

import android.R
import android.crypto.hpke.Sender
import androidx.compose.ui.geometry.Offset
import com.google.gson.annotations.SerializedName
import androidx.compose.ui.graphics.Color
import com.google.gson.JsonObject
import org.json.JSONArray
import org.json.JSONObject



data class DrawnPath(
    val points: List<Offset>,
    val color: Color = Color.Black,
    val strokeWidth: Float = 10f
)

object EventType{
    const val DRAW = "DRAW"
    const val CHAT = "CHAT"
    const val STATE_UPDATE = "STATE_UPDATE"
}


data class GameEvent(
    val type: String,
    @SerializedName("sender_id") val senderId: String = "",
    val payload: JsonObject
)


data class Chat(
    val senderId: String,
    val message: String
)




data class Player(
    val id: String,
    val username: String,
    val score: Int
)



data class BaseRoom(
    val id: String,
    val type: String,
    val host: String,
    val status: String,
    val players: Map<String, Player>,

    @SerializedName("current_turn")
    val currentTurn: String,

    @SerializedName("current_word")
    val currentWord : String,


    @SerializedName("word_mask")
    val wordMask : String,

    val round: Int,

    @SerializedName("started_at")
    val startedAt: Long,

    @SerializedName("guessed_by")
    val guessedBy : Map<String, Boolean>,

    val scores : Map<String, Int>,

    @SerializedName("turn_order")
    val turnOrder : List<String>,

    val usernames : Map<String, String>


)
data class TurnStartPayload(
    @SerializedName("drawer_id")   val drawerId: String,
    @SerializedName("drawer_name") val drawerName: String,
    @SerializedName("word")        val word: String,
    @SerializedName("word_mask")   val wordMask: String,
    @SerializedName("duration")    val duration: Int
)

// matches TurnEndPayload in Go
data class TurnEndPayload(
    @SerializedName("word")       val word: String,
    @SerializedName("scores")     val scores: Map<String, Int>,
    @SerializedName("guessed_by") val guessedBy: Map<String, Boolean>
)




// convert one stroke to JSON string and send
fun DrawnPath.toJsonObject(): JsonObject {
    val obj = JsonObject()

    val pointsArray = com.google.gson.JsonArray()
    points.forEach { offset ->
        val point = JsonObject()
        point.addProperty("x", offset.x)
        point.addProperty("y", offset.y)
        pointsArray.add(point)
    }

    obj.add("points", pointsArray)
    obj.addProperty("color", color.toHex())
    obj.addProperty("strokeWidth", strokeWidth)

    return obj
}

fun String.toJsonObject(): JsonObject{
    val obj = JsonObject()
    obj.addProperty("message", this)
    return obj
}

fun Color.toHex(): String {
    return "#%02X%02X%02X%02X".format(
        (alpha * 255).toInt(),
        (red * 255).toInt(),
        (green * 255).toInt(),
        (blue * 255).toInt()
    )
}

data class TimerPayload(
    @SerializedName("remaining") val remaining: Int
)


enum class ConnectionState{
    CONNECTING,
    CONNECTED,
    DISCONNECTED,
    RECONNECTING
}




