package com.example.mygame.ui.screens

import android.app.Activity
import android.content.pm.ActivityInfo
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.mygame.R

@Composable
fun DungeonCrawlerStartScreen(
    onStartGame: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current

    // Force landscape orientation
    LaunchedEffect(Unit) {
        (context as? Activity)?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
    }

    Box(
        modifier = modifier.fillMaxSize()
    ) {
        // Background image - sử dụng file background.jpg
        Image(
            painter = painterResource(id = R.drawable.background),
            contentDescription = "Dungeon Crawler Background",
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.FillBounds
        )

        // Semi-transparent overlay
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.3f))
        )

        // Game Title - góc trên trái
        Text(
            text = "DUNGEON CRAWLER",
            fontSize = 32.sp,
            fontWeight = FontWeight.Bold,
            color = Color.White,
            fontFamily = FontFamily.Default,
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(20.dp)
        )

        // From KMA text - góc phải phía dưới
        Text(
            text = "from l02 KMA",
            fontSize = 14.sp,
            fontWeight = FontWeight.Normal,
            color = Color.White.copy(alpha = 0.8f),
            fontFamily = FontFamily.Default,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(15.dp)
        )

        // Start Game Button
        Box(
            modifier = Modifier.align(Alignment.Center)
        ) {
            Button(
                onClick = onStartGame,
                modifier = Modifier
                    .width(250.dp)
                    .height(60.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFFFFD700),
                    contentColor = Color.Black
                )
            ) {
                Text(
                    text = "START GAME",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}


@Preview(
    showBackground = true,
    widthDp = 720,
    heightDp = 360
)
@Composable
fun DungeonCrawlerStartScreenPreview() {
    MaterialTheme {
        DungeonCrawlerStartScreen(
            onStartGame = { }
        )
    }
}