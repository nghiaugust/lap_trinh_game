package com.example.mygame.game.entities.projectiles

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.util.Log
import kotlin.math.*

/**
 * Arrow projectile class for Samurai Archer
 * Features:
 * - Directional movement with physics
 * - Rotation based on direction
 * - Collision detection
 * - Damage and range system
 */
class Arrow(
    startX: Float,
    startY: Float,
    directionX: Float,
    directionY: Float,
    private val sprite: Bitmap
) {
    
    // Position and movement
    private var x = startX
    private var y = startY
    private val dirX = directionX
    private val dirY = directionY
    private val speed = 600f // pixels per second
    
    // Arrow properties
    private val damage = 35f
    private val maxRange = 800f
    private var distanceTraveled = 0f
    private var isDestroyed = false
    
    // Visual properties
    private val width = 32f
    private val height = 8f
    private val rotation = calculateRotation(directionX, directionY)
    
    // Paint for rendering
    private val paint = Paint().apply {
        isAntiAlias = true
        isFilterBitmap = true
    }
    
    init {
        Log.d("Arrow", "Arrow created at ($x, $y) with direction ($dirX, $dirY), rotation: $rotation°")
    }
    
    /**
     * Calculate rotation angle based on direction
     */
    private fun calculateRotation(dx: Float, dy: Float): Float {
        val angle = atan2(dy, dx) * 180f / PI.toFloat()
        return angle
    }
    
    /**
     * Update arrow position and state
     */
    fun update(deltaTime: Float) {
        if (isDestroyed) return
        
        // Move arrow
        val moveDistance = speed * deltaTime
        x += dirX * moveDistance
        y += dirY * moveDistance
        distanceTraveled += moveDistance
        
        // Check if arrow has traveled too far
        if (distanceTraveled >= maxRange) {
            destroy()
            Log.d("Arrow", "Arrow destroyed - max range reached")
        }
        
        // Check world boundaries (assuming world size)
        if (x < -100 || x > 3100 || y < -100 || y > 2100) {
            destroy()
            Log.d("Arrow", "Arrow destroyed - out of bounds")
        }
    }
    
    /**
     * Render the arrow
     */
    fun render(canvas: Canvas, cameraX: Float, cameraY: Float) {
        if (isDestroyed) {
            Log.d("Arrow", "Arrow render skipped - destroyed")
            return
        }
        
        val screenX = x - cameraX
        val screenY = y - cameraY
        
        // Only render if arrow is visible on screen
        val screenWidth = canvas.width.toFloat()
        val screenHeight = canvas.height.toFloat()
        
        if (screenX > -width && screenX < screenWidth + width && 
            screenY > -height && screenY < screenHeight + height) {
            
            Log.d("Arrow", "Rendering arrow at screen ($screenX, $screenY), world ($x, $y)")
            
            canvas.save()
            
            // Rotate arrow based on direction
            canvas.rotate(rotation, screenX + width / 2, screenY + height / 2)
            
            // Draw arrow sprite
            canvas.drawBitmap(
                sprite,
                null,
                android.graphics.RectF(screenX, screenY, screenX + width, screenY + height),
                paint
            )
            
            canvas.restore()
        } else {
            Log.d("Arrow", "Arrow off-screen: screen ($screenX, $screenY), canvas (${screenWidth}x${screenHeight})")
        }
    }
    
    /**
     * Check collision with a target
     */
    fun checkCollision(targetX: Float, targetY: Float, targetWidth: Float, targetHeight: Float): Boolean {
        if (isDestroyed) return false
        
        // Simple AABB collision detection
        val collides = x < targetX + targetWidth &&
                      x + width > targetX &&
                      y < targetY + targetHeight &&
                      y + height > targetY
        
        if (collides) {
            destroy()
            Log.d("Arrow", "Arrow hit target at ($targetX, $targetY)")
        }
        
        return collides
    }
    
    /**
     * Check collision with circular target (for more precise collision)
     */
    fun checkCircularCollision(centerX: Float, centerY: Float, radius: Float): Boolean {
        if (isDestroyed) return false
        
        // Calculate distance between arrow center and target center
        val arrowCenterX = x + width / 2
        val arrowCenterY = y + height / 2
        
        val dx = arrowCenterX - centerX
        val dy = arrowCenterY - centerY
        val distance = sqrt(dx * dx + dy * dy)
        
        val collides = distance <= radius + (width / 2)
        
        if (collides) {
            destroy()
            Log.d("Arrow", "Arrow hit circular target at ($centerX, $centerY)")
        }
        
        return collides
    }
    
    /**
     * Check collision with map/walls
     */
    fun checkMapCollision(mapManager: com.example.mygame.game.managers.MapManager): Boolean {
        if (isDestroyed) return false
        
        // Check if arrow hit a wall
        val tileX = (x / mapManager.getTileSize()).toInt()
        val tileY = (y / mapManager.getTileSize()).toInt()
        
        if (mapManager.isWallTile(tileX, tileY)) {
            destroy()
            Log.d("Arrow", "Arrow hit wall at tile ($tileX, $tileY)")
            return true
        }
        
        return false
    }
    
    /**
     * Destroy the arrow
     */
    fun destroy() {
        isDestroyed = true
    }
    
    /**
     * Getters
     */
    fun getX() = x
    fun getY() = y
    fun getWidth() = width
    fun getHeight() = height
    fun getDamage() = damage
    fun isDestroyed() = isDestroyed
    fun getDistanceTraveled() = distanceTraveled
    fun getDirection() = Pair(dirX, dirY)
    fun getSpeed() = speed
    fun getRotation() = rotation
    
    /**
     * Get arrow bounds for collision detection
     */
    fun getBounds(): android.graphics.RectF {
        return android.graphics.RectF(x, y, x + width, y + height)
    }
    
    /**
     * Get arrow center point
     */
    fun getCenterX() = x + width / 2
    fun getCenterY() = y + height / 2
    
    /**
     * Check if arrow is visible on screen
     */
    fun isVisibleOnScreen(cameraX: Float, cameraY: Float, screenWidth: Float, screenHeight: Float): Boolean {
        val screenX = x - cameraX
        val screenY = y - cameraY
        
        return screenX > -width && screenX < screenWidth + width && 
               screenY > -height && screenY < screenHeight + height
    }
}