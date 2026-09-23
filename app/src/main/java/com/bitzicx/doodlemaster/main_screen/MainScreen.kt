package com.bitzicx.doodlemaster.main_screen

import android.util.Log
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Diamond
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.MeetingRoom
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.QuestionMark
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.bitzicx.doodlemaster.R


@Composable
fun MainScreen(
    viewModel: MainScreenViewModel = hiltViewModel(),
    onNavigateToProfile: () -> Unit,
    onNavigateToLeaderboard: () -> Unit,
    onNavigateToSettings: () -> Unit,
    onNavigateToHowTo: () -> Unit,
    onNavigateToGameScreen: () -> Unit
) {

    LaunchedEffect(Unit) {
        viewModel.roomCreatedEvent.collect { newRoomId ->
            if (newRoomId != null) {
                viewModel.joinRoom(newRoomId)
                onNavigateToGameScreen()
            } else {
                Log.d("MainScreen", "Failed to create room")
            }
        }
    }



    val username by viewModel.username.collectAsStateWithLifecycle(initialValue = null)
    val playerId = viewModel.playerId

    var showJoinDialog by remember { mutableStateOf(false) }
    var showSearchDialog by remember {mutableStateOf(false)}
    var roomid: String by remember { mutableStateOf("") }

    LaunchedEffect(Unit) {

    }

    Box(modifier = Modifier.fillMaxSize()) {
        Image(
            painter = painterResource(id = R.drawable.mainscreenpostertemp),
            contentDescription = "main poster",
            contentScale = ContentScale.Crop
        )
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            contentAlignment = Alignment.BottomCenter
        ){

            Column(modifier = Modifier.padding(10.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center) {

                Text(roomid,
                    color = Color.Black)
                Spacer(modifier = Modifier.height(10.dp))

                PillMenuButton(
                    text = "Play Online",
                    containerColor = Color.Blue,
                    icon = Icons.Default.Bolt

                ) {
                    showSearchDialog = true
                    viewModel.quickJoinRoom()
                }

                PillMenuButton(
                    text = "Create Room",
                    containerColor = Color.Green,
                    icon = Icons.Default.MeetingRoom

                ) {
                    viewModel.createRoom()

                }

                PillMenuButton(
                    text = "Join Room",
                    containerColor = Color.Cyan,
                    icon = Icons.Default.Key

                ) {
                    showJoinDialog = true
                }

                Row(modifier = Modifier.padding(10.dp)) {

                    // for profile
                    CircleIconButton(
                        text = "Profile",
                        icon = Icons.Default.Person
                    ) {
                        onNavigateToProfile()
                    }

                    // for leaderboard
                    CircleIconButton(
                        text = "LeaderBoard",
                        icon = Icons.Default.Diamond
                    ) {
                        onNavigateToLeaderboard()

                    }

                    //for settings
                    CircleIconButton(
                        text = "Settings",
                        icon = Icons.Default.Settings
                    ) {
                        onNavigateToSettings()

                    }


                    // how to play
                    CircleIconButton(
                        text = "How To",
                        icon = Icons.Default.QuestionMark
                    ) {
                        onNavigateToHowTo()
                    }

                    // for practice
//                    CircleIconButton(
//                        text = "Profile",
//                        icon = Icons.Default.Hardware
//                    ) {
//
//                    }
                }

            }

            if (showJoinDialog) {
                JoinRoomDialog(
                    onDismiss = { showJoinDialog = false },
                    onJoin = { roomCode ->
                        viewModel.joinRoom(roomCode)
                        onNavigateToGameScreen()
                        showJoinDialog = false
                    }
                )
            }

            if(showSearchDialog){
                SearchingRoomDialog(
                    onDismiss = {showSearchDialog = false},
                    onJoin = {roomCode ->
                        viewModel.joinRoom(roomCode)
                        showJoinDialog = false

                    }
                )
            }

        }

    }
}

@Composable
fun JoinRoomDialog(
    onDismiss:() -> Unit,
    onJoin:(String) -> Unit
){
    var roomCode by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(text = "Join Room") },
        text = {
            OutlinedTextField(
                value = roomCode,
                onValueChange = {
                    roomCode = it.uppercase().take(5)
                },
                label = { Text("Enter 5-Letter Code") },
                singleLine = true
            )
        },
        confirmButton = {
            Button(
                onClick = { onJoin(roomCode) },
                enabled = roomCode.length == 5
            ) {
                Text("Join")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}


@Composable
fun SearchingRoomDialog(
    onDismiss: () -> Unit,
    onJoin: (String) -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(text = "Searching Available Room")
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                CircularProgressIndicator(
                    strokeWidth = 4.dp
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = onDismiss
            ) {
                Text(text = "Cancel")
            }
        }
    )
}