package com.example.mygame.game.entities

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Matrix
import android.util.Log
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
    
    // Player size - reduced to fit through corridors
    private val playerWidth = 64  // Reduced from 120 to 64
    private val playerHeight = 64 // Reduced from 120 to 64
    
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
            Log.d("Player", "Loading sprites...")
            // Load sprites from assets using coroutines
            runBlocking {
                // Load idle sprites
                val idleBackBitmap = assetManager.loadTexture("characters/player/idle/player_idle_back.png")
                idleBackBitmap?.let {
                    idleBack = Bitmap.createScaledBitmap(it.asAndroidBitmap(), playerWidth, playerHeight, false)
                    Log.d("Player", "Loaded idle back sprite")
                } ?: Log.e("Player", "Failed to load idle back sprite")
                
                val idleFrontBitmap = assetManager.loadTexture("characters/player/idle/player_idle_front.png")
                idleFrontBitmap?.let {
                    idleFront = Bitmap.createScaledBitmap(it.asAndroidBitmap(), playerWidth, playerHeight, false)
                    Log.d("Player", "Loaded idle front sprite")
                } ?: Log.e("Player", "Failed to load idle front sprite")
                
                val idleLeftBitmap = assetManager.loadTexture("characters/player/idle/player_idle_left.png")
                idleLeftBitmap?.let {
                    idleLeft = Bitmap.createScaledBitmap(it.asAndroidBitmap(), playerWidth, playerHeight, false)
                    // Create right idle by flipping left
                    idleRight = flipBitmapHorizontally(idleLeft!!)
                    Log.d("Player", "Loaded idle left/right sprites")
                } ?: Log.e("Player", "Failed to load idle left sprite")
                
                // Load back sprites (3 frames)
                for (i in 1..3) {
                    val frameNumber = i.toString().padStart(2, '0')
                    val imageBitmap = assetManager.loadTexture("characters/player/walk/player_walk_back_$frameNumber.png")
                    imageBitmap?.let {
                        val bitmap = it.asAndroidBitmap()
                        spritesBack.add(Bitmap.createScaledBitmap(bitmap, playerWidth, playerHeight, false))
                        Log.d("Player", "Loaded walk back frame $i")
                    } ?: Log.e("Player", "Failed to load walk back frame $i")
                }
                
                // Load front sprites (3 frames)
                for (i in 1..3) {
                    val frameNumber = i.toString().padStart(2, '0')
                    val imageBitmap = assetManager.loadTexture("characters/player/walk/player_walk_front_$frameNumber.png")
                    imageBitmap?.let {
                        val bitmap = it.asAndroidBitmap()
                        spritesFront.add(Bitmap.createScaledBitmap(bitmap, playerWidth, playerHeight, false))
                        Log.d("Player", "Loaded walk front frame $i")
                    } ?: Log.e("Player", "Failed to load walk front frame $i")
                }
                
                // Load left sprites (3 frames)
                for (i in 1..3) {
                    val frameNumber = i.toString().padStart(2, '0')
                    val imageBitmap = assetManager.loadTexture("characters/player/walk/player_walk_left_$frameNumber.png")
                    imageBitmap?.let {
                        val bitmap = it.asAndroidBitmap()
                        spritesLeft.add(Bitmap.createScaledBitmap(bitmap, playerWidth, playerHeight, false))
                        Log.d("Player", "Loaded walk left frame $i")
                    } ?: Log.e("Player", "Failed to load walk left frame $i")
                }
                
                // Create right sprites by flipping left sprites horizontally
                for (leftSprite in spritesLeft) {
                    val flippedSprite = flipBitmapHorizontally(leftSprite)
                    spritesRight.add(flippedSprite)
                }
                Log.d("Player", "Created ${spritesRight.size} right sprites by flipping")
                
                Log.d("Player", "Sprite loading summary:")
                Log.d("Player", "  Back sprites: ${spritesBack.size}")
                Log.d("Player", "  Front sprites: ${spritesFront.size}")
                Log.d("Player", "  Left sprites: ${spritesLeft.size}")
                Log.d("Player", "  Right sprites: ${spritesRight.size}")
            }
        } catch (e: Exception) {
            Log.e("Player", "Error loading sprites", e)
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
        Log.d("Player", "Player position set to ($x, $y)")
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
        } else {
            Log.d("Player", "Zero length movement, stopping")
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
        if (!isMoving) return // Skip if not moving
        
        // Calculate new position
        val oldX = x
        val oldY = y
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
            // Fallback: Allow movement but constrain to world bounds
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
        
        // Debug actual position change (only when blocked)
        if (oldX == x && oldY == y && isMoving) {
            Log.w("Player", "Position NOT changed - blocked at ($x, $y)")
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
    fun getFacingDirection(): Direction = facingDirection
    fun getVelocityX(): Float = velocityX
    fun getVelocityY(): Float = velocityY
    fun isPlayerMoving(): Boolean = isMoving
}
