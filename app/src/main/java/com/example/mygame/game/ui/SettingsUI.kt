package com.example.mygame.game.ui

import android.content.Context
import android.graphics.*
import android.util.Log
import com.example.mygame.game.assets.GameAssetManager

class SettingsUI {
    
    // Icon bitmaps for music and sound effects
    private var musicOnIcon: Bitmap? = null
    private var musicOffIcon: Bitmap? = null
    private var soundOnIcon: Bitmap? = null
    private var soundOffIcon: Bitmap? = null
    private val iconSize = 48f // Size for icons in buttons
    
    // UI dimensions
    private var screenWidth = 0f
    private var screenHeight = 0f
    private var menuWidth = 0f
    private var menuHeight = 0f
    private var menuX = 0f
    private var menuY = 0f
    
    // Button dimensions
    private val buttonWidth = 300f
    private val buttonHeight = 80f
    private val buttonSpacing = 20f
    
    // Menu buttons
    private var continueButtonRect = RectF()
    private var musicToggleButtonRect = RectF()
    private var sfxToggleButtonRect = RectF()
    private var exitButtonRect = RectF()
    
    // Colors and paint
    private val backgroundPaint = Paint().apply {
        color = Color.argb(200, 0, 0, 0) // Semi-transparent black overlay
        style = Paint.Style.FILL
    }
    
    private val menuBackgroundPaint = Paint().apply {
        color = Color.argb(240, 50, 50, 70) // Dark blue-gray menu background
        style = Paint.Style.FILL
    }
    
    private val menuBorderPaint = Paint().apply {
        color = Color.WHITE
        style = Paint.Style.STROKE
        strokeWidth = 4f
    }
    
    private val buttonPaint = Paint().apply {
        color = Color.argb(180, 100, 100, 120)
        style = Paint.Style.FILL
    }
    
    private val buttonBorderPaint = Paint().apply {
        color = Color.WHITE
        style = Paint.Style.STROKE
        strokeWidth = 2f
    }
    
    private val buttonPressedPaint = Paint().apply {
        color = Color.argb(220, 150, 150, 170)
        style = Paint.Style.FILL
    }
    
    private val textPaint = Paint().apply {
        color = Color.WHITE
        textSize = 36f
        textAlign = Paint.Align.CENTER
        isAntiAlias = true
        typeface = Typeface.DEFAULT_BOLD
    }
    
    private val titlePaint = Paint().apply {
        color = Color.WHITE
        textSize = 48f
        textAlign = Paint.Align.CENTER
        isAntiAlias = true
        typeface = Typeface.DEFAULT_BOLD
    }
    
    // State tracking
    private var isMusicEnabled = true
    private var isSfxEnabled = true
    private var pressedButton: ButtonType? = null
    
    enum class ButtonType {
        CONTINUE,
        MUSIC_TOGGLE,
        SFX_TOGGLE,
        EXIT
    }
    
    fun initialize(screenWidth: Float, screenHeight: Float, isMusicEnabled: Boolean, isSfxEnabled: Boolean = true, assetManager: GameAssetManager? = null) {
        this.screenWidth = screenWidth
        this.screenHeight = screenHeight
        this.isMusicEnabled = isMusicEnabled
        this.isSfxEnabled = isSfxEnabled
        
        loadIcons(assetManager)
        
        // Calculate menu dimensions (60% of screen width, adjusted height for 4 buttons)
        menuWidth = screenWidth * 0.6f
        menuHeight = screenHeight * 0.6f // Increased from 0.5f to fit 4 buttons
        menuX = (screenWidth - menuWidth) / 2f
        menuY = (screenHeight - menuHeight) / 2f
        
        setupButtons()
        
        Log.d("SettingsUI", "Settings UI initialized: ${screenWidth}x${screenHeight}")
    }
    
