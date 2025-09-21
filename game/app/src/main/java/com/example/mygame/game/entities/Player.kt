package com.example.mygame.game.entities

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Matrix
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asAndroidBitmap
import com.example.mygame.game.assets.GameAssetManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking

class Player(private val context: Context, private val assetManager: GameAssetManager) {
    
    private var x = 0f
    private var y = 0f
    private var velocityX = 0f
    private var velocityY = 0f
    private val speed = 8f
    
    // Player sprite animations
    private var spritesBack = mutableListOf<Bitmap>()
    private var spritesFront = mutableListOf<Bitmap>()
    private var spritesLeft = mutableListOf<Bitmap>()
    private var spritesRight = mutableListOf<Bitmap>()
    
    // Idle sprites
    private var idleBack: Bitmap? = null
    private var idleFront: Bitmap? = null
    private var idleLeft: Bitmap? = null
    private var idleRight: Bitmap? = null
    
    private var currentSprites = mutableListOf<Bitmap>()
    private var currentIdleSprite: Bitmap? = null
    private var facingDirection = Direction.FRONT
    
    // Animation variables
    private var currentFrame = 0
    private var animationTimer = 0f
    private val animationSpeed = 8f // Frames before switching to next animation frame
    private var isMoving = false
    
    // Player size
    private val playerWidth = 120
    private val playerHeight = 120
    
    enum class Direction {
        FRONT, BACK, LEFT, RIGHT
    }
    
    init {
        loadSprites()
        currentSprites = spritesFront
        currentIdleSprite = idleFront
    }
    
