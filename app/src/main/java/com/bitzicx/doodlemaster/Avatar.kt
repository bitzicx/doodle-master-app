package com.bitzicx.doodlemaster

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage

@Composable
fun VectorAvatar(
    seed: String,
    style: String = "croodles",
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current

    // Build the dynamic DiceBear API URL using your seed
    val avatarUrl = "https://api.dicebear.com/9.x/$style/png?seed=$seed"
    AsyncImage(
        model = avatarUrl,
        contentDescription = "Vector Profile Picture",
        contentScale = ContentScale.Crop,
        modifier = modifier
            .fillMaxSize()
            .clip(CircleShape)
    )
}
