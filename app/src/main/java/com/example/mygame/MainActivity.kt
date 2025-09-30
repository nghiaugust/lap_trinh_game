package com.example.mygame

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.example.mygame.ui.theme.MygameTheme
import com.example.mygame.ui.screens.DungeonCrawlerStartScreen
import com.example.mygame.ui.activities.GameActivity

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MygameTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    DungeonCrawlerStartScreen(
                        onStartGame = {
                            // Navigate to gameplay screen
                            val intent = Intent(this@MainActivity, GameActivity::class.java)
                            startActivity(intent)
                        }
                    )
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun DungeonCrawlerPreview() {
    MygameTheme {
        DungeonCrawlerStartScreen(
            onStartGame = { }
        )
    }
}