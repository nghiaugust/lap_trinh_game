package com.example.mygame.game.entities

import android.graphics.Canvas
import android.util.Log
import com.example.mygame.game.assets.GameAssetManager
import com.example.mygame.game.managers.MapManager

/**
 * Base Hero Class - Abstract class for all hero characters
 * Provides common functionality and interface that all heroes must implement
 */
abstract class BaseHero(startX: Float, startY: Float) {
    
    // Position and movement - (x, y) represents the CENTER of the hero
    protected var x = startX
    protected var y = startY
    protected var velocityX = 0f
    protected var velocityY = 0f
    protected var isMoving = false
    
    // Health system
    protected var maxHealth = 100f
    protected var currentHealth = maxHealth
    protected var isDead = false
    
    // Armor system
    protected var maxArmor = 100f
    protected var currentArmor = maxArmor
    
    // Size (can be overridden in subclasses)
    protected open val width = 256f
    protected open val height = 256f
    
    // Movement speed (can be overridden in subclasses)
    protected open val speed = 8f
    
    // Public accessors for protected properties
    val positionX: Float get() = x
    val positionY: Float get() = y
    val heroWidth: Float get() = width
    val heroHeight: Float get() = height
    val health: Float get() = currentHealth
    val maxHeroHealth: Float get() = maxHealth
    val armor: Float get() = currentArmor
    val maxHeroArmor: Float get() = maxArmor
    val isHeroDead: Boolean get() = isDead
    val heroMoving: Boolean get() = isMoving
    val heroVelocityX: Float get() = velocityX
    val heroVelocityY: Float get() = velocityY
    
    // Direction enum
    enum class Direction {
        FRONT, BACK, LEFT, RIGHT
    }
    
    protected var facingDirection = Direction.FRONT
    
    // Abstract methods that must be implemented by all heroes
    abstract fun update(deltaTime: Float, worldWidth: Float, worldHeight: Float, mapManager: MapManager? = null)
    abstract fun render(canvas: Canvas, cameraX: Float, cameraY: Float)
    abstract fun loadTextures(assetManager: GameAssetManager)
    abstract fun performPrimaryAttack(): Boolean
    abstract fun performSecondaryAbility(): Boolean
    
    // Movement methods with default implementation
    open fun setMovementDirection(deltaX: Float, deltaY: Float) {
        val length = Math.sqrt((deltaX * deltaX + deltaY * deltaY).toDouble()).toFloat()
        
        if (length > 0) {
            velocityX = (deltaX / length) * speed
            velocityY = (deltaY / length) * speed
            isMoving = true
            
            // Keep essential movement logging only
            // Log.d("BaseHero", "Movement set: velocity($velocityX, $velocityY), speed=$speed")
            
            // Determine facing direction based on movement
            facingDirection = when {
                Math.abs(deltaX) > Math.abs(deltaY) -> {
                    if (deltaX > 0) Direction.RIGHT else Direction.LEFT
                }
                deltaY > 0 -> Direction.FRONT
                deltaY < 0 -> Direction.BACK
                else -> facingDirection // Keep current direction if no clear direction
            }
            
            // Reduced logging frequency (disabled to prevent spam)
            // if (System.currentTimeMillis() % 1000 < 50) {
            //     Log.d("BaseHero", "Movement direction set: velocity($velocityX, $velocityY), facing: $facingDirection")
            // }
        }
    }
    
    open fun stopMovement() {
        velocityX = 0f
        velocityY = 0f
        isMoving = false
        // Log.d("BaseHero", "Movement stopped") // Disabled to prevent spam
    }
    
    // Health system with default implementation
    open fun takeDamage(damage: Float): Boolean {
        if (isDead) return false
        
        var remainingDamage = damage
        
        // Armor absorbs damage first
        if (currentArmor > 0) {
            val armorAbsorbed = Math.min(currentArmor, remainingDamage)
            currentArmor -= armorAbsorbed
            remainingDamage -= armorAbsorbed
            Log.d("BaseHero", "Armor absorbed $armorAbsorbed damage, armor: $currentArmor/$maxArmor")
        }
        
        // Remaining damage affects health
        if (remainingDamage > 0) {
            currentHealth -= remainingDamage
            Log.d("BaseHero", "Took $remainingDamage health damage, health: $currentHealth/$maxHealth")
        }
        
        if (currentHealth <= 0) {
            currentHealth = 0f
            isDead = true
            Log.d("BaseHero", "Hero died!")
            return true // Hero died
        }
        return false // Hero survived
    }
    
    open fun heal(amount: Float) {
        if (isDead) return
        currentHealth = Math.min(currentHealth + amount, maxHealth)
        Log.d("BaseHero", "Healed $amount, health: $currentHealth/$maxHealth")
    }
    
    open fun repairArmor(amount: Float) {
        if (isDead) return
        currentArmor = Math.min(currentArmor + amount, maxArmor)
        Log.d("BaseHero", "Repaired armor $amount, armor: $currentArmor/$maxArmor")
    }
    
    open fun reset() {
        currentHealth = maxHealth
        currentArmor = maxArmor
        isDead = false
        isMoving = false
        velocityX = 0f
        velocityY = 0f
        facingDirection = Direction.FRONT
        Log.d("BaseHero", "Hero reset")
    }
    
