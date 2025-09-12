package com.example.mygame.ui.activities

import android.annotation.SuppressLint
import android.app.Activity
import android.os.Bundle
import android.view.Window
import android.view.WindowManager
import com.example.mygame.game.views.GameView

class GameActivity : Activity() {
    
    private lateinit var gameView: GameView
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Make the game fullscreen
        requestWindowFeature(Window.FEATURE_NO_TITLE)
        window.setFlags(
            WindowManager.LayoutParams.FLAG_FULLSCREEN,
            WindowManager.LayoutParams.FLAG_FULLSCREEN
        )
        
        // Hide the navigation bar and status bar
        window.decorView.systemUiVisibility = (
            android.view.View.SYSTEM_UI_FLAG_LAYOUT_STABLE
            or android.view.View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
            or android.view.View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
            or android.view.View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
            or android.view.View.SYSTEM_UI_FLAG_FULLSCREEN
            or android.view.View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
        )
        
        // Keep screen on during gameplay
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        
        // Create and set the game view
        gameView = GameView(this)
        setContentView(gameView)
    }
    
    override fun onResume() {
        super.onResume()
        // Game will resume automatically when surface is created
        gameView.onResume()
    }
    
    override fun onPause() {
        super.onPause()
        // Game will pause automatically when surface is destroyed
        gameView.onPause()
    }
    
    override fun onDestroy() {
        super.onDestroy()
        gameView.onDestroy()
    }
    
    @SuppressLint("GestureBackNavigation")
    @Suppress("DEPRECATION")
    override fun onBackPressed() {
        // Handle back button - pause game and return to main menu
        gameView.onPause()
        super.onBackPressed()
    }
}
