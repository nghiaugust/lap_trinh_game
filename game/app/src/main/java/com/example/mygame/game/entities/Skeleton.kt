package com.example.mygame.game.entities

import android.graphics.*
import android.util.Log
import com.example.mygame.game.managers.MapManager
import kotlin.math.*
import kotlin.random.Random

class Skeleton(
    startX: Float,
    startY: Float,
    private val mapManager: MapManager
) {
    // Position and movement
    private var x = startX
    private var y = startY
    private var velocityX = 0f
    private var velocityY = 0f
    private val speed = 90f // Increased speed for larger character size, still slower than player
    
    // AI States
    enum class State {
        IDLE,
        PATROL,
        CHASE,
        ATTACK,
        HURT,
        DEAD
    }
    
    private var currentState = State.PATROL
    private var lastStateChange = System.currentTimeMillis()
    
    // Combat properties - adjusted for larger character sizes
    private var health = 100 // Tăng từ 3 lên 100
    private var maxHealth = 100 // Tăng từ 3 lên 100
    private var attackDamage = 20 // Damage to player armor
    private var attackRange = 160f // Increased melee range for larger characters
    private var attackCooldown = 2000L // 2 seconds between attacks
    private var lastAttackTime = 0L
    private var detectionRange = 600f // Increased detection range for larger world
    
    // Animation properties
    private var currentFrame = 0
    private var animationTimer = 0f
    private val frameTime = 0.1f // 100ms per frame
    
    // Skeleton size - increased for better visibility
    private val skeletonWidth = 128f
    private val skeletonHeight = 128f
    
    // Animation frames for different states
    private var walkFrames = mutableListOf<Bitmap>()
    private var idleFrames = mutableListOf<Bitmap>()
    private var attackFrames = mutableListOf<Bitmap>()
    private var hurtFrames = mutableListOf<Bitmap>()
    private var deadFrames = mutableListOf<Bitmap>()
    
    // Patrol behavior
    private var patrolTargetX = startX
    private var patrolTargetY = startY
    private var patrolTimer = 0f
    private val patrolInterval = 3000f // Change direction every 3 seconds
    
    // Direction tracking
    private var facingRight = true
    
    // Player reference for chasing
    private var targetPlayerX = 0f
    private var targetPlayerY = 0f
    
    // Damage tracking for EnemyManager
    private var lastAttackDamageDealt = 0
    private var damageDealtTime = 0L
    private val paint = Paint().apply {
        isAntiAlias = true
        isFilterBitmap = true
    }
    
    // Hurt flash effect
    private var hurtFlashTimer = 0f
    private val hurtFlashDuration = 200f
    private var isFlashing = false
    
    // Attack animation control
    private var isPerformingAttack = false
    private var attackAnimationStartTime = 0L
    private val attackAnimationDuration = 600L // 600ms for attack animation
    private var hasDealtDamageThisAttack = false
    
    init {
        generateNewPatrolTarget()
        Log.d("Skeleton", "Skeleton created at ($x, $y)")
    }
    
    fun loadTextures(walkFrames: List<Bitmap>, idleFrames: List<Bitmap>, attackFrames: List<Bitmap>, hurtFrames: List<Bitmap>, deadFrames: List<Bitmap>) {
        this.walkFrames.clear()
        this.idleFrames.clear()
        this.attackFrames.clear()
        this.hurtFrames.clear()
        this.deadFrames.clear()
        
        this.walkFrames.addAll(walkFrames)
        this.idleFrames.addAll(idleFrames)
        this.attackFrames.addAll(attackFrames)
        this.hurtFrames.addAll(hurtFrames)
        this.deadFrames.addAll(deadFrames)
        
        Log.d("Skeleton", "Loaded ${walkFrames.size} walk, ${idleFrames.size} idle, ${attackFrames.size} attack, ${hurtFrames.size} hurt, ${deadFrames.size} death frames")
    }
    
    fun update(deltaTime: Float, playerX: Float, playerY: Float, playerInLight: Boolean) {
        if (currentState == State.DEAD) {
            updateDeathAnimation(deltaTime)
            return
        }
        
        // Update hurt flash effect
        if (isFlashing) {
            hurtFlashTimer += deltaTime * 1000f
            if (hurtFlashTimer >= hurtFlashDuration) {
                isFlashing = false
                hurtFlashTimer = 0f
            }
        }
        
        // Store player position for AI
        targetPlayerX = playerX
        targetPlayerY = playerY
        
        // Update animation timer
        animationTimer += deltaTime
        
        // AI State Machine
        when (currentState) {
            State.IDLE -> updateIdle(deltaTime)
            State.PATROL -> updatePatrol(deltaTime)
            State.CHASE -> updateChase(deltaTime, playerX, playerY, playerInLight)
            State.ATTACK -> updateAttack(deltaTime, playerX, playerY)
            State.HURT -> updateHurt(deltaTime)
            State.DEAD -> updateDeathAnimation(deltaTime)
        }
        
        // Check if should transition to chase state
        if (currentState == State.PATROL || currentState == State.IDLE) {
            if (playerInLight && canSeePlayer(playerX, playerY)) {
                changeState(State.CHASE)
            }
        }
        
        // Apply movement
        applyMovement(deltaTime)
        
        // Update animation frame
        updateAnimation()
    }
    
    private fun updateIdle(deltaTime: Float) {
        velocityX = 0f
        velocityY = 0f
        
        // After some time, start patrolling
        if (System.currentTimeMillis() - lastStateChange > 1000) {
            changeState(State.PATROL)
        }
    }
    
    private fun updatePatrol(deltaTime: Float) {
        patrolTimer += deltaTime * 1000f
        
        // Move towards patrol target
        val dx = patrolTargetX - x
        val dy = patrolTargetY - y
        val distance = sqrt(dx * dx + dy * dy)
        
        if (distance > 10f) {
            // Move towards target
            velocityX = (dx / distance) * speed * 0.5f // Patrol at half speed
            velocityY = (dy / distance) * speed * 0.5f
            
            // Update facing direction
            facingRight = dx > 0
        } else {
            // Reached target, generate new one
            generateNewPatrolTarget()
        }
        
        // Change patrol target periodically
        if (patrolTimer >= patrolInterval) {
            generateNewPatrolTarget()
            patrolTimer = 0f
        }
    }
    
    private fun updateChase(deltaTime: Float, playerX: Float, playerY: Float, playerInLight: Boolean) {
        // If player is no longer in light, return to patrol
        if (!playerInLight || !canSeePlayer(playerX, playerY)) {
            changeState(State.PATROL)
            return
        }
        
        val dx = playerX - x
        val dy = playerY - y
        val distance = sqrt(dx * dx + dy * dy)
        
        // Update facing direction
        facingRight = dx > 0
        
        if (distance <= attackRange) {
            // Close enough to attack
            changeState(State.ATTACK)
        } else {
            // Chase player
            velocityX = (dx / distance) * speed
            velocityY = (dy / distance) * speed
        }
    }
    
    private fun updateAttack(deltaTime: Float, playerX: Float, playerY: Float) {
        // Stop moving during attack
        velocityX = 0f
        velocityY = 0f
        
        val currentTime = System.currentTimeMillis()
        val dx = playerX - x
        val dy = playerY - y
        val distance = sqrt(dx * dx + dy * dy)
        
        // Update facing direction
        facingRight = dx > 0
        
        // Check if player is still in attack range
        if (distance > attackRange) {
            // Player moved away, chase them
            changeState(State.CHASE)
            return
        }
        
        // Handle attack animation and damage timing
        if (isPerformingAttack) {
            val attackElapsed = currentTime - attackAnimationStartTime
            
            // Deal damage at the middle of the attack animation (300ms mark)
            if (!hasDealtDamageThisAttack && attackElapsed >= attackAnimationDuration / 2) {
                performAttack(playerX, playerY)
                hasDealtDamageThisAttack = true
            }
            
            // Check if attack animation is complete
            if (attackElapsed >= attackAnimationDuration) {
                isPerformingAttack = false
                hasDealtDamageThisAttack = false
                lastAttackTime = currentTime
            }
        } else {
            // Check if we can start a new attack (cooldown)
            if (currentTime - lastAttackTime >= attackCooldown) {
                // Start attack animation
                isPerformingAttack = true
                attackAnimationStartTime = currentTime
                hasDealtDamageThisAttack = false
                currentFrame = 0 // Reset animation to start
                animationTimer = 0f
            }
        }
        // Stay in ATTACK state - don't automatically switch to CHASE
    }
    
    private fun updateHurt(deltaTime: Float) {
        // Stop moving when hurt
        velocityX = 0f
        velocityY = 0f
        
        // Return to previous state after hurt animation
        if (System.currentTimeMillis() - lastStateChange > 500) {
            if (health > 0) {
                changeState(State.CHASE) // Continue chasing after being hurt
            } else {
                changeState(State.DEAD)
            }
        }
    }
    
    private fun updateDeathAnimation(deltaTime: Float) {
        velocityX = 0f
        velocityY = 0f
        // Death animation will play once and then skeleton remains dead
    }
    
    private fun applyMovement(deltaTime: Float) {
        if (currentState == State.DEAD) return
        
        val newX = x + velocityX * deltaTime
        val newY = y + velocityY * deltaTime
        
        // Check wall collisions
        if (!mapManager.isWall(newX, y)) {
            x = newX
        } else {
            velocityX = 0f
            // If hit wall during patrol, generate new target
            if (currentState == State.PATROL) {
                generateNewPatrolTarget()
            }
        }
        
        if (!mapManager.isWall(x, newY)) {
            y = newY
        } else {
            velocityY = 0f
            // If hit wall during patrol, generate new target
            if (currentState == State.PATROL) {
                generateNewPatrolTarget()
            }
        }
    }
    
    private fun updateAnimation() {
        if (animationTimer >= frameTime) {
            when (currentState) {
                State.ATTACK -> {
                    // Only animate when actually performing an attack
                    if (isPerformingAttack && attackFrames.isNotEmpty()) {
                        currentFrame = (currentFrame + 1) % attackFrames.size
                    } else if (!isPerformingAttack) {
                        // When not attacking, use idle animation or first attack frame
                        if (idleFrames.isNotEmpty()) {
                            currentFrame = 0 // Use first idle frame
                        } else if (attackFrames.isNotEmpty()) {
                            currentFrame = 0 // Use first attack frame as idle
                        }
                    }
                }
                State.HURT -> {
                    if (hurtFrames.isNotEmpty()) {
                        currentFrame = (currentFrame + 1) % hurtFrames.size
                    }
                }
                State.DEAD -> {
                    if (deadFrames.isNotEmpty() && currentFrame < deadFrames.size - 1) {
                        currentFrame++
                    }
                }
                else -> {
                    // Use walk frames for walking, idle frames for idle
                    when (currentState) {
                        State.PATROL, State.CHASE -> {
                            if (walkFrames.isNotEmpty()) {
                                currentFrame = (currentFrame + 1) % walkFrames.size
                            } else if (attackFrames.isNotEmpty()) {
                                currentFrame = (currentFrame + 1) % attackFrames.size
                            }
                        }
                        State.IDLE -> {
                            if (idleFrames.isNotEmpty()) {
                                currentFrame = (currentFrame + 1) % idleFrames.size
                            } else if (attackFrames.isNotEmpty()) {
                                currentFrame = 0 // Use first attack frame as idle
                            }
                        }
                        else -> {
                            // Fallback to attack frames
                            if (attackFrames.isNotEmpty()) {
                                currentFrame = (currentFrame + 1) % attackFrames.size
                            }
                        }
                    }
                }
            }
            animationTimer = 0f
        }
    }
    
    private fun generateNewPatrolTarget() {
        val maxDistance = 150f
        var attempts = 0
        
        do {
            val angle = Random.nextFloat() * 2 * PI.toFloat()
            val distance = Random.nextFloat() * maxDistance
            
            patrolTargetX = x + cos(angle) * distance
            patrolTargetY = y + sin(angle) * distance
            
            attempts++
        } while (mapManager.isWall(patrolTargetX, patrolTargetY) && attempts < 10)
        
        // If all attempts failed, stay in place
        if (attempts >= 10) {
            patrolTargetX = x
            patrolTargetY = y
        }
    }
    
    private fun canSeePlayer(playerX: Float, playerY: Float): Boolean {
        val dx = playerX - x
        val dy = playerY - y
        val distance = sqrt(dx * dx + dy * dy)
        
        return distance <= detectionRange
    }
    
    private fun performAttack(playerX: Float, playerY: Float) {
        Log.d("Skeleton", "Skeleton attacks player at ($playerX, $playerY) for $attackDamage damage!")
        // Record the attack damage for EnemyManager to pick up
        lastAttackDamageDealt = attackDamage
        damageDealtTime = System.currentTimeMillis()
    }
    
    private fun changeState(newState: State) {
        if (currentState != newState) {
            Log.d("Skeleton", "State changed from $currentState to $newState")
            
            // Reset attack animation when leaving ATTACK state
            if (currentState == State.ATTACK) {
                isPerformingAttack = false
                hasDealtDamageThisAttack = false
            }
            
            currentState = newState
            lastStateChange = System.currentTimeMillis()
            currentFrame = 0 // Reset animation
            animationTimer = 0f
        }
    }
    
    fun takeDamage(damage: Int = 10) { // Default 10 damage from fireballs
        if (currentState == State.DEAD) return
        
        health -= damage
        isFlashing = true
        hurtFlashTimer = 0f
        
        Log.d("Skeleton", "Skeleton took $damage damage, health: $health/$maxHealth")
        
        if (health <= 0) {
            changeState(State.DEAD)
        } else {
            changeState(State.HURT)
        }
    }
    
    fun draw(canvas: Canvas, cameraX: Float, cameraY: Float) {
        val screenX = x - cameraX
        val screenY = y - cameraY
        
        // Don't draw if off screen
        if (screenX < -skeletonWidth || screenX > canvas.width + skeletonWidth ||
            screenY < -skeletonHeight || screenY > canvas.height + skeletonHeight) {
            return
        }
        
        val frames = when (currentState) {
            State.IDLE -> if (idleFrames.isNotEmpty()) idleFrames else if (walkFrames.isNotEmpty()) walkFrames else attackFrames
            State.PATROL, State.CHASE -> if (walkFrames.isNotEmpty()) walkFrames else attackFrames
            State.ATTACK -> attackFrames
            State.HURT -> hurtFrames
            State.DEAD -> deadFrames
        }
        
        if (frames.isNotEmpty() && currentFrame < frames.size) {
            val frame = frames[currentFrame]
            
            // Apply hurt flash effect
            val drawPaint = if (isFlashing) {
                Paint(paint).apply {
                    colorFilter = PorterDuffColorFilter(Color.RED, PorterDuff.Mode.MULTIPLY)
                }
            } else {
                paint
            }
            
            // Flip horizontally if facing left
            if (!facingRight) {
                canvas.save()
                canvas.scale(-1f, 1f, screenX, screenY)
            }
            
            val destRect = RectF(
                screenX - skeletonWidth / 2f,
                screenY - skeletonHeight / 2f,
                screenX + skeletonWidth / 2f,
                screenY + skeletonHeight / 2f
            )
            
            canvas.drawBitmap(frame, null, destRect, drawPaint)
            
            if (!facingRight) {
                canvas.restore()
            }
            
            // Draw health bar if damaged
            if (health < maxHealth && currentState != State.DEAD) {
                drawHealthBar(canvas, screenX, screenY)
            }
        }
    }
    
    private fun drawHealthBar(canvas: Canvas, screenX: Float, screenY: Float) {
        val barWidth = skeletonWidth * 1.2f // Tăng kích thước health bar
        val barHeight = 10f // Tăng độ dày
        val barY = screenY - skeletonHeight / 2f - 20f // Tăng khoảng cách từ đầu
        
        // Health bar border (black)
        val borderPaint = Paint().apply {
            color = Color.BLACK
            style = Paint.Style.STROKE
            strokeWidth = 2f
        }
        canvas.drawRect(
            screenX - barWidth / 2f,
            barY,
            screenX + barWidth / 2f,
            barY + barHeight,
            borderPaint
        )
        
        // Background (dark red)
        val bgPaint = Paint().apply {
            color = Color.argb(255, 100, 0, 0)
            style = Paint.Style.FILL
        }
        canvas.drawRect(
            screenX - barWidth / 2f,
            barY,
            screenX + barWidth / 2f,
            barY + barHeight,
            bgPaint
        )
        
        // Health (gradient from green to red based on health)
        val healthPercent = health.toFloat() / maxHealth.toFloat()
        val healthColor = when {
            healthPercent > 0.6f -> Color.GREEN
            healthPercent > 0.3f -> Color.YELLOW
            else -> Color.RED
        }
        
        val healthPaint = Paint().apply {
            color = healthColor
            style = Paint.Style.FILL
        }
        canvas.drawRect(
            screenX - barWidth / 2f,
            barY,
            screenX - barWidth / 2f + barWidth * healthPercent,
            barY + barHeight,
            healthPaint
        )
        
        // Health text
        val textPaint = Paint().apply {
            color = Color.WHITE
            textSize = 20f
            textAlign = Paint.Align.CENTER
            isAntiAlias = true
            setShadowLayer(2f, 1f, 1f, Color.BLACK)
        }
        canvas.drawText(
            "$health/$maxHealth",
            screenX,
            barY - 5f,
            textPaint
        )
    }
    
    // Getters
    fun getX(): Float = x
    fun getY(): Float = y
    fun getWidth(): Float = skeletonWidth
    fun getHeight(): Float = skeletonHeight
    fun isAlive(): Boolean = currentState != State.DEAD
    fun isDead(): Boolean = currentState == State.DEAD
    fun getCurrentState(): State = currentState
    fun getHealth(): Int = health
    fun getMaxHealth(): Int = maxHealth
    fun getAttackDamage(): Int = attackDamage
    
    // Get damage dealt since last check (for EnemyManager)
    fun getAndClearLastAttackDamage(): Int {
        val damage = lastAttackDamageDealt
        lastAttackDamageDealt = 0
        return damage
    }
    
    // Check if skeleton can attack player
    fun canAttackPlayer(playerX: Float, playerY: Float): Boolean {
        if (currentState != State.ATTACK) return false
        
        // Check attack cooldown
        val currentTime = System.currentTimeMillis()
        if (currentTime - lastAttackTime < attackCooldown) {
            return false
        }
        
        val dx = playerX - x
        val dy = playerY - y
        val distance = sqrt(dx * dx + dy * dy)
        
        return distance <= attackRange
        // Don't update lastAttackTime here - let updateAttack handle it
    }
}