package com.bitzicx.doodlemaster.classic_game_screen

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.bitzicx.doodlemaster.BaseRoom
import com.bitzicx.doodlemaster.VectorAvatar
import kotlin.collections.emptyList


@Composable
fun WaitingRoomScreen(
    roomState: BaseRoom?,
    playerId: String,
    onGameStart: () -> Unit
){

    val playersList = roomState?.players?.values?.toList() ?: emptyList()

    if(roomState != null){
        Column(modifier = Modifier.fillMaxSize().padding(20.dp),
            verticalArrangement = Arrangement.Center) {

            Text(
                text = "Waiting for Participants to join",
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(10.dp))

            Text(
                text = "Current Room Status: ${roomState?.status.toString()}",
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold
            )


            Spacer(modifier = Modifier.height(10.dp))

            Text(
                text = "Room ID: ${roomState?.id}",
                fontSize = 20.sp
            )

            Spacer(modifier = Modifier.height(10.dp))

            Text(
                text = "Game Room Hosted By ${roomState?.players[roomState?.host]?.username ?: ""}"
            )

            Spacer(modifier = Modifier.height(20.dp))

            LazyVerticalGrid(
                columns = GridCells.Fixed(3),
                modifier = Modifier.fillMaxWidth().weight(0.7f), // Takes up the middle space
                verticalArrangement = Arrangement.spacedBy(16.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                items(playersList) { player ->
                    // Individual Player Profile
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {

                        // Avatar Circle
                        Box(
                            modifier = Modifier
                                .size(90.dp)
                                .clip(CircleShape)
                                .background(Color(0xFFE0E0E0)),
                            contentAlignment = Alignment.Center
                        ) {
                            VectorAvatar(
                                seed = player.id
                            )
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        // Player Name
                        Text(
                            text = player.username,
                            fontSize = 19.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))




            if (roomState?.host.toString() == playerId) {
                // Only the host sees the button
                Box(
                    modifier = Modifier.fillMaxWidth(),
                    contentAlignment = Alignment.Center,

                ){
                    Button(onClick = {
                        onGameStart()
                    }
                    ) {
                        Text("Start Game")
                    }
                }

            } else {
                Text(
                    text = "Waiting for host to start the game...",
                    modifier = Modifier.padding(top = 16.dp)
                )
            }

        }



    }else{
        Text("Loading Room ...")
    }



}