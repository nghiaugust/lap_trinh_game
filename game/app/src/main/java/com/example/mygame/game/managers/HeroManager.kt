package com.example.mygame.game.managers

import android.content.Context
import android.graphics.Canvas
import android.util.Log
import com.example.mygame.game.entities.BaseHero
import com.example.mygame.game.entities.heroes.SamuraiArcher
import com.example.mygame.game.assets.GameAssetManager

/**
 * Hero Manager - manages hero selection and hero-specific gameplay
 */
class HeroManager(private val context: Context) {
    
    // Hero types
    enum class HeroType {
        SAMURAI_ARCHER
        // Future heroes: KNIGHT_WARRIOR, FIRE_MAGE, SHADOW_NINJA, etc.
    }
    
    // Current hero - using BaseHero interface
    private var currentHeroType = HeroType.SAMURAI_ARCHER
    private var currentHero: BaseHero? = null
    
    // Hero state
    private var isInitialized = false
    
    fun initialize(assetManager: GameAssetManager, startX: Float, startY: Float) {
        try {
            Log.d("HeroManager", "Initializing hero at position ($startX, $startY)")
            
            // Initialize default hero (SamuraiArcher)
            currentHero = SamuraiArcher(startX, startY)
            currentHero?.loadTextures(assetManager)
            
            isInitialized = true
            Log.d("HeroManager", "Hero initialized successfully: ${currentHeroType}")
            
        } catch (e: Exception) {
            Log.e("HeroManager", "Error initializing hero: ${e.message}")
        }
    }
    
    /**
     * Switch hero type (for future expansion)
     */
    fun switchHero(heroType: HeroType) {
        if (!isInitialized) {
            Log.w("HeroManager", "Cannot switch hero - not initialized")
            return
        }
        
        val oldPosition = currentHero?.let { Pair(it.positionX, it.positionY) }
        val oldHealth = currentHero?.health ?: 0f
        
        currentHeroType = heroType
        
        // Create new hero instance based on type
        when (heroType) {
            HeroType.SAMURAI_ARCHER -> {
                currentHero = SamuraiArcher(oldPosition?.first ?: 0f, oldPosition?.second ?: 0f)
                // In the future, preserve health between switches if desired
            }
            // Future heroes can be added here
        }
        
        Log.d("HeroManager", "Switched to hero: $heroType")
    }
    
    /**
     * Set current hero position
     */
    fun setCurrentHeroPosition(x: Float, y: Float) {
        currentHero?.setPosition(x, y)
    }
    
    /**
     * Update current hero
     */
    fun update(deltaTime: Float, worldWidth: Float, worldHeight: Float, mapManager: MapManager? = null) {
        if (!isInitialized) return
        
        currentHero?.update(deltaTime, worldWidth, worldHeight, mapManager)
        
        // Handle specific hero features
        if (currentHero is SamuraiArcher) {
            val samurai = currentHero as SamuraiArcher
            // Handle arrow-map collisions
            mapManager?.let { map ->
                samurai.getArrows().forEach { arrow ->
                    arrow.checkMapCollision(map)
                }
            }
        }
    }
    
    /**
     * Render current hero
     */
    fun render(canvas: Canvas, cameraX: Float, cameraY: Float) {
        if (!isInitialized) return
        currentHero?.render(canvas, cameraX, cameraY)
    }
    
    /**
     * Movement controls - using BaseHero interface
     */
    fun setMovementDirection(deltaX: Float, deltaY: Float) {
        currentHero?.setMovementDirection(deltaX, deltaY)
    }
    
    fun stopMovement() {
        currentHero?.stopMovement()
    }
    
    /**
     * Combat controls - using BaseHero interface
     */
    fun performAttack(): Boolean {
        return currentHero?.performPrimaryAttack() ?: false
    }
    
    fun performSpecialAttack(): Boolean {
        return currentHero?.performSecondaryAbility() ?: false
    }
    
    /**
     * Health system - using BaseHero interface
     */
    fun takeDamage(damage: Float): Boolean {
        return currentHero?.takeDamage(damage) ?: false
    }
    
    fun getCurrentHealth(): Float {
        return currentHero?.health ?: 0f
    }
    
    fun getMaxHealth(): Float {
        return currentHero?.maxHeroHealth ?: 0f
    }
    
    fun getCurrentArmor(): Float {
        return currentHero?.armor ?: 0f
    }
    
    fun getMaxArmor(): Float {
        return currentHero?.maxHeroArmor ?: 0f
    }
    
    fun isDead(): Boolean {
        return currentHero?.isHeroDead ?: true
    }
    
    /**
     * Collision detection for hero projectiles
     */
    fun checkProjectileCollisions(targetX: Float, targetY: Float, targetWidth: Float, targetHeight: Float): Int {
        var hitCount = 0
        
        // Handle specific hero projectiles
        if (currentHero is SamuraiArcher) {
            val samurai = currentHero as SamuraiArcher
            samurai.getArrows().forEach { arrow ->
                if (arrow.checkCollision(targetX, targetY, targetWidth, targetHeight)) {
                    hitCount++
                }
            }
        }
        
        return hitCount
    }
    
    /**
     * Check melee attack collision
     */
    fun checkMeleeAttackCollision(targetX: Float, targetY: Float, targetWidth: Float, targetHeight: Float): Boolean {
        if (currentHero is SamuraiArcher) {
            val samurai = currentHero as SamuraiArcher
            val attackHitbox = samurai.getAttackHitbox()
            
            if (attackHitbox != null) {
                val targetRect = android.graphics.RectF(targetX, targetY, targetX + targetWidth, targetY + targetHeight)
                return android.graphics.RectF.intersects(attackHitbox, targetRect)
            }
        }
        
        return false
    }
    
    /**
     * Get current hero bounds for collision detection
     */
    fun getCurrentHeroBounds(): android.graphics.RectF? {
        return currentHero?.let { hero ->
            android.graphics.RectF(
                hero.positionX, 
                hero.positionY, 
                hero.positionX + hero.heroWidth, 
                hero.positionY + hero.heroHeight
            )
        }
    }
    
    /**
     * Reset heroes
     */
    fun reset() {
        currentHero?.reset()
        Log.d("HeroManager", "Hero reset")
    }
    
    /**
     * Getters
     */
    fun getCurrentHeroType() = currentHeroType
    fun getCurrentHero() = currentHero
    fun isInitialized() = isInitialized
    
    /**
     * Camera position for current hero
     */
    fun getCameraTargetX(): Float {
        return currentHero?.positionX ?: 0f
    }
    
    fun getCameraTargetY(): Float {
        return currentHero?.positionY ?: 0f
    }
    
    /**
     * Debug info
     */
    fun getDebugInfo(): String {
        val heroInfo = currentHero?.getHeroInfo()?.name ?: "No Hero"
        val position = currentHero?.let { "${it.positionX.toInt()},${it.positionY.toInt()}" } ?: "0,0"
        val health = "${getCurrentHealth().toInt()}/${getMaxHealth().toInt()}"
        
        return "$heroInfo | Pos: $position | HP: $health"
    }
}