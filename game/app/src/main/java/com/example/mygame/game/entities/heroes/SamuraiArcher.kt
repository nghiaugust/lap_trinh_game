package com.example.mygame.game.entities.heroes

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.util.Log
import com.example.mygame.game.assets.GameAssetManager
import com.example.mygame.game.entities.projectiles.Arrow
import com.example.mygame.game.entities.BaseHero
import com.example.mygame.game.managers.MapManager

/**
 * Samurai Archer Hero Class
 * Features:
 * - 3-hit combo attack sequence
 * - Bow shot with arrow projectiles
 * - Smooth animations for all actions
 * - Advanced combat system
 */
class SamuraiArcher(startX: Float, startY: Float) : BaseHero(startX, startY) {
    
    // Override health and stats for SamuraiArcher
    init {
        maxHealth = 150f
        currentHealth = maxHealth
        maxArmor = 120f
        currentArmor = maxArmor
    }
    
    // Override size for SamuraiArcher - reduced for better collision
    override val width = 64f   // Reduced from 256f to match actual sprite size
    override val height = 96f  // Reduced from 256f, slightly taller for character
    override val speed = 12f

    // States
    enum class HeroState {
        IDLE,
        WALKING,
        ATTACKING_1,
        ATTACKING_2, 
        ATTACKING_3,
        SHOOTING,
        HURT,
        DEAD
    }
    
    private var currentState = HeroState.IDLE
    
    // Animation - optimized for performance
    private var currentFrame = 0
    private var animationTimer = 0f
    private val animationSpeed = 0.15f // Slower animation for better performance (was 0.1f)
    
    // Combat system
    private var attackComboCount = 0
    private var lastAttackTime = 0L
    private val attackComboCooldown = 500L // 0.5 seconds between combos
    private val attackComboResetTime = 1500L // 1.5 seconds to reset combo
    private var isAttacking = false
    private var isShooting = false
    
    // Shooting system
    private var lastShotTime = 0L
    private val shotCooldown = 800L // 0.8 seconds between shots
    private val arrows = mutableListOf<Arrow>()
    
    // Animation frames storage
    private val idleFrames = mutableListOf<Bitmap>()
    private val walkFrames = mutableListOf<Bitmap>()
    private val attack1Frames = mutableListOf<Bitmap>()
    private val attack2Frames = mutableListOf<Bitmap>()
    private val attack3Frames = mutableListOf<Bitmap>()
    private val shotFrames = mutableListOf<Bitmap>()
    private val hurtFrames = mutableListOf<Bitmap>()
    private val deadFrames = mutableListOf<Bitmap>()
    
    // Arrow sprite
    private var arrowSprite: Bitmap? = null
    
    // Current animation frames
    private var currentFrames = idleFrames
    
    // Paint for rendering
    private val paint = Paint().apply {
        isAntiAlias = true
        isFilterBitmap = true
    }
    
    init {
        Log.d("SamuraiArcher", "Samurai Archer created at position ($x, $y)")
    }
    
