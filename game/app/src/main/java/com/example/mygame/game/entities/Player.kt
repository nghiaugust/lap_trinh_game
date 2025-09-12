package com.example.mygame.game.entities

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Matrix
import com.example.mygame.R

class Player(private val context: Context) {
    
    private var x = 0f
    private var y = 0f
    private var velocityX = 0f
    private var velocityY = 0f
    private val speed = 8f
    
    // Player sprite animations (3 frames per direction)
    private var spritesBack = mutableListOf<Bitmap>()
    private var spritesFront = mutableListOf<Bitmap>()
    private var spritesLeft = mutableListOf<Bitmap>()
    private var spritesRight = mutableListOf<Bitmap>()
    
    private var currentSprites = mutableListOf<Bitmap>()
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
    }
    
    private fun loadSprites() {
        try {
            // Load back sprites (3 frames)
            for (i in 1..3) {
                val resourceName = "back_$i"
                val resourceId = context.resources.getIdentifier(resourceName, "drawable", context.packageName)
                if (resourceId != 0) {
                    val originalBitmap = BitmapFactory.decodeResource(context.resources, resourceId)
                    spritesBack.add(Bitmap.createScaledBitmap(originalBitmap, playerWidth, playerHeight, false))
                }
            }
            
            // Load front sprites (3 frames)
            for (i in 1..3) {
                val resourceName = "front_$i"
                val resourceId = context.resources.getIdentifier(resourceName, "drawable", context.packageName)
                if (resourceId != 0) {
                    val originalBitmap = BitmapFactory.decodeResource(context.resources, resourceId)
                    spritesFront.add(Bitmap.createScaledBitmap(originalBitmap, playerWidth, playerHeight, false))
                }
            }
            
            // Load left sprites (3 frames)
            for (i in 1..3) {
                val resourceName = "left_$i"
                val resourceId = context.resources.getIdentifier(resourceName, "drawable", context.packageName)
                if (resourceId != 0) {
                    val originalBitmap = BitmapFactory.decodeResource(context.resources, resourceId)
                    spritesLeft.add(Bitmap.createScaledBitmap(originalBitmap, playerWidth, playerHeight, false))
                }
            }
            
            // Create right sprites by flipping left sprites horizontally
            for (leftSprite in spritesLeft) {
                val flippedSprite = flipBitmapHorizontally(leftSprite)
                spritesRight.add(flippedSprite)
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
    }
    
    fun update(worldWidth: Float, worldHeight: Float) {
        // Update position
        x += velocityX
        y += velocityY
        
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
        if (currentSprites.isNotEmpty()) {
            val currentSprite = currentSprites[currentFrame]
            // Draw player relative to camera position
            val drawX = x - playerWidth / 2f - cameraX
            val drawY = y - playerHeight / 2f - cameraY
            canvas.drawBitmap(currentSprite, drawX, drawY, paint)
        }
    }
    
    fun getX(): Float = x
    fun getY(): Float = y
    fun getWidth(): Int = playerWidth
    fun getHeight(): Int = playerHeight
}
