package com.example.mygame.game.entities.enemies

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.util.Log
import com.example.mygame.game.entities.projectiles.DragonFireBreath
import com.example.mygame.game.managers.MapManager
import kotlin.math.*
import kotlin.random.Random

class Dragon(
    startX: Float,
    startY: Float,
    private val mapManager: MapManager
) {
    // Position and movement
    private var x = startX
    private var y = startY
    private var velocityX = 0f
    private var velocityY = 0f
    private val speed = 60f // Slower than demon but stronger
    private val size = 80f // Larger than other enemies
    private val width = 128f
    private val height = 128f
    
    // Health and combat
    private var health = 100 // High health boss-like enemy
    private val maxHealth = 100
    private var lastAttackTime = 0L
    private val meleeAttackCooldown = 2000L // 2 seconds between melee attacks
    private val fireBreathCooldown = 4000L // 4 seconds between fire breath attacks
    private var lastFireBreathTime = 0L
    private val meleeAttackRange = 90f
    private val fireBreathRange = 256f // 2 tiles range
    private val meleeAttackDamage = 25 // High melee damage
    private var lastAttackDamage = 0
    
    // AI and behavior
    private var lastDirectionChangeTime = 0L
    private val directionChangeInterval = 3000L // Change direction every 3 seconds
    private var targetDirectionX = 0f
    private var targetDirectionY = 0f
    private val aggroRange = 400f // Large detection range
    private val pursuitRange = 600f // Large chase range
    
    // Fire breath projectiles
    private val fireBreaths = mutableListOf<DragonFireBreath>()
    
    // Animation states
    enum class DragonState {
        IDLE,
        WALKING,
        MELEE_ATTACKING,
        FIRE_BREATHING,
        HURT,
        DEAD
    }
    
    private var currentState = DragonState.IDLE
    private var animationFrame = 0
    private var lastAnimationTime = 0L
    private val animationSpeed = 200L // Slower animation for majestic dragon
    
    // Textures
    private var walkFrames = mutableListOf<Bitmap>()
    private var idleFrames = mutableListOf<Bitmap>()
    private var attackFrames = mutableListOf<Bitmap>()
    private var fireBreathFrames = mutableListOf<Bitmap>()
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
        fireBreathTextures: List<Bitmap>,
        hurtTextures: List<Bitmap>,
        deadTextures: List<Bitmap>
    ) {
        walkFrames.clear()
        idleFrames.clear()
        attackFrames.clear()
        fireBreathFrames.clear()
        hurtFrames.clear()
        deadFrames.clear()
        
        walkFrames.addAll(walkTextures)
        idleFrames.addAll(idleTextures)
        attackFrames.addAll(attackTextures)
        fireBreathFrames.addAll(fireBreathTextures)
        hurtFrames.addAll(hurtTextures)
        deadFrames.addAll(deadTextures)
        
        texturesLoaded = true
        Log.d("Dragon", "Textures loaded: ${walkFrames.size} walk, ${idleFrames.size} idle, ${attackFrames.size} attack, ${fireBreathFrames.size} firebreath")
    }
    
    fun update(deltaTime: Float, playerX: Float, playerY: Float, playerInLight: Boolean) {
        if (isDead) {
            updateAnimation()
            updateFireBreaths(deltaTime)
            return
        }
        
        val currentTime = System.currentTimeMillis()
        
        // Calculate distance to player
        val dx = playerX - x
        val dy = playerY - y
        val distanceToPlayer = sqrt(dx * dx + dy * dy)
        
        // AI Decision Making - Dragon is intelligent and strategic
        when {
            // Fire breath attack (ranged priority)
            distanceToPlayer <= fireBreathRange && 
            distanceToPlayer > meleeAttackRange && 
            currentTime - lastFireBreathTime > fireBreathCooldown -> {
                performFireBreathAttack(playerX, playerY)
            }
            // Melee attack
            distanceToPlayer <= meleeAttackRange && 
            currentTime - lastAttackTime > meleeAttackCooldown -> {
                performMeleeAttack(playerX, playerY)
            }
            // Chase player
            distanceToPlayer <= pursuitRange -> {
                chasePlayer(playerX, playerY, deltaTime)
            }
            // Move towards player if detected
            distanceToPlayer <= aggroRange -> {
                moveTowardsPlayer(playerX, playerY, deltaTime)
            }
            else -> {
                // Patrol randomly
                randomPatrol(deltaTime, currentTime)
            }
        }
        
        // Apply movement
        applyMovement(deltaTime)
        
        // Update fire breath projectiles
        updateFireBreaths(deltaTime)
        
        // Update animation
        updateAnimation()
    }
    
    private fun performMeleeAttack(playerX: Float, playerY: Float) {
        currentState = DragonState.MELEE_ATTACKING
        lastAttackTime = System.currentTimeMillis()
        lastAttackDamage = meleeAttackDamage
        animationFrame = 0 // Reset attack animation
        
        Log.d("Dragon", "Dragon performs melee attack for $meleeAttackDamage damage!")
    }
    
    private fun performFireBreathAttack(playerX: Float, playerY: Float) {
        currentState = DragonState.FIRE_BREATHING
        lastFireBreathTime = System.currentTimeMillis()
        animationFrame = 0 // Reset fire breath animation
        
        // Create fire breath projectile
        val fireBreath = DragonFireBreath(x, y, playerX, playerY, mapManager)
        fireBreaths.add(fireBreath)
        
        Log.d("Dragon", "Dragon breathes fire at player!")
    }
    
    private fun chasePlayer(playerX: Float, playerY: Float, deltaTime: Float) {
        currentState = DragonState.WALKING
        
        val dx = playerX - x
        val dy = playerY - y
        val distance = sqrt(dx * dx + dy * dy)
        
        if (distance > 0) {
            velocityX = (dx / distance) * speed
            velocityY = (dy / distance) * speed
        }
    }
    
    private fun moveTowardsPlayer(playerX: Float, playerY: Float, deltaTime: Float) {
        currentState = DragonState.WALKING
        
        val dx = playerX - x
        val dy = playerY - y
        val distance = sqrt(dx * dx + dy * dy)
        
        if (distance > 0) {
            // Move slower when approaching for strategic positioning
            velocityX = (dx / distance) * speed * 0.6f
            velocityY = (dy / distance) * speed * 0.6f
        }
    }
    
    private fun randomPatrol(deltaTime: Float, currentTime: Long) {
        // Change direction periodically
        if (currentTime - lastDirectionChangeTime > directionChangeInterval) {
            generateRandomDirection()
            lastDirectionChangeTime = currentTime
        }
        
        if (targetDirectionX != 0f || targetDirectionY != 0f) {
            currentState = DragonState.WALKING
            velocityX = targetDirectionX * speed * 0.4f // Slow patrol speed
            velocityY = targetDirectionY * speed * 0.4f
        } else {
            currentState = DragonState.IDLE
            velocityX = 0f
            velocityY = 0f
        }
    }
    
    private fun generateRandomDirection() {
        val angle = Random.nextFloat() * 2 * PI
        targetDirectionX = cos(angle).toFloat()
        targetDirectionY = sin(angle).toFloat()
        
        // 40% chance to stop and idle (Dragons are more contemplative)
        if (Random.nextFloat() < 0.4f) {
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
    
    private fun updateFireBreaths(deltaTime: Float) {
        // Update all fire breath projectiles
        val iterator = fireBreaths.iterator()
        while (iterator.hasNext()) {
            val fireBreath = iterator.next()
            fireBreath.update(deltaTime)
            
            // Remove inactive fire breaths
            if (!fireBreath.isActive()) {
                iterator.remove()
            }
        }
    }
    
    private fun updateAnimation() {
        val currentTime = System.currentTimeMillis()
        
        if (currentTime - lastAnimationTime > animationSpeed) {
            animationFrame++
            lastAnimationTime = currentTime
            
            // Reset animation frame based on current state
            val maxFrames = when (currentState) {
                DragonState.IDLE -> idleFrames.size
                DragonState.WALKING -> walkFrames.size
                DragonState.MELEE_ATTACKING -> attackFrames.size
                DragonState.FIRE_BREATHING -> fireBreathFrames.size
                DragonState.HURT -> hurtFrames.size
                DragonState.DEAD -> deadFrames.size
            }
            
            if (maxFrames > 0 && animationFrame >= maxFrames) {
                animationFrame = 0
                
                // Reset state after attack animations
                if (currentState == DragonState.MELEE_ATTACKING) {
                    currentState = DragonState.IDLE
                }
                
                if (currentState == DragonState.FIRE_BREATHING) {
                    currentState = DragonState.IDLE
                }
                
                // Reset state after hurt animation
                if (currentState == DragonState.HURT) {
                    currentState = DragonState.IDLE
                }
            }
        }
    }
    
    fun takeDamage(damage: Int) {
        if (isDead) return
        
        health -= damage
        currentState = DragonState.HURT
        animationFrame = 0
        
        Log.d("Dragon", "Dragon took $damage damage, health: $health")
        
        if (health <= 0) {
            health = 0
            isDead = true
            currentState = DragonState.DEAD
            deathTime = System.currentTimeMillis()
            animationFrame = 0
            // Clear all fire breaths when dragon dies
            fireBreaths.clear()
            Log.d("Dragon", "Dragon died!")
        }
    }
    
    fun draw(canvas: Canvas, cameraX: Float, cameraY: Float) {
        // Draw fire breath projectiles first (behind dragon)
        fireBreaths.forEach { fireBreath ->
            fireBreath.draw(canvas, cameraX, cameraY)
        }
        
        if (!texturesLoaded) return
        
        val drawX = x - cameraX - width / 2
        val drawY = y - cameraY - height / 2
        
        // Get current animation frame
        val frames = when (currentState) {
            DragonState.IDLE -> idleFrames
            DragonState.WALKING -> walkFrames
            DragonState.MELEE_ATTACKING -> attackFrames
            DragonState.FIRE_BREATHING -> fireBreathFrames
            DragonState.HURT -> hurtFrames
            DragonState.DEAD -> deadFrames
        }
        
        if (frames.isNotEmpty()) {
            val frameIndex = animationFrame.coerceIn(0, frames.size - 1)
            val currentFrame = frames[frameIndex]
            canvas.drawBitmap(currentFrame, drawX, drawY, null)
        }
    }
    
    // Check fire breath collisions with player
    fun checkFireBreathCollisions(playerX: Float, playerY: Float, playerSize: Float): Int {
        var totalDamage = 0
        
        fireBreaths.forEach { fireBreath ->
            if (fireBreath.checkCollision(playerX, playerY, playerSize)) {
                fireBreath.triggerExplosion()
                totalDamage += fireBreath.getDamage()
            } else if (fireBreath.checkExplosionCollision(playerX, playerY, playerSize)) {
                totalDamage += fireBreath.getDamage() / 2 // Reduced explosion damage
            }
        }
        
        return totalDamage
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
    fun getFireBreaths(): List<DragonFireBreath> = fireBreaths.toList()
    
    fun getAndClearLastAttackDamage(): Int {
        val damage = lastAttackDamage
        lastAttackDamage = 0
        return damage
    }
    
    // Get new projectiles created this frame and clear the list
    fun getNewProjectiles(): List<DragonFireBreath> {
        val newProjectiles = fireBreaths.toList()
        fireBreaths.clear()
        return newProjectiles
    }
}