package com.example.mygame.game.managers

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.util.Log
import com.example.mygame.game.assets.GameAssetManager
import com.example.mygame.game.managers.MapManager
import com.example.mygame.game.entities.Skeleton
import com.example.mygame.game.entities.Fireball
import com.example.mygame.game.lighting.LightingSystem
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlin.math.sqrt
import kotlin.random.Random

class EnemyManager(
    private val context: Context,
    private val assetManager: GameAssetManager,
    private val mapManager: MapManager
) {
    private val skeletons = mutableListOf<Skeleton>()
    private val maxSkeletons = 3 // Reduced maximum number of skeletons to prevent ANR
    
    // Asset loading
    private var skeletonWalkFrames = mutableListOf<Bitmap>()
    private var skeletonIdleFrames = mutableListOf<Bitmap>()
    private var skeletonAttackFrames = mutableListOf<Bitmap>()
    private var skeletonHurtFrames = mutableListOf<Bitmap>()
    private var skeletonDeadFrames = mutableListOf<Bitmap>()
    private var assetsLoaded = false
    
    // Spawn management
    private var lastSpawnTime = 0L
    private val spawnCooldown = 10000L // Increase to 10 seconds to reduce spawn frequency
    private val spawnDistance = 800f // Spawn skeletons at least this far from player
    
    init {
        // Start loading assets immediately when EnemyManager is created
        // This ensures assets are ready when needed
        loadSkeletonAssets()
    }
    
    private fun loadSkeletonAssets() {
        // Don't block the main thread - load assets asynchronously
        CoroutineScope(Dispatchers.IO).launch {
            try {
                Log.d("EnemyManager", "Loading skeleton assets...")
                
                // Load walk frames
                val walkAssets = assetManager.loadAssetsFromFolder("textures/characters/enemies/skeleton/walk")
                walkAssets?.let { assets ->
                    val tempWalkFrames = mutableListOf<Bitmap>()
                    assets.forEach { asset ->
                        asset?.let { bitmap ->
                            val scaledBitmap = Bitmap.createScaledBitmap(bitmap, 128, 128, false)
                            tempWalkFrames.add(scaledBitmap)
                        }
                    }
                    
                    CoroutineScope(Dispatchers.Main).launch {
                        skeletonWalkFrames.addAll(tempWalkFrames)
                    }
                }
                
                // Load idle frames
                val idleAssets = assetManager.loadAssetsFromFolder("textures/characters/enemies/skeleton/idle")
                idleAssets?.let { assets ->
                    val tempIdleFrames = mutableListOf<Bitmap>()
                    assets.forEach { asset ->
                        asset?.let { bitmap ->
                            val scaledBitmap = Bitmap.createScaledBitmap(bitmap, 128, 128, false)
                            tempIdleFrames.add(scaledBitmap)
                        }
                    }
                    
                    CoroutineScope(Dispatchers.Main).launch {
                        skeletonIdleFrames.addAll(tempIdleFrames)
                    }
                }
                
                // Load attack frames
                val attackAssets = assetManager.loadAssetsFromFolder("textures/characters/enemies/skeleton/attack")
                attackAssets?.let { assets ->
                    val tempAttackFrames = mutableListOf<Bitmap>()
                    assets.forEach { asset ->
                        asset?.let { bitmap ->
                            val scaledBitmap = Bitmap.createScaledBitmap(bitmap, 128, 128, false)
                            tempAttackFrames.add(scaledBitmap)
                        }
                    }
                    
                    // Switch to main thread to update UI-related data
                    CoroutineScope(Dispatchers.Main).launch {
                        skeletonAttackFrames.addAll(tempAttackFrames)
                    }
                }
                
                // Load hurt frames
                val hurtAssets = assetManager.loadAssetsFromFolder("textures/characters/enemies/skeleton/hurt")
                hurtAssets?.let { assets ->
                    val tempHurtFrames = mutableListOf<Bitmap>()
                    assets.forEach { asset ->
                        asset?.let { bitmap ->
                            val scaledBitmap = Bitmap.createScaledBitmap(bitmap, 128, 128, false)
                            tempHurtFrames.add(scaledBitmap)
                        }
                    }
                    
                    CoroutineScope(Dispatchers.Main).launch {
                        skeletonHurtFrames.addAll(tempHurtFrames)
                    }
                }
                
                // Load dead frames
                val deadAssets = assetManager.loadAssetsFromFolder("textures/characters/enemies/skeleton/dead")
                deadAssets?.let { assets ->
                    val tempDeadFrames = mutableListOf<Bitmap>()
                    assets.forEach { asset ->
                        asset?.let { bitmap ->
                            val scaledBitmap = Bitmap.createScaledBitmap(bitmap, 128, 128, false)
                            tempDeadFrames.add(scaledBitmap)
                        }
                    }
                    
                    CoroutineScope(Dispatchers.Main).launch {
                        skeletonDeadFrames.addAll(tempDeadFrames)
                    }
                }
                
                // Fallback textures if assets fail to load
                if (skeletonWalkFrames.isEmpty() && skeletonIdleFrames.isEmpty() && skeletonAttackFrames.isEmpty()) {
                    CoroutineScope(Dispatchers.Main).launch {
                        createFallbackTextures()
                    }
                }
                
                // Mark as loaded and initialize existing skeletons
                CoroutineScope(Dispatchers.Main).launch {
                    assetsLoaded = true
                    Log.d("EnemyManager", "Skeleton assets loaded: ${skeletonWalkFrames.size} walk, ${skeletonIdleFrames.size} idle, ${skeletonAttackFrames.size} attack, ${skeletonHurtFrames.size} hurt, ${skeletonDeadFrames.size} dead frames")
                    
                    // Initialize any existing skeletons with textures
                    skeletons.forEach { skeleton ->
                        skeleton.loadTextures(skeletonWalkFrames, skeletonIdleFrames, skeletonAttackFrames, skeletonHurtFrames, skeletonDeadFrames)
                    }
                }
                
            } catch (e: Exception) {
                Log.e("EnemyManager", "Error loading skeleton assets: ${e.message}")
                CoroutineScope(Dispatchers.Main).launch {
                    createFallbackTextures()
                    assetsLoaded = true
                    
                    // Initialize existing skeletons with fallback textures
                    skeletons.forEach { skeleton ->
                        skeleton.loadTextures(skeletonWalkFrames, skeletonIdleFrames, skeletonAttackFrames, skeletonHurtFrames, skeletonDeadFrames)
                    }
                }
            }
        }
    }
    
    private fun createFallbackTextures() {
        Log.d("EnemyManager", "Creating fallback skeleton textures")
        
        // Create simple colored rectangles as fallback - increased size
        val fallbackBitmap = Bitmap.createBitmap(128, 128, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(fallbackBitmap)
        
        // Walk frames (white)
        canvas.drawColor(android.graphics.Color.WHITE)
        skeletonWalkFrames.add(fallbackBitmap.copy(Bitmap.Config.ARGB_8888, false))
        
        // Idle frames (light gray)
        canvas.drawColor(android.graphics.Color.LTGRAY)
        skeletonIdleFrames.add(fallbackBitmap.copy(Bitmap.Config.ARGB_8888, false))
        
        // Attack frames (red)
        canvas.drawColor(android.graphics.Color.RED)
        skeletonAttackFrames.add(fallbackBitmap.copy(Bitmap.Config.ARGB_8888, false))
        
        // Hurt frames (orange)
        canvas.drawColor(android.graphics.Color.argb(255, 255, 165, 0))
        skeletonHurtFrames.add(fallbackBitmap.copy(Bitmap.Config.ARGB_8888, false))
        
        // Dead frames (gray)
        canvas.drawColor(android.graphics.Color.GRAY)
        skeletonDeadFrames.add(fallbackBitmap.copy(Bitmap.Config.ARGB_8888, false))
    }
    
    fun spawnSkeleton(x: Float, y: Float) {
        if (skeletons.size >= maxSkeletons) {
            Log.d("EnemyManager", "Cannot spawn skeleton: maximum limit reached")
            return
        }
        
        if (mapManager.isWall(x, y)) {
            Log.d("EnemyManager", "Cannot spawn skeleton at ($x, $y): position is wall")
            return
        }
        
        val skeleton = Skeleton(x, y, mapManager)
        
        if (assetsLoaded) {
            skeleton.loadTextures(skeletonWalkFrames, skeletonIdleFrames, skeletonAttackFrames, skeletonHurtFrames, skeletonDeadFrames)
        }
        
        skeletons.add(skeleton)
        Log.d("EnemyManager", "Spawned skeleton at ($x, $y). Total skeletons: ${skeletons.size}")
    }
    
    fun spawnRandomSkeleton(playerX: Float, playerY: Float) {
        val currentTime = System.currentTimeMillis()
        
        // Check if assets are loaded first to prevent spawning before ready
        if (!assetsLoaded) {
            return
        }
        
        // Check spawn cooldown
        if (currentTime - lastSpawnTime < spawnCooldown) {
            return
        }
        
        // Check skeleton limit
        if (skeletons.size >= maxSkeletons) {
            return
        }
        
        // Try to find a valid spawn location
        var attempts = 0
        while (attempts < 20) {
            val angle = Random.nextFloat() * 2 * kotlin.math.PI.toFloat()
            val distance = spawnDistance + Random.nextFloat() * 200f
            
            val spawnX = playerX + kotlin.math.cos(angle) * distance
            val spawnY = playerY + kotlin.math.sin(angle) * distance
            
            if (!mapManager.isWall(spawnX, spawnY)) {
                spawnSkeleton(spawnX, spawnY)
                lastSpawnTime = currentTime
                return
            }
            
            attempts++
        }
        
        Log.d("EnemyManager", "Failed to find valid spawn location after $attempts attempts")
    }
    
    fun update(deltaTime: Float, playerX: Float, playerY: Float, lightingSystem: LightingSystem) {
        if (!assetsLoaded) return
        
        // Limit updates to prevent ANR - only update some skeletons per frame
        val maxUpdatesPerFrame = kotlin.math.min(5, skeletons.size)
        var updatesThisFrame = 0
        
        // Determine if player is in light (simplified - always true for now)
        val playerInLight = true // We'll improve this later with proper light detection
        
        // Update skeletons with frame limiting
        val iterator = skeletons.iterator()
        while (iterator.hasNext() && updatesThisFrame < maxUpdatesPerFrame) {
            val skeleton = iterator.next()
            
            // Only update skeletons near the player to save performance
            val distance = kotlin.math.sqrt(
                (skeleton.getX() - playerX) * (skeleton.getX() - playerX) + 
                (skeleton.getY() - playerY) * (skeleton.getY() - playerY)
            )
            
            if (distance < 1000f) { // Only update skeletons within 1000 pixels
                skeleton.update(deltaTime, playerX, playerY, playerInLight)
                updatesThisFrame++
            }
            
            // Remove dead skeletons after their death animation
            if (skeleton.isDead() && System.currentTimeMillis() - 3000 > 0) { // Keep dead bodies for 3 seconds
                iterator.remove()
            }
        }
        
        // Spawn new skeletons occasionally
        spawnRandomSkeleton(playerX, playerY)
        
        // Debug log every 5 seconds
        if (System.currentTimeMillis() % 5000 < 100) {
            Log.d("EnemyManager", "Active skeletons: ${skeletons.count { it.isAlive() }}, Dead skeletons: ${skeletons.count { it.isDead() }}")
        }
    }
    
    fun draw(canvas: Canvas, cameraX: Float, cameraY: Float) {
        if (!assetsLoaded) return
        
        skeletons.forEach { skeleton ->
            skeleton.draw(canvas, cameraX, cameraY)
        }
    }
    
    // Combat system integration
    fun checkFireballCollisions(fireballs: List<Fireball>): Int {
        var hitCount = 0
        
        fireballs.forEach { fireball ->
            if (!fireball.isExploding() && fireball.isActive()) {
                skeletons.forEach { skeleton ->
                    if (skeleton.isAlive()) {
                        val dx = fireball.getX() - skeleton.getX()
                        val dy = fireball.getY() - skeleton.getY()
                        val distance = sqrt(dx * dx + dy * dy)
                        
                        // Check collision with skeleton - updated for larger fireball
                        if (distance <= (skeleton.getWidth() / 2f + 64f)) { // 64f is fireball radius (128/2)
                            skeleton.takeDamage(10) // 10 damage per fireball hit
                            fireball.triggerExplosion()
                            hitCount++
                            Log.d("EnemyManager", "Fireball hit skeleton! Skeleton health: ${skeleton.getHealth()}")
                        }
                    }
                }
            }
        }
        
        return hitCount
    }
    
    // Check if any skeleton can attack the player
    fun checkSkeletonAttacks(playerX: Float, playerY: Float): Int {
        var totalDamage = 0
        
        skeletons.forEach { skeleton ->
            val damage = skeleton.getAndClearLastAttackDamage()
            if (damage > 0) {
                totalDamage += damage
                Log.d("EnemyManager", "Skeleton dealt $damage damage to player!")
            }
        }
        
        return totalDamage
    }
    
    // Get skeleton count for UI
    fun getSkeletonCount(): Int = skeletons.size
    fun getAliveSkeletonCount(): Int = skeletons.count { it.isAlive() }
    fun getDeadSkeletonCount(): Int = skeletons.count { it.isDead() }
    
    // Clear all skeletons (for level transitions)
    fun clearAllSkeletons() {
        skeletons.clear()
        Log.d("EnemyManager", "All skeletons cleared")
    }
    
    // Spawn initial skeletons for level start
    fun spawnInitialSkeletons(playerX: Float, playerY: Float, count: Int = 3) {
        repeat(count) {
            spawnRandomSkeleton(playerX, playerY)
        }
    }
    
    // Get all skeletons (for debug purposes)
    fun getAllSkeletons(): List<Skeleton> = skeletons.toList()
    
    // Check if assets are loaded
    fun areAssetsLoaded(): Boolean = assetsLoaded
    
    // Cleanup resources
    fun cleanup() {
        skeletonWalkFrames.forEach { it.recycle() }
        skeletonIdleFrames.forEach { it.recycle() }
        skeletonAttackFrames.forEach { it.recycle() }
        skeletonHurtFrames.forEach { it.recycle() }
        skeletonDeadFrames.forEach { it.recycle() }
        
        skeletonWalkFrames.clear()
        skeletonIdleFrames.clear()
        skeletonAttackFrames.clear()
        skeletonHurtFrames.clear()
        skeletonDeadFrames.clear()
        
        skeletons.clear()
        
        Log.d("EnemyManager", "EnemyManager cleaned up")
    }
}