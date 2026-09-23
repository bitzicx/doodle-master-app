package com.bitzicx.doodlemaster.classic_game_screen

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Send
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.bitzicx.doodlemaster.Chat
import com.bitzicx.doodlemaster.DrawnPath
import com.bitzicx.doodlemaster.Player
import com.bitzicx.doodlemaster.VectorAvatar

@Composable
fun OtherPlayerTurnScreen(
    playersList: List<Player>,
    scores: Map<String, Int>,
    curentTurnPlayer: String,
    remainingTime: Int,
    paths: List<DrawnPath>,
    messages: List<Chat>,
    wordMask: String,
    onSendMessage: (String) -> Unit

) {
    var guessWord by remember { mutableStateOf("") }
    val listState = rememberLazyListState()


    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) {
            listState.animateScrollToItem(messages.lastIndex)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            // this pushes the content upward
            .imePadding()
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {

        // Top Row — canvas + player list
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .weight(0.55f),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Left: player list + timer
            Column(
                modifier = Modifier
                    .weight(0.25f)
                    .fillMaxHeight(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // drawing player avatar
                Box(
                    modifier = Modifier
                        .size(52.dp)
                        .clip(CircleShape)
                        .border(2.dp, MaterialTheme.colorScheme.primary, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    VectorAvatar(curentTurnPlayer)
                }

                // timer
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(36.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                        .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(8.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = remainingTime.toString(),
                        color = MaterialTheme.colorScheme.primary,
                        style = MaterialTheme.typography.labelLarge
                    )
                }


                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .fillMaxHeight()
                        .clip(RoundedCornerShape(8.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                        .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(8.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    // players list
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .fillMaxHeight()
                            .clip(RoundedCornerShape(8.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant)
                            .border(
                                1.dp,
                                MaterialTheme.colorScheme.outline,
                                RoundedCornerShape(8.dp)
                            )
                            .padding(6.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(playersList) { player ->
                            Row( modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 10.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(40.dp)
                                        .clip(CircleShape)
                                ) {
                                    VectorAvatar(player.id)
                                }
                                Text(scores[player.id]?.toString() ?: "0")

                            }

                        }
                    }
                }

            }

            // Right: canvas
            Box(
                modifier = Modifier
                    .weight(0.75f)
                    .fillMaxHeight()
                    .clip(RoundedCornerShape(12.dp))
                    .border(2.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(12.dp))
            ) {
                Canvas(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.White)
                ) {
                    paths.forEach { drawn ->
                        val path = Path().apply {
                            drawn.points.forEachIndexed { i, offset ->
                                val x = offset.x * size.width
                                val y = offset.y * size.height
                                if (i == 0) moveTo(x, y) else lineTo(x, y)
                            }
                        }
                        drawPath(
                            path = path,
                            color = drawn.color,
                            style = Stroke(
                                width = drawn.strokeWidth,
                                cap = StrokeCap.Round,
                                join = StrokeJoin.Round
                            )
                        )
                    }
                }
            }
        }

        // Chat messages
        LazyColumn(
            state = listState,
            modifier = Modifier
                .fillMaxWidth()
                .weight(0.45f)
                .clip(RoundedCornerShape(12.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant)
                .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(12.dp))
                .padding(8.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            items(messages) { message ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    ChatField(message)
                }
            }
        }

        // Guess input
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(28.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant)
                .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(28.dp))
                .padding(horizontal = 4.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            BasicTextField(
                value = guessWord,
                onValueChange = { guessWord = it },
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 16.dp, vertical = 10.dp),
                textStyle = TextStyle(
                    color = MaterialTheme.colorScheme.onSurface,
                    fontSize = 15.sp
                ),
                decorationBox = { innerTextField ->
                        Text(
                            wordMask,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            style = MaterialTheme.typography.bodyMedium
                        )

                    innerTextField()
                },
                keyboardOptions = KeyboardOptions.Default.copy(
                    imeAction = ImeAction.Send
                ),
                keyboardActions = KeyboardActions(
                    onSend = {
                        if (guessWord.isNotBlank()) {
                            onSendMessage(guessWord)
                            guessWord = ""
                        }
                    }
                )
            )

            // Send button
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(
                        if (guessWord.isNotBlank()) MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.surfaceVariant
                    )
                    .clickable {
                        if (guessWord.isNotBlank()) {
                            onSendMessage(guessWord)
                            guessWord = ""
                        }
                    },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Send,
                    contentDescription = "Send",
                    tint = if (guessWord.isNotBlank()) MaterialTheme.colorScheme.onPrimary
                    else MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}


@Composable
fun ChatField(message: Chat){

    Box(
        modifier = Modifier
            .size(32.dp)
            .clip(CircleShape)
            .border(1.dp, MaterialTheme.colorScheme.primary, CircleShape)
    ) {
        VectorAvatar(message.senderId)
    }

    val isServer = message.senderId == "SERVER"

    val backgroundColor = if (isServer) {
        MaterialTheme.colorScheme.primaryContainer
    } else {
        MaterialTheme.colorScheme.surface
    }

    val textColor = if (isServer) {
        MaterialTheme.colorScheme.onPrimaryContainer
    } else {
        MaterialTheme.colorScheme.onSurface
    }

    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .background(backgroundColor)
            .then(
                if (isServer) {
                    Modifier.border(
                        width = 1.dp,
                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f),
                        shape = RoundedCornerShape(8.dp)
                    )
                } else {
                    Modifier
                }
            )
            .padding(horizontal = 10.dp, vertical = 6.dp)
    ) {
        Text(
            text = message.message,
            color = textColor,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = if (isServer) FontWeight.SemiBold else FontWeight.Normal
        )
    }
}