    private fun loadIcons(assetManager: GameAssetManager?) {
        assetManager?.let { assets ->
            try {
                // Load icons using the new direct bitmap method
                Log.d("SettingsUI", "Starting to load music/sound icons using loadBitmapDirect...")
                
                // Load music icons
                try {
                    val musicOnBitmap = assets.loadBitmapDirect("textures/music/music_TurnOn.png")
                    musicOnBitmap?.let {
                        musicOnIcon = Bitmap.createScaledBitmap(it, iconSize.toInt(), iconSize.toInt(), false)
                        Log.d("SettingsUI", "Successfully loaded music ON icon")
                    } ?: Log.w("SettingsUI", "Failed to load music ON bitmap")
                } catch (e: Exception) {
                    Log.e("SettingsUI", "Error loading music ON icon: ${e.message}")
                }
                
                try {
                    val musicOffBitmap = assets.loadBitmapDirect("textures/music/music_TurnOff.png")
                    musicOffBitmap?.let {
                        musicOffIcon = Bitmap.createScaledBitmap(it, iconSize.toInt(), iconSize.toInt(), false)
                        Log.d("SettingsUI", "Successfully loaded music OFF icon")
                    } ?: Log.w("SettingsUI", "Failed to load music OFF bitmap")
                } catch (e: Exception) {
                    Log.e("SettingsUI", "Error loading music OFF icon: ${e.message}")
                }
                
                // Load sound effect icons
                try {
                    val soundOnBitmap = assets.loadBitmapDirect("textures/music/sound_on.png")
                    soundOnBitmap?.let {
                        soundOnIcon = Bitmap.createScaledBitmap(it, iconSize.toInt(), iconSize.toInt(), false)
                        Log.d("SettingsUI", "Successfully loaded sound ON icon")
                    } ?: Log.w("SettingsUI", "Failed to load sound ON bitmap")
                } catch (e: Exception) {
                    Log.e("SettingsUI", "Error loading sound ON icon: ${e.message}")
                }
                
                try {
                    val soundOffBitmap = assets.loadBitmapDirect("textures/music/sound_off.png")
                    soundOffBitmap?.let {
                        soundOffIcon = Bitmap.createScaledBitmap(it, iconSize.toInt(), iconSize.toInt(), false)
                        Log.d("SettingsUI", "Successfully loaded sound OFF icon")
                    } ?: Log.w("SettingsUI", "Failed to load sound OFF bitmap")
                } catch (e: Exception) {
                    Log.e("SettingsUI", "Error loading sound OFF icon: ${e.message}")
                }
                
                Log.d("SettingsUI", "Icon loading completed. Icons loaded: musicOn=${musicOnIcon != null}, musicOff=${musicOffIcon != null}, soundOn=${soundOnIcon != null}, soundOff=${soundOffIcon != null}")
                
            } catch (e: Exception) {
                Log.e("SettingsUI", "General error loading icons: ${e.message}")
                e.printStackTrace()
            }
        } ?: Log.w("SettingsUI", "AssetManager is null, cannot load icons")
    }
    
    private fun setupButtons() {
        val buttonStartX = menuX + (menuWidth - buttonWidth) / 2f
        var currentY = menuY + 120f // Start below title
        
        // Continue button
        continueButtonRect.set(
            buttonStartX,
            currentY,
            buttonStartX + buttonWidth,
            currentY + buttonHeight
        )
        currentY += buttonHeight + buttonSpacing
        
        // Music toggle button
        musicToggleButtonRect.set(
            buttonStartX,
            currentY,
            buttonStartX + buttonWidth,
            currentY + buttonHeight
        )
        currentY += buttonHeight + buttonSpacing
        
        // SFX toggle button
        sfxToggleButtonRect.set(
            buttonStartX,
            currentY,
            buttonStartX + buttonWidth,
            currentY + buttonHeight
        )
        currentY += buttonHeight + buttonSpacing
        
        // Exit button
        exitButtonRect.set(
            buttonStartX,
            currentY,
            buttonStartX + buttonWidth,
            currentY + buttonHeight
        )
    }
    
