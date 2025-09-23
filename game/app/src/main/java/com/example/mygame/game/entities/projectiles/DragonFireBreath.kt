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
    private val size = 64f // Larger fire breath size (was 32f)
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
    
    // Animation - optimized for performance
    private var animationFrame = 0
    private var lastAnimationTime = 0L
    private val animationSpeed = 120L // Slower animation for better performance (was 80L)
    
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
            // Draw with texture - scale to larger size
            val frameIndex = animationFrame.coerceIn(0, fireBreathTextures.size - 1)
            val texture = fireBreathTextures[frameIndex]
            
            // Create scaled destination rectangle for fire breath sprite
            val destRect = android.graphics.RectF(
                drawX - size,
                drawY - size,
                drawX + size,
                drawY + size
            )
            
            canvas.drawBitmap(texture, null, destRect, null)
        } else {
            // Enhanced fallback: draw more impressive fire effect
            // Outer fire ring (red-orange)
            val outerPaint = Paint().apply {
                color = Color.argb(200, 255, 69, 0) // Red-orange
                style = Paint.Style.FILL
                isAntiAlias = true
            }
            canvas.drawCircle(drawX, drawY, size, outerPaint)
            
            // Middle fire ring (orange)
            val middlePaint = Paint().apply {
                color = Color.argb(220, 255, 140, 0) // Orange
                style = Paint.Style.FILL
                isAntiAlias = true
            }
            canvas.drawCircle(drawX, drawY, size * 0.7f, middlePaint)
            
            // Inner fire core (yellow-white)
            val innerPaint = Paint().apply {
                color = Color.argb(255, 255, 255, 100) // Yellow-white
                style = Paint.Style.FILL
                isAntiAlias = true
            }
            canvas.drawCircle(drawX, drawY, size * 0.4f, innerPaint)
        }
    }
    
    private fun drawExplosion(canvas: Canvas, drawX: Float, drawY: Float) {
        val explosionSize = size * (1.5f + explosionFrame * 0.3f) // Growing explosion, larger base size
        
        if (texturesLoaded && explosionTextures.isNotEmpty() && explosionFrame < explosionTextures.size) {
            // Draw with texture - scaled
            val texture = explosionTextures[explosionFrame]
            val destRect = android.graphics.RectF(
                drawX - explosionSize,
                drawY - explosionSize,
                drawX + explosionSize,
                drawY + explosionSize
            )
            canvas.drawBitmap(texture, null, destRect, null)
        } else {
            // Enhanced fallback: draw impressive fire explosion
            val alpha = (255 * (1f - explosionFrame / 8f)).toInt().coerceIn(0, 255)
            
            // Outer explosion ring (dark red)
            val outerPaint = Paint().apply {
                color = Color.argb(alpha, 139, 0, 0)
                style = Paint.Style.FILL
                isAntiAlias = true
            }
            canvas.drawCircle(drawX, drawY, explosionSize, outerPaint)
            
            // Middle explosion ring (red-orange)
            val middlePaint = Paint().apply {
                color = Color.argb(alpha, 255, 69, 0)
                style = Paint.Style.FILL
                isAntiAlias = true
            }
            canvas.drawCircle(drawX, drawY, explosionSize * 0.7f, middlePaint)
            
            // Inner explosion core (bright orange-yellow)
            val innerPaint = Paint().apply {
                color = Color.argb(alpha, 255, 215, 0)
                style = Paint.Style.FILL
                isAntiAlias = true
            }
            canvas.drawCircle(drawX, drawY, explosionSize * 0.4f, innerPaint)
        }
    }
    
    // Check collision with target (heroes)
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
    
    // Check collision with heroes
    fun checkPlayerCollision(playerX: Float, playerY: Float): Boolean {
        if (!isActive || isExploding) return false
        
        val dx = x - playerX
        val dy = y - playerY
        val distance = sqrt(dx * dx + dy * dy)
        
        // Check if heroes is within fire breath collision radius
        if (distance <= (size + 32f)) { // 32f is half heroes size
            triggerExplosion()
            return true
        }
        
        return false
    }
}