    package com.bitzicx.doodlemaster.classic_game_screen

    import androidx.compose.foundation.Canvas
    import androidx.compose.foundation.background
    import androidx.compose.foundation.border
    import androidx.compose.foundation.gestures.detectDragGestures
    import androidx.compose.foundation.layout.Arrangement
    import androidx.compose.foundation.layout.Box
    import androidx.compose.foundation.layout.Column
    import androidx.compose.foundation.layout.Row
    import androidx.compose.foundation.layout.fillMaxSize
    import androidx.compose.foundation.layout.fillMaxWidth
    import androidx.compose.foundation.layout.height
    import androidx.compose.foundation.layout.padding
    import androidx.compose.foundation.layout.size
    import androidx.compose.foundation.layout.width
    import androidx.compose.foundation.lazy.LazyColumn
    import androidx.compose.foundation.lazy.LazyRow
    import androidx.compose.foundation.lazy.items
    import androidx.compose.foundation.lazy.rememberLazyListState
    import androidx.compose.foundation.shape.CircleShape
    import androidx.compose.foundation.shape.RoundedCornerShape
    import androidx.compose.material.icons.Icons
    import androidx.compose.material.icons.filled.CommentsDisabled
    import androidx.compose.material.icons.filled.Pinch
    import androidx.compose.material3.Icon
    import androidx.compose.material3.IconButton
    import androidx.compose.material3.MaterialTheme
    import androidx.compose.material3.Text
    import androidx.compose.runtime.Composable
    import androidx.compose.runtime.LaunchedEffect
    import androidx.compose.runtime.getValue
    import androidx.compose.runtime.mutableFloatStateOf
    import androidx.compose.runtime.mutableStateListOf
    import androidx.compose.runtime.mutableStateOf
    import androidx.compose.runtime.remember
    import androidx.compose.runtime.setValue
    import androidx.compose.runtime.snapshots.SnapshotStateList
    import androidx.compose.ui.Alignment
    import androidx.compose.ui.Modifier
    import androidx.compose.ui.draw.clip
    import androidx.compose.ui.geometry.Offset
    import androidx.compose.ui.graphics.Color
    import androidx.compose.ui.graphics.Path
    import androidx.compose.ui.graphics.StrokeCap
    import androidx.compose.ui.graphics.StrokeJoin
    import androidx.compose.ui.graphics.drawscope.Stroke
    import androidx.compose.ui.input.pointer.pointerInput
    import androidx.compose.ui.unit.dp
    import com.bitzicx.doodlemaster.DrawnPath
    import com.bitzicx.doodlemaster.Player
    import com.bitzicx.doodlemaster.VectorAvatar
    import com.bitzicx.doodlemaster.Chat

    @Composable
    fun MyTurnScreen(
        playersList: List<Player>,
        currentWord: String,
        remainingTime: Int,
        messages: SnapshotStateList<Chat>,
        returnPath: (DrawnPath) -> Unit
    ) {

        val paths = remember { mutableStateListOf<DrawnPath>() }
        var currentPath by remember { mutableStateOf<Path?>(null) }
        var currentColor by remember { mutableStateOf(Color.Black) }
        var currentStrokeWidth by remember { mutableFloatStateOf(10f) }
        val listState = rememberLazyListState()

        var currentPoints = remember { mutableStateListOf<Offset>() }

        LaunchedEffect(messages.size) {
            if (messages.isNotEmpty()) {
                listState.animateScrollToItem(messages.lastIndex)
            }
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {


            // Chat messages
            LazyColumn(
                state = listState,
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(0.22f)
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

            //Timer + word to draw
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(15.dp, Alignment.CenterHorizontally),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Target Word to Guess
                Box(
                    modifier = Modifier
                        .width(200.dp)
                        .height(40.dp)
                        .border(2.dp, Color.White, RoundedCornerShape(8.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = currentWord,
                        color = Color.White
                    )
                }

                // Remaining time
                Box(
                    modifier = Modifier
                        .height(40.dp) // Matched to 40.dp so both boxes center-align symmetrically
                        .width(45.dp)
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
            }



            // Drawing Canvas
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .border(2.dp, Color.White, RoundedCornerShape(8.dp)),
                contentAlignment = Alignment.Center
            ) {
                        // Drawing Area

                        Canvas(
                            modifier = Modifier
                                .fillMaxSize()
                                .clip(RoundedCornerShape(8.dp))
                                .background(Color.White)
                                .pointerInput(Unit) {
                                    detectDragGestures(
                                        onDragStart = { offset ->
                                            currentPoints.clear()
                                            currentPoints.add(offset)
                                            val newPath = Path().apply {
                                                moveTo(offset.x, offset.y)
                                            }
                                            currentPath = newPath
                                        },

                                        onDrag = { change, _ ->
                                            change.consume()
                                            currentPoints.add(change.position)
                                            currentPath?.lineTo(change.position.x, change.position.y)
                                            // Re-assign to trigger recomposition during drag
                                            currentPath =
                                                currentPath?.let { Path().apply { addPath(it) } }

//                                            val partialPath = DrawnPath(
//                                                points = currentPoints.toList(),
//                                                color = currentColor,
//                                                strokeWidth = currentStrokeWidth
//                                            )
//                                            returnPath(partialPath)

                                        },
                                        onDragEnd = {
                                            currentPath?.let {
                                                // normalize before saving/sending
                                                val normalizedPoints = currentPoints.map { offset ->
                                                    Offset(
                                                        x = offset.x / size.width.toFloat(),
                                                        y = offset.y / size.height.toFloat()
                                                    )
                                                }
                                                val drawnPath = DrawnPath(
                                                    points = normalizedPoints,   // 0.0 to 1.0
                                                    color = currentColor,
                                                    strokeWidth = currentStrokeWidth
                                                )
                                                paths.add(drawnPath)
                                                returnPath(drawnPath)   // send normalized points
                                                //

                                                currentPath = null
                                                currentPoints.clear()
                                            }
                                        },

                                        onDragCancel = {
                                            currentPath = null
                                            currentPoints.clear()
                                        }
                                    )
                                }
                        ) {

                            val strokeStyle = { width: Float ->
                                Stroke(
                                    width = width,
                                    cap = StrokeCap.Round,
                                    join = StrokeJoin.Round
                                )
                            }

                            // Draw completed paths
                            paths.forEach { drawn ->
                                val path = Path().apply {
                                    drawn.points.forEachIndexed { i, offset ->
                                        val x = offset.x * size.width    // scale back up
                                        val y = offset.y * size.height
                                        if (i == 0) moveTo(x, y)
                                        else lineTo(x, y)
                                    }
                                }

                                drawPath(path, drawn.color, style = strokeStyle(drawn.strokeWidth))
                            }

                            // Draw actively dragged path
                            currentPath?.let { path ->
                                drawPath(
                                    path = path,
                                    color = currentColor,
                                    style = strokeStyle(currentStrokeWidth)
                                )
                            }
                        }

            }

            // Bottom Section: Common Tools (Pencils, Pens, etc.)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(60.dp)
                    .border(2.dp, Color.White, RoundedCornerShape(8.dp)),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = {
                    currentColor = Color.Black
                    currentStrokeWidth = 10f

                }) {
                    Icon(
                        imageVector = Icons.Default.Pinch,
                        contentDescription = "Pen"
                    )
                }
                IconButton(onClick = {
                    currentColor = Color.White
                    currentStrokeWidth = 20f
                }) {
                    Icon(
                        imageVector = Icons.Default.CommentsDisabled,
                        contentDescription = "Pen"
                    )
                }
            }
        }
    }