    fun draw(canvas: Canvas) {
        // Draw semi-transparent overlay
        canvas.drawRect(0f, 0f, screenWidth, screenHeight, backgroundPaint)
        
        // Draw menu background
        val menuRect = RectF(menuX, menuY, menuX + menuWidth, menuY + menuHeight)
        canvas.drawRoundRect(menuRect, 20f, 20f, menuBackgroundPaint)
        canvas.drawRoundRect(menuRect, 20f, 20f, menuBorderPaint)
        
        // Draw title
        canvas.drawText(
            "CÀI ĐẶT",
            menuX + menuWidth / 2f,
            menuY + 70f,
            titlePaint
        )
        
        // Draw buttons
        drawButton(canvas, continueButtonRect, "TIẾP TỤC", ButtonType.CONTINUE)
        
        val musicText = if (isMusicEnabled) "TẮT NHẠC NỀN" else "BẬT NHẠC NỀN"
        drawButtonWithIcon(canvas, musicToggleButtonRect, musicText, ButtonType.MUSIC_TOGGLE, 
                          if (isMusicEnabled) musicOnIcon else musicOffIcon)
        
        val sfxText = if (isSfxEnabled) "TẮT HIỆU ỨNG" else "BẬT HIỆU ỨNG"
        drawButtonWithIcon(canvas, sfxToggleButtonRect, sfxText, ButtonType.SFX_TOGGLE,
                          if (isSfxEnabled) soundOnIcon else soundOffIcon)
        
        drawButton(canvas, exitButtonRect, "THOÁT GAME", ButtonType.EXIT)
    }
    
    private fun drawButton(canvas: Canvas, rect: RectF, text: String, buttonType: ButtonType) {
        val paint = if (pressedButton == buttonType) buttonPressedPaint else buttonPaint
        
        // Draw button background
        canvas.drawRoundRect(rect, 10f, 10f, paint)
        canvas.drawRoundRect(rect, 10f, 10f, buttonBorderPaint)
        
        // Draw button text
        val textY = rect.centerY() + (textPaint.textSize / 3f) // Center text vertically
        canvas.drawText(text, rect.centerX(), textY, textPaint)
    }
    
    private fun drawButtonWithIcon(canvas: Canvas, rect: RectF, text: String, buttonType: ButtonType, icon: Bitmap?) {
        val paint = if (pressedButton == buttonType) buttonPressedPaint else buttonPaint
        
        // Draw button background
        canvas.drawRoundRect(rect, 10f, 10f, paint)
        canvas.drawRoundRect(rect, 10f, 10f, buttonBorderPaint)
        
        // Debug: Log icon status
        Log.d("SettingsUI", "Drawing button ${buttonType.name} - icon is ${if (icon != null) "available" else "null"}")
        
        // Draw icon on the left side of button
        icon?.let { iconBitmap ->
            val iconX = rect.left + 20f
            val iconY = rect.centerY() - iconSize / 2f
            canvas.drawBitmap(iconBitmap, iconX, iconY, null)
            Log.d("SettingsUI", "Drew icon for ${buttonType.name} at ($iconX, $iconY)")
        } ?: Log.w("SettingsUI", "No icon to draw for ${buttonType.name}")
        
        // Draw button text (offset to the right if icon exists)
        val textX = if (icon != null) rect.centerX() + 30f else rect.centerX()
        val textY = rect.centerY() + (textPaint.textSize / 3f) // Center text vertically
        canvas.drawText(text, textX, textY, textPaint)
    }
    
    fun handleTouch(x: Float, y: Float, isPressed: Boolean): ButtonType? {
        if (isPressed) {
            // Check which button was pressed
            when {
                continueButtonRect.contains(x, y) -> {
                    pressedButton = ButtonType.CONTINUE
                    return ButtonType.CONTINUE
                }
                musicToggleButtonRect.contains(x, y) -> {
                    pressedButton = ButtonType.MUSIC_TOGGLE
                    return ButtonType.MUSIC_TOGGLE
                }
                sfxToggleButtonRect.contains(x, y) -> {
                    pressedButton = ButtonType.SFX_TOGGLE
                    return ButtonType.SFX_TOGGLE
                }
                exitButtonRect.contains(x, y) -> {
                    pressedButton = ButtonType.EXIT
                    return ButtonType.EXIT
                }
            }
        } else {
            // Release press state
            val wasPressed = pressedButton
            pressedButton = null
            return wasPressed
        }
        
        return null
    }
    
    fun updateMusicState(enabled: Boolean) {
        isMusicEnabled = enabled
    }
    
    fun updateSfxState(enabled: Boolean) {
        isSfxEnabled = enabled
    }
    
    fun isMusicEnabled(): Boolean = isMusicEnabled
    fun isSfxEnabled(): Boolean = isSfxEnabled
}