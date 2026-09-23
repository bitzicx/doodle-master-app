package com.bitzicx.doodlemaster.main_screen

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.shape.CircleShape


@Composable
fun PillMenuButton(
    text: String,
    icon: ImageVector,
    containerColor: Color,
    onClick: () -> Unit
) {
    Button(
        onClick = onClick,
        shape = RoundedCornerShape(50), // Gives the pill shape
        colors = ButtonDefaults.buttonColors(containerColor = containerColor),
        border = BorderStroke(3.dp, Color.Black), // Thick cartoon border
        modifier = Modifier
            .fillMaxWidth(0.95f)
            .height(72.dp)
            .padding(vertical = 8.dp)
    ) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            // Icon aligned to the left
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier
                    .align(Alignment.CenterStart)
                    .padding(start = 16.dp, end = 16.dp)
                    .size(32.dp)
            )

            // Text centered in the button
            Text(
                text = text,
                color = Color.White,
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
                // fontFamily = CustomDoodleFont, // Add your custom font here
            )
        }
    }
}



@Composable
fun CircleIconButton(
    text: String,
    icon: ImageVector,
    onClick: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.padding(8.dp)
    ) {
        Surface(
            onClick = onClick,
            shape = CircleShape,
            color = Color.White,
            border = BorderStroke(2.dp, Color.Black),
            modifier = Modifier.size(64.dp)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = icon,
                    contentDescription = text,
                    tint = Color.Black,
                    modifier = Modifier.size(32.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = text,
            color = Color.Black,
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold
        )
    }
}