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
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
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

@Composable
fun GameRoomScreen(
    viewModel: GameRoomScreenViewModel = hiltViewModel(),
    onNavigateToMainScreen: () -> Unit
){
    val connectionState by viewModel.connectionState.collectAsState()

    val roomState by viewModel.roomState.collectAsStateWithLifecycle()
    val playersList = roomState?.players?.values?.toList() ?: emptyList()
    val playerId by viewModel.playerId.collectAsStateWithLifecycle()
    val scores = roomState?.scores ?: emptyMap()
    val paths = viewModel.paths
    val messages = viewModel.messages
    val turnStart by viewModel.turnStartPayload.collectAsState()
    val turnEnd by viewModel.turnEndPayload.collectAsState()
    val remainingTime by viewModel.timer.collectAsState()

    var showExitDialog by remember { mutableStateOf(false) }


    var showTurnStartBanner by remember { mutableStateOf(false) }
    var showTurnEndBanner by remember { mutableStateOf(false) }

    var gameOver by remember { mutableStateOf(false) }

    // handling back pressing
    BackHandler {
        showExitDialog = true
    }

    LaunchedEffect(roomState?.status) {
        if (roomState?.status == RoomStatus.FINISHED) {
            gameOver = true
        }
    }


// Trigger when the turn starts
    LaunchedEffect(turnStart) {
        viewModel.clearPath()

        if (roomState?.status == RoomStatus.STARTED && turnStart != null) {
            showTurnStartBanner = true
        }
    }

    LaunchedEffect(turnEnd) {
        if (roomState?.status == RoomStatus.STARTED && turnEnd != null) {
            showTurnEndBanner = true
        }
    }

    if (showExitDialog) {
        AlertDialog(
            onDismissRequest = { showExitDialog = false },
            title = { Text("Leave game?") },
            text = { Text("Are you sure you want to leave the current game?") },
            confirmButton = {
                TextButton(
                    onClick = {
                        showExitDialog = false
                        viewModel.disconnect()
                        onNavigateToMainScreen()
                    }
                ) {
                    Text("Leave")
                }
            },
            dismissButton = {
                TextButton(onClick = { showExitDialog = false }) {
                    Text("Stay")
                }
            }
        )
    }



    Box(modifier = Modifier.fillMaxSize()) {


        if (roomState?.status == RoomStatus.STARTED) {
            val isDrawer = roomState?.currentTurn == playerId
            if (isDrawer) {
                MyTurnScreen(
                    playersList,
                    turnStart?.word.orEmpty(),
                    remainingTime,
                    messages
                ) { path ->
                    val event = GameEvent(
                        type = EventType.DRAW,
                        payload = path.toJsonObject()
                    )
                    viewModel.sendEvent(event)
                }
            } else {
                OtherPlayerTurnScreen(
                    playersList,
                    scores,
                    roomState?.currentTurn.orEmpty(),
                    remainingTime,
                    paths,
                    messages,
                    roomState?.wordMask ?: ""

                ) { message ->
                    val event = GameEvent(
                        type = EventType.CHAT,
                        payload = message.toJsonObject()
                    )
                    viewModel.sendEvent(event)
                }
            }
        } else if(roomState?.status == RoomStatus.WAITING) {
            WaitingRoomScreen(
                roomState,
                playerId
            ){
                viewModel.startGame()
            }
        }

        ReadyBannerOverlay(
            text = if (roomState?.currentTurn == playerId)
                "Draw"
            else "Guess ",
            visible = showTurnStartBanner,
            onFinished = { showTurnStartBanner = false }
        )

        ReadyBannerOverlay(
            text = if (turnEnd?.guessedBy?.size == playersList.size - 1) "Everyone Guessed 🎉"
            else if(remainingTime <= 1) "Time's Up"
            else "Player Left"
            ,
            visible = showTurnEndBanner,
            onFinished = { showTurnEndBanner = false },
        )

        if (gameOver) {
            if (roomState != null && turnEnd != null) {
                Box(
                    modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center
                ) {
                    GameOverBanner(
                        roomState = roomState!!,
                        turnEndPayload = turnEnd!!,
                        onBack = {
                            gameOver = false
                            viewModel.disconnect()
                            onNavigateToMainScreen()
                        },
                        onOk = {
                            gameOver = false
                            viewModel.disconnect()
                            onNavigateToMainScreen()
                        }
                    )
                }
            }
        }

        ShowConnectionState(
            connectionState = connectionState,
            modifier = Modifier.align(Alignment.TopCenter)
        )

    }


}

@Composable
fun ShowConnectionState(
    connectionState: ConnectionState,
    modifier: Modifier = Modifier
) {
    AnimatedVisibility(
        visible = connectionState != ConnectionState.CONNECTED,
        modifier = modifier
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    when (connectionState) {
                        ConnectionState.RECONNECTING -> Color(0xFFFFA000)
                        ConnectionState.DISCONNECTED -> Color(0xFFD32F2F)
                        ConnectionState.CONNECTING   -> Color(0xFF1976D2)
                        else -> Color.Transparent
                    }
                )
                .padding(8.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = when (connectionState) {
                    ConnectionState.RECONNECTING -> "Reconnecting..."
                    ConnectionState.DISCONNECTED -> "No connection"
                    ConnectionState.CONNECTING   -> "Connecting..."
                    else -> ""
                },
                color = Color.White,
                style = MaterialTheme.typography.bodySmall
            )
        }
    }
}