    private fun loadSprites() {
        try {
            // Load sprites from assets using coroutines
            runBlocking {
                // Load idle sprites
                val idleBackBitmap = assetManager.loadTexture("characters/player/idle/player_idle_back.png")
                idleBackBitmap?.let {
                    idleBack = Bitmap.createScaledBitmap(it.asAndroidBitmap(), playerWidth, playerHeight, false)
                }
                
                val idleFrontBitmap = assetManager.loadTexture("characters/player/idle/player_idle_front.png")
                idleFrontBitmap?.let {
                    idleFront = Bitmap.createScaledBitmap(it.asAndroidBitmap(), playerWidth, playerHeight, false)
                }
                
                val idleLeftBitmap = assetManager.loadTexture("characters/player/idle/player_idle_left.png")
                idleLeftBitmap?.let {
                    idleLeft = Bitmap.createScaledBitmap(it.asAndroidBitmap(), playerWidth, playerHeight, false)
                    // Create right idle by flipping left
                    idleRight = flipBitmapHorizontally(idleLeft!!)
                }
                
                // Load back sprites (3 frames)
                for (i in 1..3) {
                    val frameNumber = i.toString().padStart(2, '0')
                    val imageBitmap = assetManager.loadTexture("characters/player/walk/player_walk_back_$frameNumber.png")
                    imageBitmap?.let {
                        val bitmap = it.asAndroidBitmap()
                        spritesBack.add(Bitmap.createScaledBitmap(bitmap, playerWidth, playerHeight, false))
                    }
                }
                
                // Load front sprites (3 frames)
                for (i in 1..3) {
                    val frameNumber = i.toString().padStart(2, '0')
                    val imageBitmap = assetManager.loadTexture("characters/player/walk/player_walk_front_$frameNumber.png")
                    imageBitmap?.let {
                        val bitmap = it.asAndroidBitmap()
                        spritesFront.add(Bitmap.createScaledBitmap(bitmap, playerWidth, playerHeight, false))
                    }
                }
                
                // Load left sprites (3 frames)
                for (i in 1..3) {
                    val frameNumber = i.toString().padStart(2, '0')
                    val imageBitmap = assetManager.loadTexture("characters/player/walk/player_walk_left_$frameNumber.png")
                    imageBitmap?.let {
                        val bitmap = it.asAndroidBitmap()
                        spritesLeft.add(Bitmap.createScaledBitmap(bitmap, playerWidth, playerHeight, false))
                    }
                }
                
                // Create right sprites by flipping left sprites horizontally
                for (leftSprite in spritesLeft) {
                    val flippedSprite = flipBitmapHorizontally(leftSprite)
                    spritesRight.add(flippedSprite)
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
    
    private fun flipBitmapHorizontally(bitmap: Bitmap): Bitmap {
        val matrix = Matrix()
        matrix.preScale(-1f, 1f)
        return Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, false)
    }
    
    fun setPosition(newX: Float, newY: Float) {
        x = newX
        y = newY
    }
    
    fun setMovementDirection(deltaX: Float, deltaY: Float) {
        val length = Math.sqrt((deltaX * deltaX + deltaY * deltaY).toDouble()).toFloat()
        
        if (length > 0) {
            velocityX = (deltaX / length) * speed
            velocityY = (deltaY / length) * speed
            isMoving = true
            
            // Determine facing direction based on movement
            when {
                Math.abs(deltaX) > Math.abs(deltaY) -> {
                    facingDirection = if (deltaX > 0) Direction.RIGHT else Direction.LEFT
                }
                deltaY > 0 -> facingDirection = Direction.FRONT
                deltaY < 0 -> facingDirection = Direction.BACK
            }
            
            updateCurrentSprites()
        }
    }
    
    fun stopMovement() {
        velocityX = 0f
        velocityY = 0f
        isMoving = false
        // Reset to first frame when stopping
        currentFrame = 0
        animationTimer = 0f
    }
    
    private fun updateCurrentSprites() {
        currentSprites = when (facingDirection) {
            Direction.FRONT -> spritesFront
            Direction.BACK -> spritesBack
            Direction.LEFT -> spritesLeft
            Direction.RIGHT -> spritesRight
        }
        
        currentIdleSprite = when (facingDirection) {
            Direction.FRONT -> idleFront
            Direction.BACK -> idleBack
            Direction.LEFT -> idleLeft
            Direction.RIGHT -> idleRight
        }
    }
    
    fun update(worldWidth: Float, worldHeight: Float, mapManager: com.example.mygame.game.managers.MapManager? = null) {
        // Calculate new position
        val newX = x + velocityX
        val newY = y + velocityY
        
        // Check collision with map if mapManager is provided
        if (mapManager != null) {
            // Check X movement
            if (mapManager.canMoveTo(newX, y, playerWidth.toFloat(), playerHeight.toFloat())) {
                x = newX
            }
            // Check Y movement  
            if (mapManager.canMoveTo(x, newY, playerWidth.toFloat(), playerHeight.toFloat())) {
                y = newY
            }
        } else {
            // Fallback to old boundary checking
            x = newX
            y = newY
            
            // Keep player within world bounds (not screen bounds)
            val halfWidth = playerWidth / 2f
            val halfHeight = playerHeight / 2f
            
            if (x - halfWidth < 0) {
                x = halfWidth
            } else if (x + halfWidth > worldWidth) {
                x = worldWidth - halfWidth
            }
            
            if (y - halfHeight < 0) {
                y = halfHeight
            } else if (y + halfHeight > worldHeight) {
                y = worldHeight - halfHeight
            }
        }
        
        // Update animation only when moving
        if (isMoving && currentSprites.isNotEmpty()) {
            animationTimer += 1f
            if (animationTimer >= animationSpeed) {
                currentFrame = (currentFrame + 1) % currentSprites.size
                animationTimer = 0f
            }
        }
    }
    
    fun draw(canvas: Canvas, paint: Paint, cameraX: Float, cameraY: Float) {
        val drawX = x - playerWidth / 2f - cameraX
        val drawY = y - playerHeight / 2f - cameraY
        
        if (isMoving && currentSprites.isNotEmpty()) {
            // Draw walking animation
            val currentSprite = currentSprites[currentFrame]
            canvas.drawBitmap(currentSprite, drawX, drawY, paint)
        } else {
            // Draw idle sprite
            currentIdleSprite?.let { idleSprite ->
                canvas.drawBitmap(idleSprite, drawX, drawY, paint)
            }
        }
    }
    
    fun getX(): Float = x
    fun getY(): Float = y
    fun getWidth(): Int = playerWidth
    fun getHeight(): Int = playerHeight
}
