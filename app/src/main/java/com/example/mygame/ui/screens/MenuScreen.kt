package com.example.mygame.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

/**
 * Màn hình menu chính của game
 */
@Composable
fun MenuScreen(
    onStartGame: () -> Unit,
    onSettings: () -> Unit,
    onExit: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "Dungeon Crawler",
            style = MaterialTheme.typography.headlineLarge
        )

        Spacer(modifier = Modifier.height(32.dp))

        Button(
            onClick = onStartGame,
            modifier = Modifier.fillMaxWidth(0.6f)
        ) {
            Text("Bắt đầu Game")
        }

        Spacer(modifier = Modifier.height(16.dp))

        Button(
            onClick = onSettings,
            modifier = Modifier.fillMaxWidth(0.6f)
        ) {
            Text("Cài đặt")
        }

        Spacer(modifier = Modifier.height(16.dp))

        Button(
            onClick = onExit,
            modifier = Modifier.fillMaxWidth(0.6f)
        ) {
            Text("Thoát Game")
        }
    }
}