    /**
     * Load all animation frames from assets
     */
    override fun loadTextures(assetManager: GameAssetManager) {
        try {
            Log.d("SamuraiArcher", "Loading Samurai Archer textures...")
            
            // Load idle animation (9 frames)
            idleFrames.clear()
            idleFrames.add(assetManager.loadBitmap("characters/heroes/Samurai_Archer/idle/Idle.png"))
            for (i in 1..8) {
                idleFrames.add(assetManager.loadBitmap("characters/heroes/Samurai_Archer/idle/Idle$i.png"))
            }
            
            // Load walk animation (8 frames)
            walkFrames.clear()
            walkFrames.add(assetManager.loadBitmap("characters/heroes/Samurai_Archer/walk/Walk.png"))
            for (i in 1..7) {
                walkFrames.add(assetManager.loadBitmap("characters/heroes/Samurai_Archer/walk/Walk$i.png"))
            }
            
            // Load attack combo animations (5 frames each)
            attack1Frames.clear()
            for (i in 1..5) {
                attack1Frames.add(assetManager.loadBitmap("characters/heroes/Samurai_Archer/attack/type1/Attack_$i.png"))
            }
            
            attack2Frames.clear()
            for (i in 1..5) {
                attack2Frames.add(assetManager.loadBitmap("characters/heroes/Samurai_Archer/attack/type2/Attack_$i.png"))
            }
            
            attack3Frames.clear()
            for (i in 1..5) {
                attack3Frames.add(assetManager.loadBitmap("characters/heroes/Samurai_Archer/attack/type3/Attack_$i.png"))
            }
            
            // Load shot animation (11 frames: Shot.png + Shot1.png to Shot10.png)
            shotFrames.clear()
            shotFrames.add(assetManager.loadBitmap("characters/heroes/Samurai_Archer/shot/Shot.png"))
            for (i in 1..10) {
                shotFrames.add(assetManager.loadBitmap("characters/heroes/Samurai_Archer/shot/Shot$i.png"))
            }
            
            // Load hurt animation (if available)
            hurtFrames.clear()
            try {
                hurtFrames.add(assetManager.loadBitmap("characters/heroes/Samurai_Archer/hurt/Hurt.png"))
            } catch (e: Exception) {
                Log.w("SamuraiArcher", "Hurt animation not found, using idle frame")
                if (idleFrames.isNotEmpty()) hurtFrames.add(idleFrames[0])
            }
            
            // Load dead animation (if available)
            deadFrames.clear()
            try {
                deadFrames.add(assetManager.loadBitmap("characters/heroes/Samurai_Archer/dead/Dead.png"))
            } catch (e: Exception) {
                Log.w("SamuraiArcher", "Dead animation not found, using idle frame")
                if (idleFrames.isNotEmpty()) deadFrames.add(idleFrames[0])
            }
            
            // Load arrow sprite with alternative method
            arrowSprite = assetManager.loadBitmapDirect("characters/heroes/Samurai_Archer/arrow/Arrow.png")
            if (arrowSprite != null) {
                Log.d("SamuraiArcher", "Arrow sprite loaded successfully with loadBitmapDirect")
            } else {
                Log.e("SamuraiArcher", "Failed to load arrow sprite with loadBitmapDirect!")
                // Try again with loadBitmap
                try {
                    arrowSprite = assetManager.loadBitmap("characters/heroes/Samurai_Archer/arrow/Arrow.png")
                    if (arrowSprite != null) {
                        Log.d("SamuraiArcher", "Arrow sprite loaded successfully with loadBitmap fallback")
                    }
                } catch (e: Exception) {
                    Log.e("SamuraiArcher", "Exception loading arrow sprite: ${e.message}")
                    // Create a simple programmatic arrow as fallback
                    arrowSprite = createFallbackArrowSprite()
                    Log.d("SamuraiArcher", "Created fallback arrow sprite")
                }
            }
            
            // Set initial animation
            currentFrames = idleFrames
            
            Log.d("SamuraiArcher", "Textures loaded successfully: ${idleFrames.size} idle, ${walkFrames.size} walk, ${attack1Frames.size} attack1, ${shotFrames.size} shot")
            
        } catch (e: Exception) {
            Log.e("SamuraiArcher", "Error loading textures: ${e.message}")
        }
    }
    
