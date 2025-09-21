package com.example.mygame.game.entities

import android.graphics.*
import android.util.Log
import com.example.mygame.game.managers.MapManager
import kotlin.math.*

class Fireball(
    startX: Float,
    startY: Float,
    targetX: Float,
    targetY: Float,
    private val speed: Float = 450f // Increased speed for larger world scale
) {
    private var x = startX
    private var y = startY
    private val startPosX = startX
    private val startPosY = startY
    private var directionX: Float
    private var directionY: Float
    private var isActive = true
    private var isExploding = false
    
    // Animation settings
    private var currentFrame = 0
    private var animationTimer = 0f
    private val frameTime = 0.08f // 80ms per frame
    
    // Fireball properties - increased for better visibility
    private val fireballSize = 128f
    private val explosionSize = 128f
    private var explosionTimer = 0f
    private val explosionDuration = 0.5f // 500ms explosion animation
    
    // Textures (will be loaded from assets)
    private var flameFrames: List<Bitmap>? = null
    private var explosionFrames: List<Bitmap>? = null
    
    // Paint for drawing
    private val paint = Paint().apply {
        isAntiAlias = true
        isFilterBitmap = true
    }
    
    init {
        // Calculate direction vector
        val dx = targetX - startX
        val dy = targetY - startY
        val distance = sqrt(dx * dx + dy * dy)
        directionX = dx / distance
        directionY = dy / distance
        
        Log.d("Fireball", "Created fireball at ($startX, $startY) towards ($targetX, $targetY)")
    }
    
    fun loadTextures(flameFrames: List<Bitmap>, explosionFrames: List<Bitmap>) {
        this.flameFrames = flameFrames
        this.explosionFrames = explosionFrames
        Log.d("Fireball", "Loaded ${flameFrames.size} flame frames, ${explosionFrames.size} explosion frames")
    }
    
    fun update(deltaTime: Float, mapManager: MapManager) {
        if (!isActive) return
        
        animationTimer += deltaTime
        
        if (isExploding) {
            // Update explosion animation
            explosionTimer += deltaTime
            
            if (animationTimer >= frameTime) {
                currentFrame++
                animationTimer = 0f
                
                // Check if explosion animation is complete
                val explosionFrameCount = explosionFrames?.size ?: 0
                if (currentFrame >= explosionFrameCount || explosionTimer >= explosionDuration) {
                    isActive = false
                }
            }
        } else {
            // Update flying animation and movement
            if (animationTimer >= frameTime) {
                val flameFrameCount = flameFrames?.size ?: 0
                if (flameFrameCount > 0) {
                    currentFrame = (currentFrame + 1) % flameFrameCount
                }
                animationTimer = 0f
            }
            
            // Move fireball
            val moveDistance = speed * deltaTime
            x += directionX * moveDistance
            y += directionY * moveDistance
            
            // Check collision with walls
            if (mapManager.isWall(x, y)) {
                explode()
            }
            
            // Check if fireball went too far (safety check)
            val dx = x - startPosX
            val dy = y - startPosY
            val travelDistance = sqrt(dx * dx + dy * dy)
            if (travelDistance > 800f) { // Max range 800 pixels
                explode()
            }
        }
    }
    
    fun draw(canvas: Canvas, cameraX: Float, cameraY: Float) {
        if (!isActive) return
        
        val screenX = x - cameraX
        val screenY = y - cameraY
        
        if (isExploding) {
            // Draw explosion
            explosionFrames?.let { frames ->
                if (currentFrame < frames.size) {
                    val frame = frames[currentFrame]
                    val destRect = RectF(
                        screenX - explosionSize / 2f,
                        screenY - explosionSize / 2f,
                        screenX + explosionSize / 2f,
                        screenY + explosionSize / 2f
                    )
                    canvas.drawBitmap(frame, null, destRect, paint)
                }
            }
        } else {
            // Draw flying fireball
            flameFrames?.let { frames ->
                if (frames.isNotEmpty()) {
                    val frame = frames[currentFrame % frames.size]
                    val destRect = RectF(
                        screenX - fireballSize / 2f,
                        screenY - fireballSize / 2f,
                        screenX + fireballSize / 2f,
                        screenY + fireballSize / 2f
                    )
                    
                    // Calculate rotation based on direction
                    val angle = atan2(directionY, directionX) * 180f / PI.toFloat()
                    
                    canvas.save()
                    canvas.rotate(angle, screenX, screenY)
                    canvas.drawBitmap(frame, null, destRect, paint)
                    canvas.restore()
                }
            }
        }
    }
    
    private fun explode() {
        if (!isExploding) {
            isExploding = true
            currentFrame = 0
            explosionTimer = 0f
            Log.d("Fireball", "Fireball exploded at ($x, $y)")
        }
    }
    
    // Public methods
    fun isActive(): Boolean = isActive
    fun isExploding(): Boolean = isExploding
    fun getX(): Float = x
    fun getY(): Float = y
    fun getStartX(): Float = startPosX
    fun getStartY(): Float = startPosY
    
    // Check collision with a circular target (like enemies)
    fun checkCollision(targetX: Float, targetY: Float, targetRadius: Float): Boolean {
        if (isExploding || !isActive) return false
        
        val dx = x - targetX
        val dy = y - targetY
        val distance = sqrt(dx * dx + dy * dy)
        return distance <= (fireballSize / 2f + targetRadius)
    }
    
    // Trigger explosion manually (when hitting enemies)
    fun triggerExplosion() {
        if (!isExploding) {
            explode()
        }
    }
}