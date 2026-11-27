package com.bitzicx.doodlemaster.classic_game_screen

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.bitzicx.doodlemaster.ConnectionState
import com.bitzicx.doodlemaster.EventType
import com.bitzicx.doodlemaster.GameEvent
import com.bitzicx.doodlemaster.RoomStatus
import com.bitzicx.doodlemaster.toJsonObject
import com.google.gson.JsonObject

@Composable
fun GameRoomScreen(
    viewModel: GameRoomScreenViewModel = hiltViewModel(),
    onNavigateToMainScreen: () -> Unit
) {
    val game by viewModel.gameState.collectAsStateWithLifecycle()
    val connectionState by viewModel.connectionState.collectAsStateWithLifecycle()
    val connectionError by viewModel.connectionError.collectAsStateWithLifecycle()
    val playerId by viewModel.playerId.collectAsStateWithLifecycle()
    val room = game.room
    val players = room?.players?.values?.toList().orEmpty()
    val isDrawer = game.turnStart?.drawerId == playerId
    val canPlay = connectionState == ConnectionState.CONNECTED && game.turnEnd == null &&
        room?.status == RoomStatus.STARTED && !room.currentTurn.isBlank()
    var showExitDialog by remember { mutableStateOf(false) }
    var showTurnStart by remember { mutableStateOf(false) }
    var showTurnEnd by remember { mutableStateOf(false) }

    BackHandler { showExitDialog = true }
    LaunchedEffect(game.turnSequence) {
        showTurnEnd = false
        showTurnStart = game.turnStart != null
    }
    LaunchedEffect(game.turnEnd) {
        if (game.turnEnd != null) { showTurnStart = false; showTurnEnd = true }
    }

    if (showExitDialog) {
        AlertDialog(
            onDismissRequest = { showExitDialog = false },
            title = { Text("Leave game?") },
            text = { Text("Are you sure you want to leave the current game?") },
            confirmButton = {
                TextButton(onClick = { viewModel.disconnect(); onNavigateToMainScreen() }) { Text("Leave") }
            },
            dismissButton = { TextButton(onClick = { showExitDialog = false }) { Text("Stay") } }
        )
    }

    Box(Modifier.fillMaxSize()) {
        when (room?.status) {
            RoomStatus.WAITING -> WaitingRoomScreen(room, playerId, viewModel::startGame)
            RoomStatus.STARTED -> key(game.turnSequence) {
                if (isDrawer) {
                    MyTurnScreen(
                        playersList = players,
                        currentWord = game.turnStart?.word.orEmpty(),
                        paths = game.paths,
                        remainingTime = game.timer,
                        messages = game.messages,
                        enabled = canPlay,
                        returnPath = { viewModel.sendEvent(GameEvent(EventType.DRAW, payload = it.toJsonObject())) },
                        onUndo = { viewModel.sendEvent(GameEvent(EventType.UNDO, payload = JsonObject())) },
                        onClear = { viewModel.sendEvent(GameEvent(EventType.CLEAR, payload = JsonObject())) }
                    )
                } else {
                    OtherPlayerTurnScreen(
                        playersList = players,
                        scores = room.scores,
                        curentTurnPlayer = game.turnStart?.drawerId.orEmpty(),
                        remainingTime = game.timer,
                        paths = game.paths,
                        messages = game.messages,
                        wordMask = room.wordMask,
                        enabled = canPlay && room.guessedBy[playerId] != true,
                        onSendMessage = { viewModel.sendEvent(GameEvent(EventType.CHAT, payload = it.toJsonObject())) }
                    )
                }
            }
            RoomStatus.FINISHED -> GameOverBanner(
                roomState = room,
                scores = game.finalScores ?: room.scores,
                playerNames = game.playerNames,
                onBack = { viewModel.disconnect(); onNavigateToMainScreen() },
                onOk = { viewModel.disconnect(); onNavigateToMainScreen() }
            )
            null -> Text("Connecting to room...", modifier = Modifier.align(Alignment.Center))
        }
        if (room?.status == RoomStatus.STARTED) {
            ReadyBannerOverlay(if (isDrawer) "Draw" else "Guess", showTurnStart) { showTurnStart = false }
            ReadyBannerOverlay("The word was ${game.turnEnd?.word.orEmpty()}", showTurnEnd) { showTurnEnd = false }
        }
        ShowConnectionState(connectionState, Modifier.align(Alignment.TopCenter), connectionError)
    }
}

@Composable
fun ShowConnectionState(connectionState: ConnectionState, modifier: Modifier = Modifier, error: String? = null) {
    AnimatedVisibility(connectionState != ConnectionState.CONNECTED, modifier = modifier) {
        Box(
            Modifier.fillMaxWidth().background(when (connectionState) {
                ConnectionState.RECONNECTING -> Color(0xFFFFA000)
                ConnectionState.DISCONNECTED -> Color(0xFFD32F2F)
                ConnectionState.CONNECTING -> Color(0xFF1976D2)
                else -> Color.Transparent
            }).padding(8.dp), contentAlignment = Alignment.Center
        ) {
            Text(error ?: when (connectionState) {
                ConnectionState.RECONNECTING -> "Reconnecting..."
                ConnectionState.DISCONNECTED -> "No connection"
                ConnectionState.CONNECTING -> "Connecting..."
                else -> ""
            }, color = Color.White, style = MaterialTheme.typography.bodySmall)
        }
    }
}