    /**
     * Create a simple fallback arrow sprite programmatically
     */
    private fun createFallbackArrowSprite(): Bitmap {
        val bitmap = Bitmap.createBitmap(32, 8, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        val paint = Paint().apply {
            color = 0xFF8B4513.toInt() // Brown color (saddle brown)
            style = Paint.Style.FILL
        }
        
        // Draw a simple brown arrow shape
        canvas.drawRect(0f, 3f, 28f, 5f, paint) // Arrow shaft
        
        paint.color = android.graphics.Color.GRAY
        canvas.drawRect(28f, 1f, 32f, 7f, paint) // Arrow head
        
        return bitmap
    }
    
    /**
     * Update hero logic - Override from BaseHero
     */
    override fun update(deltaTime: Float, worldWidth: Float, worldHeight: Float, mapManager: MapManager?) {
        if (isDead) return
        
        // Update position with collision checking
        updatePositionWithCollision(deltaTime, mapManager)
        
        // Constrain to world bounds using BaseHero method
        constrainToBounds(worldWidth, worldHeight)
        
        // Update movement state
        updateMovementState()
        
        // Update combat states
        updateCombatStates()
        
        // Update arrows
        updateArrows(deltaTime)
        
        // Update animation
        updateAnimation(deltaTime)
    }
    
    private fun updateMovementState() {
        if (isAttacking || isShooting) return // Can't move while attacking/shooting
        
        if (isMoving) {
            currentState = HeroState.WALKING
            currentFrames = walkFrames
        } else if (!isAttacking && !isShooting) {
            currentState = HeroState.IDLE
            currentFrames = idleFrames
        }
    }
    
    private fun updateCombatStates() {
        val currentTime = System.currentTimeMillis()
        
        // Reset attack combo if too much time has passed
        if (currentTime - lastAttackTime > attackComboResetTime) {
            attackComboCount = 0
        }
        
        // Check if attack animation is finished
        if (isAttacking && isAnimationFinished()) {
            isAttacking = false
            currentState = HeroState.IDLE
            currentFrames = idleFrames
            currentFrame = 0
        }
        
        // Check if shooting animation is finished
        if (isShooting && isAnimationFinished()) {
            isShooting = false
            currentState = HeroState.IDLE
            currentFrames = idleFrames
            currentFrame = 0
        }
    }
    
    private fun updateArrows(deltaTime: Float) {
        // Update all arrows
        arrows.removeAll { arrow ->
            arrow.update(deltaTime)
            arrow.isDestroyed() // Remove destroyed arrows
        }
    }
    
    private fun updateAnimation(deltaTime: Float) {
        animationTimer += deltaTime
        
        if (animationTimer >= animationSpeed && currentFrames.isNotEmpty()) {
            animationTimer = 0f
            currentFrame = (currentFrame + 1) % currentFrames.size
        }
    }
    
    private fun isAnimationFinished(): Boolean {
        return currentFrame >= currentFrames.size - 1
    }
    
    /**
     * Movement controls - Override from BaseHero
     */
    override fun setMovementDirection(deltaX: Float, deltaY: Float) {
        if (isAttacking || isShooting) return // Can't move while attacking/shooting
        
        // Call parent implementation
        super.setMovementDirection(deltaX, deltaY)
        
        // Reduced logging frequency
        if (System.currentTimeMillis() % 2000 < 50) {
            Log.d("SamuraiArcher", "Movement direction set: ($deltaX, $deltaY), current velocity: ($velocityX, $velocityY)")
        }
    }
    
    override fun stopMovement() {
        // Call parent implementation
        super.stopMovement()
    }
    
    /**
     * Combat system - Override from BaseHero
     */
    override fun performPrimaryAttack(): Boolean {
        return performAttack()
    }
    
    override fun performSecondaryAbility(): Boolean {
        return performShot()
    }
    
    /**
     * Samurai-specific attack method
     */
    fun performAttack(): Boolean {
        if (isAttacking || isShooting || isDead) return false
        
        val currentTime = System.currentTimeMillis()
        if (currentTime - lastAttackTime < attackComboCooldown) return false
        
        isAttacking = true
        lastAttackTime = currentTime
        currentFrame = 0
        
        // Determine which attack in combo
        when (attackComboCount % 3) {
            0 -> {
                currentState = HeroState.ATTACKING_1
                currentFrames = attack1Frames
                Log.d("SamuraiArcher", "Performing attack combo 1")
            }
            1 -> {
                currentState = HeroState.ATTACKING_2
                currentFrames = attack2Frames
                Log.d("SamuraiArcher", "Performing attack combo 2")
            }
            2 -> {
                currentState = HeroState.ATTACKING_3
                currentFrames = attack3Frames
                Log.d("SamuraiArcher", "Performing attack combo 3")
            }
        }
        
        attackComboCount++
        return true
    }
    
    fun performShot(): Boolean {
        if (isAttacking || isShooting || isDead) return false
        
        val currentTime = System.currentTimeMillis()
        if (currentTime - lastShotTime < shotCooldown) return false
        
        isShooting = true
        lastShotTime = currentTime
        currentFrame = 0
        currentState = HeroState.SHOOTING
        currentFrames = shotFrames
        
        // Create arrow projectile
        createArrow()
        
        Log.d("SamuraiArcher", "Performing bow shot")
        return true
    }
    
    private fun createArrow() {
        if (arrowSprite == null) {
            Log.e("SamuraiArcher", "Cannot create arrow - arrowSprite is null!")
            return
        }
        
        arrowSprite?.let { sprite ->
            // Calculate arrow starting position (in front of archer)
            val arrowX = x + width / 2
            val arrowY = y + height / 2
            
            // Calculate direction based on facing direction
            val (dirX, dirY) = when (facingDirection) {
                Direction.FRONT -> 0f to 1f
                Direction.BACK -> 0f to -1f
                Direction.LEFT -> -1f to 0f
                Direction.RIGHT -> 1f to 0f
            }
            
            val arrow = Arrow(arrowX, arrowY, dirX, dirY, sprite)
            arrows.add(arrow)
            
            Log.d("SamuraiArcher", "Arrow created at ($arrowX, $arrowY) direction ($dirX, $dirY). Total arrows: ${arrows.size}")
        }
    }
    
    /**
     * Override health methods from BaseHero to add logging
     */
    override fun takeDamage(damage: Float): Boolean {
        val died = super.takeDamage(damage)
        if (died) {
            currentState = HeroState.DEAD
            currentFrames = deadFrames
            currentFrame = 0
            Log.d("SamuraiArcher", "Samurai Archer died!")
        } else {
            // Show hurt animation briefly
            currentState = HeroState.HURT
            currentFrames = hurtFrames
            currentFrame = 0
        }
        return died
    }
    
    override fun heal(amount: Float) {
        if (isDead) return
        currentHealth = Math.min(currentHealth + amount, maxHealth)
        Log.d("SamuraiArcher", "Healed $amount, health: $currentHealth/$maxHealth")
    }
    
    /**
     * Rendering - Override from BaseHero
     */
    override fun render(canvas: Canvas, cameraX: Float, cameraY: Float) {
        if (currentFrames.isEmpty()) return
        
        val frameIndex = Math.min(currentFrame, currentFrames.size - 1)
        val bitmap = currentFrames[frameIndex]
        
        // Calculate screen position based on hero's logical center
        val screenX = x - cameraX
        val screenY = y - cameraY
        
        // Hero's logical size (collision bounds)
        val heroWidth = width
        val heroHeight = height
        
        // Sprite dimensions - scale up by 2x for better visibility
        val spriteScale = 2f
        val spriteWidth = bitmap.width.toFloat() * spriteScale
        val spriteHeight = bitmap.height.toFloat() * spriteScale

        // Calculate position to center sprite on hero's logical position
        // The hero's logical position (x,y) represents the center of the hero
        val renderX = screenX - spriteWidth / 2f
        val renderY = screenY - spriteHeight / 2f

        // Debug: Log positions for checking alignment (reduced frequency)
        if (System.currentTimeMillis() % 2000 < 50) { // Log every 2 seconds only
            Log.d("SamuraiArcher", "Render: logical($x, $y), screen($screenX, $screenY), render($renderX, $renderY), scale=$spriteScale")
        }

        // Create scaled bitmap destination rectangle
        val destRect = android.graphics.RectF(renderX, renderY, renderX + spriteWidth, renderY + spriteHeight)

        // Flip sprite based on direction
        when (facingDirection) {
            Direction.LEFT -> {
                canvas.save()
                canvas.scale(-1f, 1f, screenX, screenY)
                canvas.drawBitmap(bitmap, null, destRect, paint)
                canvas.restore()
            }
            else -> {
                canvas.drawBitmap(bitmap, null, destRect, paint)
            }
        }
        
        // Render arrows
        arrows.forEach { arrow ->
            arrow.render(canvas, cameraX, cameraY)
        }
        
        // Debug: render attack hitbox if attacking
        if (Log.isLoggable("SamuraiArcher", Log.DEBUG) && isAttacking) {
            getAttackHitbox()?.let { hitbox ->
                val debugPaint = Paint().apply {
                    color = android.graphics.Color.RED
                    style = Paint.Style.STROKE
                    strokeWidth = 3f
                    alpha = 128
                }
                canvas.drawRect(
                    hitbox.left - cameraX, 
                    hitbox.top - cameraY,
                    hitbox.right - cameraX, 
                    hitbox.bottom - cameraY, 
                    debugPaint
                )
            }
        }
        
        // Debug info - show both hitbox and sprite bounds
        if (Log.isLoggable("SamuraiArcher", Log.DEBUG)) {
            renderDebugInfo(canvas, screenX, screenY, renderX, renderY, spriteWidth, spriteHeight)
        }
    }
    
    private fun renderDebugInfo(canvas: Canvas, hitboxX: Float, hitboxY: Float, spriteX: Float, spriteY: Float, spriteWidth: Float, spriteHeight: Float) {
        val debugPaint = Paint().apply {
            color = android.graphics.Color.RED
            textSize = 20f
            isAntiAlias = true
            style = Paint.Style.STROKE
            strokeWidth = 2f
        }
        
        // Draw hitbox (logical position) - centered around the logical position
        debugPaint.color = android.graphics.Color.RED
        val hitboxLeft = hitboxX - width / 2f
        val hitboxTop = hitboxY - height / 2f
        canvas.drawRect(hitboxLeft, hitboxTop, hitboxLeft + width, hitboxTop + height, debugPaint)

        // Draw sprite bounds (now scaled)
        debugPaint.color = android.graphics.Color.BLUE
        canvas.drawRect(spriteX, spriteY, spriteX + spriteWidth, spriteY + spriteHeight, debugPaint)

        // Draw center point
        debugPaint.color = android.graphics.Color.GREEN
        debugPaint.style = Paint.Style.FILL
        canvas.drawCircle(hitboxX, hitboxY, 5f, debugPaint)

        // Draw debug text
        debugPaint.style = Paint.Style.FILL
        debugPaint.color = android.graphics.Color.YELLOW
        debugPaint.textSize = 16f
        val debugText = "State: $currentState, Pos: (${x.toInt()}, ${y.toInt()}), Scale: 2x"
        canvas.drawText(debugText, hitboxLeft, hitboxTop - 25, debugPaint)
    }
    
    /**
     * Override getHeroInfo from BaseHero
     */
    override fun getHeroInfo(): BaseHero.HeroInfo {
        return BaseHero.HeroInfo(
            name = "Samurai Archer",
            description = "Master of combo attacks and bow techniques",
            primaryAttackName = "Combo Slash",
            secondaryAbilityName = "Bow Shot",
            maxHealth = maxHealth,
            speed = speed
        )
    }
    
    /**
     * Samurai-specific getters
     */
    fun isAttacking() = isAttacking
    fun isShooting() = isShooting
    fun getArrows() = arrows.toList() // Return copy for safety
    fun getCurrentState() = currentState
    fun getAttackComboCount() = attackComboCount
    
    /**
     * Get attack hitbox when attacking
     */
    fun getAttackHitbox(): android.graphics.RectF? {
        if (!isAttacking) return null
        
        val attackRange = 100f // Attack range in pixels
        val attackWidth = 80f
        val attackHeight = 80f
        
        // Calculate attack position based on facing direction
        val (attackX, attackY) = when (facingDirection) {
            Direction.FRONT -> x to y + height
            Direction.BACK -> x to y - attackHeight
            Direction.LEFT -> x - attackRange to y
            Direction.RIGHT -> x + width to y
        }
        
        return android.graphics.RectF(
            attackX,
            attackY,
            attackX + attackWidth,
            attackY + attackHeight
        )
    }
    
    /**
     * Setters
     */
    override fun setPosition(newX: Float, newY: Float) {
        x = newX
        y = newY
    }
    
    override fun reset() {
        super.reset() // Reset BaseHero properties
        // Reset SamuraiArcher specific properties
        isAttacking = false
        isShooting = false
        attackComboCount = 0
        currentState = HeroState.IDLE
        currentFrames = idleFrames
        currentFrame = 0
        arrows.clear()
        Log.d("SamuraiArcher", "Samurai Archer reset")
    }
}