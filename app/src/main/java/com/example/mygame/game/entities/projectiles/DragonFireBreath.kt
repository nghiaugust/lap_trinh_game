package com.example.mygame.game.entities.projectiles

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import com.example.mygame.game.managers.MapManager
import kotlin.math.*

class DragonFireBreath(
    startX: Float,
    startY: Float,
    targetX: Float,
    targetY: Float,
    private val mapManager: MapManager
) {
    // Position and movement
    private var x = startX
    private var y = startY
    private val speed = 200f // Fire breath speed
    private val size = 32f // Smaller than fireball
    private var isActive = true
    private var isExploding = false
    
    // Calculate direction
    private val distance = sqrt((targetX - startX) * (targetX - startX) + (targetY - startY) * (targetY - startY))
    private val directionX = if (distance > 0) (targetX - startX) / distance else 0f
    private val directionY = if (distance > 0) (targetY - startY) / distance else 0f
    
    // Fire breath properties
    private val maxRange = 256f // 2 tiles range (128 * 2)
    private var travelledDistance = 0f
    private val damage = 20 // High damage fire breath
    
    // Animation
    private var animationFrame = 0
    private var lastAnimationTime = 0L
    private val animationSpeed = 80L // Fast animation for fire effect
    
    // Explosion properties
    private var explosionFrame = 0
    private var explosionStartTime = 0L
    private val explosionDuration = 300L // 0.3 seconds explosion
    
    // Textures (will be fallback colored circles if no textures)
    private var fireBreathTextures = mutableListOf<Bitmap>()
    private var explosionTextures = mutableListOf<Bitmap>()
    private var texturesLoaded = false
    
    // Fallback rendering
    private val fireBreathPaint = Paint().apply {
        color = Color.argb(200, 255, 100, 0) // Orange fire color
        style = Paint.Style.FILL
        isAntiAlias = true
    }
    
    private val explosionPaint = Paint().apply {
        color = Color.argb(150, 255, 50, 0) // Red explosion color
        style = Paint.Style.FILL
        isAntiAlias = true
    }
    
    fun loadTextures(fireBreathFrames: List<Bitmap>, explosionFrames: List<Bitmap>) {
        fireBreathTextures.clear()
        explosionTextures.clear()
        
        fireBreathTextures.addAll(fireBreathFrames)
        explosionTextures.addAll(explosionFrames)
        texturesLoaded = true
    }
    
    fun update(deltaTime: Float) {
        if (!isActive) return
        
        val currentTime = System.currentTimeMillis()
        
        if (isExploding) {
            // Handle explosion animation
            if (currentTime - explosionStartTime > explosionDuration) {
                isActive = false
            } else {
                updateExplosionAnimation(currentTime)
            }
            return
        }
        
        // Move fire breath
        val moveDistance = speed * deltaTime
        x += directionX * moveDistance
        y += directionY * moveDistance
        travelledDistance += moveDistance
        
        // Check if reached max range
        if (travelledDistance >= maxRange) {
            triggerExplosion()
            return
        }
        
        // Check collision with walls
        if (mapManager.isWall(x, y)) {
            triggerExplosion()
            return
        }
        
        // Update animation
        updateAnimation(currentTime)
    }
    
    private fun updateAnimation(currentTime: Long) {
        if (currentTime - lastAnimationTime > animationSpeed) {
            animationFrame++
            lastAnimationTime = currentTime
            
            if (fireBreathTextures.isNotEmpty()) {
                if (animationFrame >= fireBreathTextures.size) {
                    animationFrame = 0
                }
            }
        }
    }
    
    private fun updateExplosionAnimation(currentTime: Long) {
        val explosionProgress = (currentTime - explosionStartTime).toFloat() / explosionDuration
        explosionFrame = (explosionProgress * 8).toInt().coerceIn(0, 7) // 8 frame explosion
    }
    
    fun triggerExplosion() {
        if (!isExploding) {
            isExploding = true
            explosionStartTime = System.currentTimeMillis()
            explosionFrame = 0
        }
    }
    
    fun draw(canvas: Canvas, cameraX: Float, cameraY: Float) {
        if (!isActive) return
        
        val drawX = x - cameraX
        val drawY = y - cameraY
        
        if (isExploding) {
            drawExplosion(canvas, drawX, drawY)
        } else {
            drawFireBreath(canvas, drawX, drawY)
        }
    }
    
    private fun drawFireBreath(canvas: Canvas, drawX: Float, drawY: Float) {
        if (texturesLoaded && fireBreathTextures.isNotEmpty()) {
            // Draw with texture
            val frameIndex = animationFrame.coerceIn(0, fireBreathTextures.size - 1)
            val texture = fireBreathTextures[frameIndex]
            canvas.drawBitmap(texture, drawX - size, drawY - size, null)
        } else {
            // Fallback: draw colored circle
            canvas.drawCircle(drawX, drawY, size, fireBreathPaint)
            
            // Add inner glow effect
            val glowPaint = Paint().apply {
                color = Color.argb(100, 255, 200, 0)
                style = Paint.Style.FILL
                isAntiAlias = true
            }
            canvas.drawCircle(drawX, drawY, size * 0.6f, glowPaint)
        }
    }
    
    private fun drawExplosion(canvas: Canvas, drawX: Float, drawY: Float) {
        val explosionSize = size * (2f + explosionFrame * 0.5f) // Growing explosion
        
        if (texturesLoaded && explosionTextures.isNotEmpty() && explosionFrame < explosionTextures.size) {
            // Draw with texture
            val texture = explosionTextures[explosionFrame]
            canvas.drawBitmap(texture, drawX - explosionSize, drawY - explosionSize, null)
        } else {
            // Fallback: draw expanding colored circles
            val alpha = (255 * (1f - explosionFrame / 8f)).toInt().coerceIn(0, 255)
            explosionPaint.alpha = alpha
            
            canvas.drawCircle(drawX, drawY, explosionSize, explosionPaint)
            
            // Inner explosion effect
            val innerPaint = Paint().apply {
                color = Color.argb(alpha / 2, 255, 255, 0)
                style = Paint.Style.FILL
                isAntiAlias = true
            }
            canvas.drawCircle(drawX, drawY, explosionSize * 0.6f, innerPaint)
        }
    }
    
    // Check collision with target (player)
    fun checkCollision(targetX: Float, targetY: Float, targetSize: Float): Boolean {
        if (!isActive || isExploding) return false
        
        val dx = x - targetX
        val dy = y - targetY
        val distance = sqrt(dx * dx + dy * dy)
        
        return distance <= (size + targetSize)
    }
    
    // Check explosion collision (larger area)
    fun checkExplosionCollision(targetX: Float, targetY: Float, targetSize: Float): Boolean {
        if (!isExploding) return false
        
        val explosionRadius = size * (2f + explosionFrame * 0.5f)
        val dx = x - targetX
        val dy = y - targetY
        val distance = sqrt(dx * dx + dy * dy)
        
        return distance <= (explosionRadius + targetSize)
    }
    
    // Getters
    fun getX(): Float = x
    fun getY(): Float = y
    fun isActive(): Boolean = isActive
    fun isExploding(): Boolean = isExploding
    fun getDamage(): Int = damage
    fun getSize(): Float = size
    
    // Check if projectile should be removed
    fun shouldRemove(): Boolean = !isActive
    
    // Check collision with player
    fun checkPlayerCollision(playerX: Float, playerY: Float): Boolean {
        if (!isActive || isExploding) return false
        
        val dx = x - playerX
        val dy = y - playerY
        val distance = sqrt(dx * dx + dy * dy)
        
        // Check if player is within fire breath collision radius
        if (distance <= (size + 32f)) { // 32f is half player size
            triggerExplosion()
            return true
        }
        
        return false
    }
}