    // Common getters (properties automatically provide getters in Kotlin)
    // x, y, width, height, currentHealth, maxHealth, isDead, isMoving, 
    // facingDirection, velocityX, velocityY are all accessible directly
    
    // Common setters
    open fun setPosition(newX: Float, newY: Float) {
        x = newX
        y = newY
        Log.d("BaseHero", "Position set to ($x, $y)")
    }
    
    // Helper method for boundary checking
    protected fun constrainToBounds(worldWidth: Float, worldHeight: Float) {
        val halfWidth = width / 2f
        val halfHeight = height / 2f
        
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
    
    // Helper method for position update with movement
    protected fun updatePosition(deltaTime: Float) {
        if (isMoving) {
            x += velocityX * deltaTime * 60f // Convert to 60 FPS equivalent
            y += velocityY * deltaTime * 60f
        }
    }
    
    // Helper method for position update with collision checking
    protected fun updatePositionWithCollision(deltaTime: Float, mapManager: MapManager?) {
        if (!isMoving) {
            return
        }
        
        // Use smaller movement step to prevent large jumps and improve collision detection
        val deltaX = velocityX * deltaTime * 30f  // Reduced from 60f to 30f for better collision
        val deltaY = velocityY * deltaTime * 30f
        
        // Debug logging - reduced frequency
        // Disabled to prevent spam
        // if (System.currentTimeMillis() % 1000 < 50) { // Log only occasionally
        //     Log.d("BaseHero", "Movement: velocity($velocityX, $velocityY), delta($deltaX, $deltaY), pos($x, $y)")
        // }
        
        // If no mapManager, just move freely
        if (mapManager == null) {
            x += deltaX
            y += deltaY
            // Log.d("BaseHero", "Free movement to ($x, $y)") // Disabled to prevent spam
            return
        }
        
        // Check if hero is currently in a valid position
        if (!mapManager.canMoveTo(x, y, width, height)) {
            Log.w("BaseHero", "Hero stuck in wall! Attempting to free...")
            // Try to find a nearby valid position
            if (tryToFreeFromWall(mapManager)) {
                Log.d("BaseHero", "Hero freed from wall to ($x, $y)")
            } else {
                Log.e("BaseHero", "Could not free hero from wall!")
                return // Don't try to move if stuck
            }
        }
        
        // Store original position
        val originalX = x
        val originalY = y
        
        // Calculate potential new positions
        val newX = x + deltaX
        val newY = y + deltaY
        
        // Try to move to new position first (both X and Y)
        if (mapManager.canMoveTo(newX, newY, width, height)) {
            x = newX
            y = newY
            // Log.d("BaseHero", "Full movement to ($x, $y)") // Disabled to prevent spam
            return
        }
        
        // If full movement blocked, try each axis separately
        var movedX = false
        var movedY = false
        
        // Try X movement only
        if (mapManager.canMoveTo(newX, originalY, width, height)) {
            x = newX
            movedX = true
            // Log.d("BaseHero", "X movement only to ($x, $y)") // Disabled to prevent spam
        }
        
        // Try Y movement only (use current X position, which may have changed)
        if (mapManager.canMoveTo(x, newY, width, height)) {
            y = newY
            movedY = true
            // Log.d("BaseHero", "Y movement only to ($x, $y)") // Disabled to prevent spam
        }
        
        // Log movement result (disabled to prevent spam)
        // if (System.currentTimeMillis() % 1000 < 50) {
        //     if (!movedX && !movedY) {
        //         Log.d("BaseHero", "No movement possible - collision on both axes")
        //     } else if (movedX && movedY) {
        //         Log.d("BaseHero", "Both axes movement successful")
        //     } else if (movedX) {
        //         Log.d("BaseHero", "Only X movement successful")
        //     } else {
        //         Log.d("BaseHero", "Only Y movement successful")
        //     }
        //     Log.d("BaseHero", "Final position: ($x, $y)")
        // }
    }
    
    // Helper method to free hero from wall
    private fun tryToFreeFromWall(mapManager: MapManager): Boolean {
        val searchRadius = 32f // Search within 32 pixels
        val step = 8f // Try positions every 8 pixels
        
        // Try positions in a spiral pattern around current position
        for (radius in step.toInt()..searchRadius.toInt() step step.toInt()) {
            val offsets = listOf(
                Pair(-radius.toFloat(), 0f),
                Pair(radius.toFloat(), 0f),
                Pair(0f, -radius.toFloat()),
                Pair(0f, radius.toFloat()),
                Pair(-radius.toFloat(), -radius.toFloat()),
                Pair(radius.toFloat(), -radius.toFloat()),
                Pair(-radius.toFloat(), radius.toFloat()),
                Pair(radius.toFloat(), radius.toFloat())
            )
            
            for ((offsetX, offsetY) in offsets) {
                val testX = x + offsetX
                val testY = y + offsetY
                
                if (mapManager.canMoveTo(testX, testY, width, height)) {
                    x = testX
                    y = testY
                    return true
                }
            }
        }
        return false
    }
    
    // Abstract method for hero-specific abilities/skills info
    abstract fun getHeroInfo(): HeroInfo
    
    // Data class for hero information
    data class HeroInfo(
        val name: String,
        val description: String,
        val primaryAttackName: String,
        val secondaryAbilityName: String,
        val maxHealth: Float,
        val speed: Float
    )
}