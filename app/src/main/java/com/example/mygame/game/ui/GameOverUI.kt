package com.example.mygame.game.ui

import android.graphics.*
import android.util.Log

class GameOverUI {
    // UI positioning
    private var screenWidth = 0f
    private var screenHeight = 0f
    
    // Button properties
    private var continueButtonX = 0f
    private var continueButtonY = 0f
    private val buttonWidth = 300f
    private val buttonHeight = 80f
    private var isButtonPressed = false
    
    // Paint objects for rendering
    private val backgroundPaint = Paint().apply {
        color = Color.argb(200, 0, 0, 0) // Semi-transparent black
        style = Paint.Style.FILL
    }
    
    private val titlePaint = Paint().apply {
        color = Color.RED
        textSize = 120f
        textAlign = Paint.Align.CENTER
        typeface = Typeface.DEFAULT_BOLD
        isAntiAlias = true
    }
    
    private val subtitlePaint = Paint().apply {
        color = Color.WHITE
        textSize = 40f
        textAlign = Paint.Align.CENTER
        isAntiAlias = true
    }
    
    private val buttonPaint = Paint().apply {
        style = Paint.Style.FILL
        isAntiAlias = true
    }
    
    private val buttonBorderPaint = Paint().apply {
        color = Color.WHITE
        style = Paint.Style.STROKE
        strokeWidth = 4f
        isAntiAlias = true
    }
    
    private val buttonTextPaint = Paint().apply {
        color = Color.WHITE
        textSize = 36f
        textAlign = Paint.Align.CENTER
        typeface = Typeface.DEFAULT_BOLD
        isAntiAlias = true
    }
    
    fun initialize(screenWidth: Float, screenHeight: Float) {
        this.screenWidth = screenWidth
        this.screenHeight = screenHeight
        
        // Position continue button at center-bottom
        continueButtonX = screenWidth / 2f
        continueButtonY = screenHeight * 0.7f
        
        Log.d("GameOverUI", "GameOverUI initialized with screen size: ${screenWidth}x${screenHeight}")
    }
    
    fun draw(canvas: Canvas) {
        // Draw semi-transparent background overlay
        canvas.drawRect(0f, 0f, screenWidth, screenHeight, backgroundPaint)
        
        // Draw "GAME OVER" title
        val titleY = screenHeight * 0.3f
        canvas.drawText("GAME OVER", screenWidth / 2f, titleY, titlePaint)
        
        // Draw subtitle
        val subtitleY = titleY + 80f
        canvas.drawText("Bạn đã bị đánh bại!", screenWidth / 2f, subtitleY, subtitlePaint)
        
        // Draw continue button
        drawContinueButton(canvas)
    }
    
    private fun drawContinueButton(canvas: Canvas) {
        // Calculate button rectangle
        val left = continueButtonX - buttonWidth / 2f
        val top = continueButtonY - buttonHeight / 2f
        val right = continueButtonX + buttonWidth / 2f
        val bottom = continueButtonY + buttonHeight / 2f
        
        // Draw button background (changes color when pressed)
        buttonPaint.color = if (isButtonPressed) {
            Color.argb(255, 100, 150, 100) // Darker green when pressed
        } else {
            Color.argb(255, 150, 200, 150) // Light green
        }
        
        canvas.drawRoundRect(left, top, right, bottom, 20f, 20f, buttonPaint)
        
        // Draw button border
        canvas.drawRoundRect(left, top, right, bottom, 20f, 20f, buttonBorderPaint)
        
        // Draw button text
        val textY = continueButtonY + 12f // Slightly offset for better visual centering
        canvas.drawText("TIẾP TỤC", continueButtonX, textY, buttonTextPaint)
    }
    
    fun handleTouch(x: Float, y: Float, action: Int): Boolean {
        when (action) {
            0 -> { // ACTION_DOWN
                if (isTouchInContinueButton(x, y)) {
                    isButtonPressed = true
                    Log.d("GameOverUI", "Continue button pressed")
                    return true
                }
            }
            1 -> { // ACTION_UP
                if (isButtonPressed) {
                    isButtonPressed = false
                    if (isTouchInContinueButton(x, y)) {
                        Log.d("GameOverUI", "Continue button clicked - returning to start screen")
                        return true // Signal that continue was clicked
                    }
                }
            }
        }
        return false
    }
    
    private fun isTouchInContinueButton(x: Float, y: Float): Boolean {
        val left = continueButtonX - buttonWidth / 2f
        val top = continueButtonY - buttonHeight / 2f
        val right = continueButtonX + buttonWidth / 2f
        val bottom = continueButtonY + buttonHeight / 2f
        
        return x >= left && x <= right && y >= top && y <= bottom
    }
    
    fun reset() {
        isButtonPressed = false
        Log.d("GameOverUI", "GameOverUI reset")
    }
}