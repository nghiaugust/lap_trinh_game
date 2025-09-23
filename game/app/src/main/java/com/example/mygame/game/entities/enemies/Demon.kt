package com.example.mygame.game.entities.enemies

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.util.Log
import com.example.mygame.game.managers.MapManager
import kotlin.math.*
import kotlin.random.Random

class Demon(
    startX: Float,
    startY: Float,
    private val mapManager: MapManager
) {
    // Position and movement
    private var x = startX
    private var y = startY
    private var velocityX = 0f
    private var velocityY = 0f
    private val speed = 100f // Faster than skeleton
    private val size = 64f
    private val width = 256f
    private val height = 256f
    
    // Health and combat
    private var health = 50 // More health than skeleton
    private val maxHealth = 50
    private var lastAttackTime = 0L
    private val attackCooldown = 1800L // 1.8 seconds between attacks
    private val attackRange = 80f // Melee range
    private val attackDamage = 15 // Higher damage than skeleton
    private var lastAttackDamage = 0
    
    // AI and behavior
    private var lastDirectionChangeTime = 0L
    private val directionChangeInterval = 2000L // Change direction every 2 seconds
    private var targetDirectionX = 0f
    private var targetDirectionY = 0f
    private val aggroRange = 300f // Detection range
    private val pursuitRange = 500f // Chase range
    
    // Animation states
    enum class DemonState {
        IDLE,
        WALKING,
        ATTACKING,
        HURT,
        DEAD
    }
    
    private var currentState = DemonState.IDLE
    private var animationFrame = 0
    private var lastAnimationTime = 0L
    private val animationSpeed = 200L // Slower animation for better performance (was 150L)
    
    // Textures
    private var walkFrames = mutableListOf<Bitmap>()
    private var idleFrames = mutableListOf<Bitmap>()
    private var attackFrames = mutableListOf<Bitmap>()
    private var hurtFrames = mutableListOf<Bitmap>()
    private var deadFrames = mutableListOf<Bitmap>()
    private var texturesLoaded = false
    
    // Death tracking
    private var deathTime = 0L
    private var isDead = false
    
    init {
        // Initialize random direction
        generateRandomDirection()
    }
    
    fun loadTextures(
        walkTextures: List<Bitmap>,
        idleTextures: List<Bitmap>,
        attackTextures: List<Bitmap>,
        hurtTextures: List<Bitmap>,
        deadTextures: List<Bitmap>
    ) {
        walkFrames.clear()
        idleFrames.clear()
        attackFrames.clear()
        hurtFrames.clear()
        deadFrames.clear()
        
        walkFrames.addAll(walkTextures)
        idleFrames.addAll(idleTextures)
        attackFrames.addAll(attackTextures)
        hurtFrames.addAll(hurtTextures)
        deadFrames.addAll(deadTextures)
        
        texturesLoaded = true
        Log.d("Demon", "Textures loaded: ${walkFrames.size} walk, ${idleFrames.size} idle, ${attackFrames.size} attack")
    }
    
    fun update(deltaTime: Float, playerX: Float, playerY: Float, playerInLight: Boolean) {
        if (isDead) {
            updateAnimation()
            return
        }
        
        val currentTime = System.currentTimeMillis()
        
        // Calculate distance to heroes
        val dx = playerX - x
        val dy = playerY - y
        val distanceToPlayer = sqrt(dx * dx + dy * dy)
        
        // AI Decision Making
        when {
            distanceToPlayer <= attackRange && currentTime - lastAttackTime > attackCooldown -> {
                // Attack heroes
                performAttack(playerX, playerY)
            }
            distanceToPlayer <= pursuitRange -> {
                // Chase heroes
                chasePlayer(playerX, playerY, deltaTime)
            }
            distanceToPlayer <= aggroRange -> {
                // Move towards heroes
                moveTowardsPlayer(playerX, playerY, deltaTime)
            }
            else -> {
                // Patrol randomly
                randomPatrol(deltaTime, currentTime)
            }
        }
        
        // Apply movement
        applyMovement(deltaTime)
        
        // Update animation
        updateAnimation()
    }
    
    private fun performAttack(playerX: Float, playerY: Float) {
        currentState = DemonState.ATTACKING
        lastAttackTime = System.currentTimeMillis()
        lastAttackDamage = attackDamage
        animationFrame = 0 // Reset attack animation
        
        Log.d("Demon", "Demon attacks heroes for $attackDamage damage!")
    }
    
    private fun chasePlayer(playerX: Float, playerY: Float, deltaTime: Float) {
        currentState = DemonState.WALKING
        
        val dx = playerX - x
        val dy = playerY - y
        val distance = sqrt(dx * dx + dy * dy)
        
        if (distance > 0) {
            velocityX = (dx / distance) * speed
            velocityY = (dy / distance) * speed
        }
    }
    
    private fun moveTowardsPlayer(playerX: Float, playerY: Float, deltaTime: Float) {
        currentState = DemonState.WALKING
        
        val dx = playerX - x
        val dy = playerY - y
        val distance = sqrt(dx * dx + dy * dy)
        
        if (distance > 0) {
            // Move slower when approaching
            velocityX = (dx / distance) * speed * 0.7f
            velocityY = (dy / distance) * speed * 0.7f
        }
    }
    
    private fun randomPatrol(deltaTime: Float, currentTime: Long) {
        // Change direction periodically
        if (currentTime - lastDirectionChangeTime > directionChangeInterval) {
            generateRandomDirection()
            lastDirectionChangeTime = currentTime
        }
        
        if (targetDirectionX != 0f || targetDirectionY != 0f) {
            currentState = DemonState.WALKING
            velocityX = targetDirectionX * speed * 0.5f // Slower patrol speed
            velocityY = targetDirectionY * speed * 0.5f
        } else {
            currentState = DemonState.IDLE
            velocityX = 0f
            velocityY = 0f
        }
    }
    
    private fun generateRandomDirection() {
        val angle = Random.nextFloat() * 2 * PI
        targetDirectionX = cos(angle).toFloat()
        targetDirectionY = sin(angle).toFloat()
        
        // 30% chance to stop and idle
        if (Random.nextFloat() < 0.3f) {
            targetDirectionX = 0f
            targetDirectionY = 0f
        }
    }
    
    private fun applyMovement(deltaTime: Float) {
        val newX = x + velocityX * deltaTime
        val newY = y + velocityY * deltaTime
        
        // Check collision with walls
        if (mapManager.canMoveTo(newX, y, width, height)) {
            x = newX
        } else {
            velocityX = 0f
            // Change direction on wall collision
            generateRandomDirection()
        }
        
        if (mapManager.canMoveTo(x, newY, width, height)) {
            y = newY
        } else {
            velocityY = 0f
            // Change direction on wall collision
            generateRandomDirection()
        }
    }
    
    private fun updateAnimation() {
        val currentTime = System.currentTimeMillis()
        
        if (currentTime - lastAnimationTime > animationSpeed) {
            animationFrame++
            lastAnimationTime = currentTime
            
            // Reset animation frame based on current state
            val maxFrames = when (currentState) {
                DemonState.IDLE -> idleFrames.size
                DemonState.WALKING -> walkFrames.size
                DemonState.ATTACKING -> attackFrames.size
                DemonState.HURT -> hurtFrames.size
                DemonState.DEAD -> deadFrames.size
            }
            
            if (maxFrames > 0 && animationFrame >= maxFrames) {
                animationFrame = 0
                
                // Reset state after attack animation
                if (currentState == DemonState.ATTACKING) {
                    currentState = DemonState.IDLE
                }
                
                // Reset state after hurt animation
                if (currentState == DemonState.HURT) {
                    currentState = DemonState.IDLE
                }
            }
        }
    }
    
    fun takeDamage(damage: Int) {
        if (isDead) return
        
        health -= damage
        currentState = DemonState.HURT
        animationFrame = 0
        
        Log.d("Demon", "Demon took $damage damage, health: $health")
        
        if (health <= 0) {
            health = 0
            isDead = true
            currentState = DemonState.DEAD
            deathTime = System.currentTimeMillis()
            animationFrame = 0
            Log.d("Demon", "Demon died!")
        }
    }
    
    fun draw(canvas: Canvas, cameraX: Float, cameraY: Float) {
        if (!texturesLoaded) return
        
        val screenX = x - cameraX
        val screenY = y - cameraY
        
        // Get current animation frame
        val frames = when (currentState) {
            DemonState.IDLE -> idleFrames
            DemonState.WALKING -> walkFrames
            DemonState.ATTACKING -> attackFrames
            DemonState.HURT -> hurtFrames
            DemonState.DEAD -> deadFrames
        }
        
        if (frames.isNotEmpty()) {
            val frameIndex = animationFrame.coerceIn(0, frames.size - 1)
            val currentFrame = frames[frameIndex]
            
            // Create destination rectangle for scaling (same approach as Skeleton)
            val destRect = android.graphics.RectF(
                screenX - width / 2f,
                screenY - height / 2f,
                screenX + width / 2f,
                screenY + height / 2f
            )
            
            canvas.drawBitmap(currentFrame, null, destRect, null)
        }
    }
    
    // Getters
    fun getX(): Float = x
    fun getY(): Float = y
    fun getWidth(): Float = width
    fun getHeight(): Float = height
    fun getHealth(): Int = health
    fun getMaxHealth(): Int = maxHealth
    fun isAlive(): Boolean = !isDead
    fun isDead(): Boolean = isDead
    fun getDeathTime(): Long = deathTime
    
    fun getAndClearLastAttackDamage(): Int {
        val damage = lastAttackDamage
        lastAttackDamage = 0
        return damage
    }
}