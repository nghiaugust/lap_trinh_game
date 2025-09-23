package com.example.mygame.game.managers

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.util.Log
import com.example.mygame.game.assets.GameAssetManager
import com.example.mygame.game.managers.MapManager
import com.example.mygame.game.entities.enemies.Skeleton
import com.example.mygame.game.entities.enemies.Demon
import com.example.mygame.game.entities.enemies.Dragon
import com.example.mygame.game.entities.projectiles.DragonFireBreath
import com.example.mygame.game.entities.Fireball
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
    // Enemy collections
    private val skeletons = mutableListOf<Skeleton>()
    private val demons = mutableListOf<Demon>()
    private val dragons = mutableListOf<Dragon>()
    private val dragonProjectiles = mutableListOf<DragonFireBreath>()
    
    // Enemy limits - reduced for better performance
    private val maxSkeletons = 1 // Reduced from 2 to 1
    private val maxDemons = 1    // Keep at 1
    private val maxDragons = 1   // Keep at 1 (boss-like)
    
    // Death time tracking - since Skeleton class doesn't have getDeathTime()
    private val skeletonDeathTimes = mutableMapOf<Skeleton, Long>()
    private val demonDeathTimes = mutableMapOf<Demon, Long>()
    private val dragonDeathTimes = mutableMapOf<Dragon, Long>()
    
    // Asset loading - Skeleton
    private var skeletonWalkFrames = mutableListOf<Bitmap>()
    private var skeletonIdleFrames = mutableListOf<Bitmap>()
    private var skeletonAttackFrames = mutableListOf<Bitmap>()
    private var skeletonHurtFrames = mutableListOf<Bitmap>()
    private var skeletonDeadFrames = mutableListOf<Bitmap>()
    
    // Asset loading - Demon
    private var demonWalkFrames = mutableListOf<Bitmap>()
    private var demonIdleFrames = mutableListOf<Bitmap>()
    private var demonAttackFrames = mutableListOf<Bitmap>()
    private var demonHurtFrames = mutableListOf<Bitmap>()
    private var demonDeadFrames = mutableListOf<Bitmap>()
    
    // Asset loading - Dragon
    private var dragonWalkFrames = mutableListOf<Bitmap>()
    private var dragonIdleFrames = mutableListOf<Bitmap>()
    private var dragonAttackFrames = mutableListOf<Bitmap>()
    private var dragonFireBreathFrames = mutableListOf<Bitmap>()
    private var dragonHurtFrames = mutableListOf<Bitmap>()
    private var dragonDeadFrames = mutableListOf<Bitmap>()
    
    private var assetsLoaded = false
    
    // Spawn management - optimized for better performance
    private var lastSpawnTime = 0L
    private val spawnCooldown = 20000L // Increased to 20 seconds to reduce enemy density
    private val spawnDistance = 800f // Spawn enemies at least this far from heroes
    
    init {
        // Start loading assets immediately when EnemyManager is created
        // This ensures assets are ready when needed
        loadAllAssets()
    }
    
    private fun loadAllAssets() {
        loadSkeletonAssets()
        loadDemonAssets()
        loadDragonAssets()
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
                            // Optimize: Use background thread for scaling
                            val scaledBitmap = Bitmap.createScaledBitmap(bitmap, 128, 128, false)
                            tempWalkFrames.add(scaledBitmap)
                        }
                    }
                    
                    // Switch to main thread only for final assignment
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
                
                // Mark as loaded and initialize existing skeletons
                CoroutineScope(Dispatchers.Main).launch {
                    // Only create fallback if no assets were loaded
                    if (skeletonWalkFrames.isEmpty() && skeletonIdleFrames.isEmpty() && skeletonAttackFrames.isEmpty()) {
                        Log.w("EnemyManager", "No skeleton assets found, creating fallback textures")
                        createFallbackTextures()
                    }
                    
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
        Log.d("EnemyManager", "Creating fallback textures for missing assets")
        
        // Create simple colored rectangles as fallback - increased size
        val fallbackBitmap = Bitmap.createBitmap(128, 128, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(fallbackBitmap)
        
        // Only create fallback for skeleton if no assets loaded
        if (skeletonWalkFrames.isEmpty()) {
            Log.w("EnemyManager", "Creating fallback skeleton textures")
            // Skeleton textures (white/gray theme)
            canvas.drawColor(android.graphics.Color.WHITE)
            skeletonWalkFrames.add(fallbackBitmap.copy(Bitmap.Config.ARGB_8888, false))
            canvas.drawColor(android.graphics.Color.LTGRAY)
            skeletonIdleFrames.add(fallbackBitmap.copy(Bitmap.Config.ARGB_8888, false))
            canvas.drawColor(android.graphics.Color.RED)
            skeletonAttackFrames.add(fallbackBitmap.copy(Bitmap.Config.ARGB_8888, false))
            canvas.drawColor(android.graphics.Color.argb(255, 255, 165, 0))
            skeletonHurtFrames.add(fallbackBitmap.copy(Bitmap.Config.ARGB_8888, false))
            canvas.drawColor(android.graphics.Color.GRAY)
            skeletonDeadFrames.add(fallbackBitmap.copy(Bitmap.Config.ARGB_8888, false))
        }
        
        // Only create fallback for demon if no assets loaded
        if (demonWalkFrames.isEmpty()) {
            Log.w("EnemyManager", "Creating fallback demon textures")
            // Demon textures (dark red/purple theme)
            canvas.drawColor(android.graphics.Color.argb(255, 139, 0, 0)) // Dark red
            demonWalkFrames.add(fallbackBitmap.copy(Bitmap.Config.ARGB_8888, false))
            canvas.drawColor(android.graphics.Color.argb(255, 75, 0, 130)) // Indigo
            demonIdleFrames.add(fallbackBitmap.copy(Bitmap.Config.ARGB_8888, false))
            canvas.drawColor(android.graphics.Color.argb(255, 220, 20, 60)) // Crimson
            demonAttackFrames.add(fallbackBitmap.copy(Bitmap.Config.ARGB_8888, false))
            canvas.drawColor(android.graphics.Color.argb(255, 255, 69, 0)) // Red orange
            demonHurtFrames.add(fallbackBitmap.copy(Bitmap.Config.ARGB_8888, false))
            canvas.drawColor(android.graphics.Color.argb(255, 64, 64, 64)) // Dark gray
            demonDeadFrames.add(fallbackBitmap.copy(Bitmap.Config.ARGB_8888, false))
        }
        
        // Only create fallback for dragon if no assets loaded
        if (dragonWalkFrames.isEmpty()) {
            Log.w("EnemyManager", "Creating fallback dragon textures")
            // Dragon textures (green/golden theme)
            canvas.drawColor(android.graphics.Color.argb(255, 34, 139, 34)) // Forest green
            dragonWalkFrames.add(fallbackBitmap.copy(Bitmap.Config.ARGB_8888, false))
            canvas.drawColor(android.graphics.Color.argb(255, 0, 128, 0)) // Green
            dragonIdleFrames.add(fallbackBitmap.copy(Bitmap.Config.ARGB_8888, false))
            canvas.drawColor(android.graphics.Color.argb(255, 255, 215, 0)) // Gold
            dragonAttackFrames.add(fallbackBitmap.copy(Bitmap.Config.ARGB_8888, false))
            canvas.drawColor(android.graphics.Color.argb(255, 255, 140, 0)) // Dark orange (fire breath)
            dragonFireBreathFrames.add(fallbackBitmap.copy(Bitmap.Config.ARGB_8888, false))
            canvas.drawColor(android.graphics.Color.argb(255, 255, 165, 0)) // Orange
            dragonHurtFrames.add(fallbackBitmap.copy(Bitmap.Config.ARGB_8888, false))
            canvas.drawColor(android.graphics.Color.argb(255, 85, 107, 47)) // Dark olive green
            dragonDeadFrames.add(fallbackBitmap.copy(Bitmap.Config.ARGB_8888, false))
        }
    }
    
    // Demon asset loading
    private fun loadDemonAssets() {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                Log.d("EnemyManager", "Loading demon assets...")
                
                // Load demon walk frames
                val walkAssets = assetManager.loadAssetsFromFolder("textures/characters/enemies/demon/walk")
                walkAssets?.let { assets ->
                    val tempWalkFrames = mutableListOf<Bitmap>()
                    assets.forEach { asset ->
                        asset?.let { bitmap ->
                            val scaledBitmap = Bitmap.createScaledBitmap(bitmap, 128, 128, false)
                            tempWalkFrames.add(scaledBitmap)
                        }
                    }
                    CoroutineScope(Dispatchers.Main).launch {
                        demonWalkFrames.addAll(tempWalkFrames)
                    }
                }
                
                // Load demon idle frames
                val idleAssets = assetManager.loadAssetsFromFolder("textures/characters/enemies/demon/idle")
                idleAssets?.let { assets ->
                    val tempIdleFrames = mutableListOf<Bitmap>()
                    assets.forEach { asset ->
                        asset?.let { bitmap ->
                            val scaledBitmap = Bitmap.createScaledBitmap(bitmap, 128, 128, false)
                            tempIdleFrames.add(scaledBitmap)
                        }
                    }
                    CoroutineScope(Dispatchers.Main).launch {
                        demonIdleFrames.addAll(tempIdleFrames)
                    }
                }
                
                // Load demon attack frames
                val attackAssets = assetManager.loadAssetsFromFolder("textures/characters/enemies/demon/attack")
                attackAssets?.let { assets ->
                    val tempAttackFrames = mutableListOf<Bitmap>()
                    assets.forEach { asset ->
                        asset?.let { bitmap ->
                            val scaledBitmap = Bitmap.createScaledBitmap(bitmap, 128, 128, false)
                            tempAttackFrames.add(scaledBitmap)
                        }
                    }
                    CoroutineScope(Dispatchers.Main).launch {
                        demonAttackFrames.addAll(tempAttackFrames)
                    }
                }
                
                // Load demon hurt frames
                val hurtAssets = assetManager.loadAssetsFromFolder("textures/characters/enemies/demon/hurt")
                hurtAssets?.let { assets ->
                    val tempHurtFrames = mutableListOf<Bitmap>()
                    assets.forEach { asset ->
                        asset?.let { bitmap ->
                            val scaledBitmap = Bitmap.createScaledBitmap(bitmap, 128, 128, false)
                            tempHurtFrames.add(scaledBitmap)
                        }
                    }
                    CoroutineScope(Dispatchers.Main).launch {
                        demonHurtFrames.addAll(tempHurtFrames)
                    }
                }
                
                // Load demon dead frames
                val deadAssets = assetManager.loadAssetsFromFolder("textures/characters/enemies/demon/dead")
                deadAssets?.let { assets ->
                    val tempDeadFrames = mutableListOf<Bitmap>()
                    assets.forEach { asset ->
                        asset?.let { bitmap ->
                            val scaledBitmap = Bitmap.createScaledBitmap(bitmap, 128, 128, false)
                            tempDeadFrames.add(scaledBitmap)
                        }
                    }
                    CoroutineScope(Dispatchers.Main).launch {
                        demonDeadFrames.addAll(tempDeadFrames)
                    }
                }
                
                Log.d("EnemyManager", "Demon assets loaded: ${demonWalkFrames.size} walk, ${demonIdleFrames.size} idle")
                
                // Initialize any existing demons with textures
                CoroutineScope(Dispatchers.Main).launch {
                    demons.forEach { demon ->
                        demon.loadTextures(demonWalkFrames, demonIdleFrames, demonAttackFrames, demonHurtFrames, demonDeadFrames)
                    }
                }
                
            } catch (e: Exception) {
                Log.e("EnemyManager", "Error loading demon assets: ${e.message}")
            }
        }
    }
    
    // Dragon asset loading
    private fun loadDragonAssets() {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                Log.d("EnemyManager", "Loading dragon assets...")
                
                // Load dragon walk frames
                val walkAssets = assetManager.loadAssetsFromFolder("textures/characters/enemies/dragon/walk")
                walkAssets?.let { assets ->
                    val tempWalkFrames = mutableListOf<Bitmap>()
                    assets.forEach { asset ->
                        asset?.let { bitmap ->
                            val scaledBitmap = Bitmap.createScaledBitmap(bitmap, 128, 128, false)
                            tempWalkFrames.add(scaledBitmap)
                        }
                    }
                    CoroutineScope(Dispatchers.Main).launch {
                        dragonWalkFrames.addAll(tempWalkFrames)
                    }
                }
                
                // Load dragon idle frames
                val idleAssets = assetManager.loadAssetsFromFolder("textures/characters/enemies/dragon/idle")
                idleAssets?.let { assets ->
                    val tempIdleFrames = mutableListOf<Bitmap>()
                    assets.forEach { asset ->
                        asset?.let { bitmap ->
                            val scaledBitmap = Bitmap.createScaledBitmap(bitmap, 128, 128, false)
                            tempIdleFrames.add(scaledBitmap)
                        }
                    }
                    CoroutineScope(Dispatchers.Main).launch {
                        dragonIdleFrames.addAll(tempIdleFrames)
                    }
                }
                
                // Load dragon attack frames
                val attackAssets = assetManager.loadAssetsFromFolder("textures/characters/enemies/dragon/attack")
                attackAssets?.let { assets ->
                    val tempAttackFrames = mutableListOf<Bitmap>()
                    assets.forEach { asset ->
                        asset?.let { bitmap ->
                            val scaledBitmap = Bitmap.createScaledBitmap(bitmap, 128, 128, false)
                            tempAttackFrames.add(scaledBitmap)
                        }
                    }
                    CoroutineScope(Dispatchers.Main).launch {
                        dragonAttackFrames.addAll(tempAttackFrames)
                    }
                }
                
                // Load dragon fire breath frames from skill folder
                val fireBreathAssets = assetManager.loadAssetsFromFolder("textures/characters/enemies/dragon/skill")
                fireBreathAssets?.let { assets ->
                    val tempFireBreathFrames = mutableListOf<Bitmap>()
                    assets.forEach { asset ->
                        asset?.let { bitmap ->
                            val scaledBitmap = Bitmap.createScaledBitmap(bitmap, 128, 128, false)
                            tempFireBreathFrames.add(scaledBitmap)
                        }
                    }
                    CoroutineScope(Dispatchers.Main).launch {
                        dragonFireBreathFrames.addAll(tempFireBreathFrames)
                        Log.d("EnemyManager", "Loaded ${tempFireBreathFrames.size} dragon fire breath frames from skill folder")
                    }
                }
                
                // Load dragon hurt frames
                val hurtAssets = assetManager.loadAssetsFromFolder("textures/characters/enemies/dragon/hurt")
                hurtAssets?.let { assets ->
                    val tempHurtFrames = mutableListOf<Bitmap>()
                    assets.forEach { asset ->
                        asset?.let { bitmap ->
                            val scaledBitmap = Bitmap.createScaledBitmap(bitmap, 128, 128, false)
                            tempHurtFrames.add(scaledBitmap)
                        }
                    }
                    CoroutineScope(Dispatchers.Main).launch {
                        dragonHurtFrames.addAll(tempHurtFrames)
                    }
                }
                
                // Load dragon dead frames
                val deadAssets = assetManager.loadAssetsFromFolder("textures/characters/enemies/dragon/dead")
                deadAssets?.let { assets ->
                    val tempDeadFrames = mutableListOf<Bitmap>()
                    assets.forEach { asset ->
                        asset?.let { bitmap ->
                            val scaledBitmap = Bitmap.createScaledBitmap(bitmap, 128, 128, false)
                            tempDeadFrames.add(scaledBitmap)
                        }
                    }
                    CoroutineScope(Dispatchers.Main).launch {
                        dragonDeadFrames.addAll(tempDeadFrames)
                    }
                }
                
                Log.d("EnemyManager", "Dragon assets loaded: ${dragonWalkFrames.size} walk, ${dragonIdleFrames.size} idle, ${dragonFireBreathFrames.size} firebreath")
                
                // Initialize any existing dragons with textures
                CoroutineScope(Dispatchers.Main).launch {
                    dragons.forEach { dragon ->
                        dragon.loadTextures(dragonWalkFrames, dragonIdleFrames, dragonAttackFrames, dragonFireBreathFrames, dragonHurtFrames, dragonDeadFrames)
                    }
                }
                
            } catch (e: Exception) {
                Log.e("EnemyManager", "Error loading dragon assets: ${e.message}")
            }
        }
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
    
    fun spawnDemon(x: Float, y: Float) {
        if (demons.size >= maxDemons) {
            Log.d("EnemyManager", "Cannot spawn demon: maximum limit reached")
            return
        }
        
        if (mapManager.isWall(x, y)) {
            Log.d("EnemyManager", "Cannot spawn demon at ($x, $y): position is wall")
            return
        }
        
        val demon = Demon(x, y, mapManager)
        
        if (assetsLoaded) {
            demon.loadTextures(demonWalkFrames, demonIdleFrames, demonAttackFrames, demonHurtFrames, demonDeadFrames)
        }
        
        demons.add(demon)
        Log.d("EnemyManager", "Spawned demon at ($x, $y). Total demons: ${demons.size}")
    }
    
    fun spawnDragon(x: Float, y: Float) {
        if (dragons.size >= maxDragons) {
            Log.d("EnemyManager", "Cannot spawn dragon: maximum limit reached")
            return
        }
        
        if (mapManager.isWall(x, y)) {
            Log.d("EnemyManager", "Cannot spawn dragon at ($x, $y): position is wall")
            return
        }
        
        val dragon = Dragon(x, y, mapManager)
        
        if (assetsLoaded) {
            dragon.loadTextures(dragonWalkFrames, dragonIdleFrames, dragonAttackFrames, dragonFireBreathFrames, dragonHurtFrames, dragonDeadFrames)
        }
        
        dragons.add(dragon)
        Log.d("EnemyManager", "Spawned dragon at ($x, $y). Total dragons: ${dragons.size}")
    }
    
    fun spawnRandomEnemy(playerX: Float, playerY: Float) {
        val currentTime = System.currentTimeMillis()
        
        // Check if assets are loaded first to prevent spawning before ready
        if (!assetsLoaded) {
            return
        }
        
        // Check spawn cooldown
        if (currentTime - lastSpawnTime < spawnCooldown) {
            return
        }
        
        // Check if we can spawn any enemy type
        val canSpawnSkeleton = skeletons.size < maxSkeletons
        val canSpawnDemon = demons.size < maxDemons
        val canSpawnDragon = dragons.size < maxDragons
        
        if (!canSpawnSkeleton && !canSpawnDemon && !canSpawnDragon) {
            Log.d("EnemyManager", "Cannot spawn any enemy: all limits reached")
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
                // Randomly choose enemy type to spawn from available options
                val availableTypes = mutableListOf<String>()
                if (canSpawnSkeleton) availableTypes.add("skeleton")
                if (canSpawnDemon) availableTypes.add("demon")
                if (canSpawnDragon) availableTypes.add("dragon")
                
                if (availableTypes.isNotEmpty()) {
                    val selectedType = availableTypes[Random.nextInt(availableTypes.size)]
                    
                    when (selectedType) {
                        "skeleton" -> spawnSkeleton(spawnX, spawnY)
                        "demon" -> spawnDemon(spawnX, spawnY)
                        "dragon" -> spawnDragon(spawnX, spawnY)
                    }
                    
                    lastSpawnTime = currentTime
                    Log.d("EnemyManager", "Spawned random $selectedType at ($spawnX, $spawnY)")
                    return
                }
            }
            
            attempts++
        }
        
        Log.d("EnemyManager", "Failed to find valid spawn location after $attempts attempts")
    }
    
    // Keep the old method for backwards compatibility
    fun spawnRandomSkeleton(playerX: Float, playerY: Float) {
        spawnRandomEnemy(playerX, playerY)
    }
    
    fun update(deltaTime: Float, playerX: Float, playerY: Float) {
        if (!assetsLoaded) return
        
        // ANR Prevention: Limit execution time
        val updateStartTime = System.currentTimeMillis()
        val maxUpdateTime = 8L // Maximum 8ms for enemy updates to prevent ANR
        
        // Enhanced culling and performance optimizations
        val updateRadius = 800f // Reduced from 1000f
        val maxUpdatesPerFrame = 3 // Increased slightly to handle 3 enemy types
        var updatesThisFrame = 0
        
        // Determine if heroes is in light (simplified - always true for now)
        val playerInLight = true
        
        // Update skeletons
        updateEnemyType(skeletons, skeletonDeathTimes, deltaTime, playerX, playerY, playerInLight, 
                      updateRadius, maxUpdateTime, updateStartTime, updatesThisFrame, maxUpdatesPerFrame, "skeleton")
        
        // Update demons
        updateEnemyType(demons, demonDeathTimes, deltaTime, playerX, playerY, playerInLight,
                      updateRadius, maxUpdateTime, updateStartTime, updatesThisFrame, maxUpdatesPerFrame, "demon")
        
        // Update dragons and their projectiles
        updateEnemyType(dragons, dragonDeathTimes, deltaTime, playerX, playerY, playerInLight,
                      updateRadius, maxUpdateTime, updateStartTime, updatesThisFrame, maxUpdatesPerFrame, "dragon")
        
        // Update dragon fire breath projectiles
        updateDragonProjectiles(deltaTime, playerX, playerY)
        
        // Reduce spawn frequency further to prevent performance issues
        if (System.currentTimeMillis() % 20 == 0L) { // Only try to spawn every 20th frame (was 10)
            spawnRandomEnemy(playerX, playerY)
        }
        
        // Debug log every 15 seconds (reduced frequency)
        if (System.currentTimeMillis() % 15000 < 100) {
            val updateTime = System.currentTimeMillis() - updateStartTime
            val totalActive = skeletons.count { it.isAlive() } + demons.count { it.isAlive() } + dragons.count { it.isAlive() }
            val totalDead = skeletons.count { it.isDead() } + demons.count { it.isDead() } + dragons.count { it.isDead() }
            Log.d("EnemyManager", "Active: $totalActive (S:${skeletons.count { it.isAlive() }}, D:${demons.count { it.isAlive() }}, Dr:${dragons.count { it.isAlive() }}), Dead: $totalDead, Time: ${updateTime}ms")
        }
    }
    
    private fun <T> updateEnemyType(
        enemies: MutableList<T>, 
        deathTimes: MutableMap<T, Long>,
        deltaTime: Float, 
        playerX: Float, 
        playerY: Float, 
        playerInLight: Boolean,
        updateRadius: Float,
        maxUpdateTime: Long,
        updateStartTime: Long,
        updatesThisFrame: Int,
        maxUpdatesPerFrame: Int,
        enemyType: String
    ) where T : Any {
        var currentUpdates = updatesThisFrame
        val iterator = enemies.iterator()
        
        while (iterator.hasNext() && currentUpdates < maxUpdatesPerFrame) {
            // ANR Prevention: Check if we're taking too long
            if (System.currentTimeMillis() - updateStartTime > maxUpdateTime) {
                Log.w("EnemyManager", "Breaking $enemyType update loop to prevent ANR")
                break
            }
            
            val enemy = iterator.next()
            
            // Get enemy position based on type
            val (enemyX, enemyY, isAlive, isDead) = when (enemy) {
                is Skeleton -> Tuple4(enemy.getX(), enemy.getY(), enemy.isAlive(), enemy.isDead())
                is Demon -> Tuple4(enemy.getX(), enemy.getY(), enemy.isAlive(), enemy.isDead())
                is Dragon -> Tuple4(enemy.getX(), enemy.getY(), enemy.isAlive(), enemy.isDead())
                else -> continue
            }
            
            // Calculate distance once (optimized)
            val dx = enemyX - playerX
            val dy = enemyY - playerY
            val distanceSquared = dx * dx + dy * dy
            
            // Skip expensive sqrt for far objects
            if (distanceSquared > (updateRadius * updateRadius * 4)) {
                // Very far enemies - minimal updates
                if (System.currentTimeMillis() % 10 == 0L) { // Update every 10th frame
                    val wasAlive = isAlive
                    
                    when (enemy) {
                        is Skeleton -> enemy.update(deltaTime * 0.1f, playerX, playerY, playerInLight)
                        is Demon -> enemy.update(deltaTime * 0.1f, playerX, playerY, playerInLight)
                        is Dragon -> enemy.update(deltaTime * 0.1f, playerX, playerY, playerInLight)
                    }
                    
                    if (wasAlive && isDead && !deathTimes.containsKey(enemy)) {
                        deathTimes[enemy] = System.currentTimeMillis()
                    }
                }
                continue
            }
            
            val distance = kotlin.math.sqrt(distanceSquared)
            
            // Enhanced culling: only update enemies near heroes
            if (distance < updateRadius) {
                // Track when enemy dies for cleanup
                val wasAlive = isAlive
                
                when (enemy) {
                    is Skeleton -> enemy.update(deltaTime, playerX, playerY, playerInLight)
                    is Demon -> enemy.update(deltaTime, playerX, playerY, playerInLight)
                    is Dragon -> {
                        enemy.update(deltaTime, playerX, playerY, playerInLight)
                        // Add any fire breath projectiles created by the dragon
                        val newProjectiles = enemy.getNewProjectiles()
                        dragonProjectiles.addAll(newProjectiles)
                    }
                }
                
                // Record death time if enemy just died
                if (wasAlive && isDead && !deathTimes.containsKey(enemy)) {
                    deathTimes[enemy] = System.currentTimeMillis()
                }
                
                currentUpdates++
            } else if (distance < updateRadius * 2f) {
                // Medium distance enemies - reduced update frequency
                if (System.currentTimeMillis() % 5 == 0L) { // Update every 5th frame
                    val wasAlive = isAlive
                    
                    when (enemy) {
                        is Skeleton -> enemy.update(deltaTime * 0.5f, playerX, playerY, playerInLight)
                        is Demon -> enemy.update(deltaTime * 0.5f, playerX, playerY, playerInLight)
                        is Dragon -> enemy.update(deltaTime * 0.5f, playerX, playerY, playerInLight)
                    }
                    
                    if (wasAlive && isDead && !deathTimes.containsKey(enemy)) {
                        deathTimes[enemy] = System.currentTimeMillis()
                    }
                }
            }
            
            // Remove dead enemies after their death animation (3 seconds)
            if (isDead) {
                val deathTime = deathTimes[enemy] ?: System.currentTimeMillis()
                if (System.currentTimeMillis() - deathTime > 3000) {
                    iterator.remove()
                    deathTimes.remove(enemy)
                    Log.d("EnemyManager", "Removed dead $enemyType after 3 seconds")
                }
            }
        }
    }
    
    private fun updateDragonProjectiles(deltaTime: Float, playerX: Float, playerY: Float) {
        val iterator = dragonProjectiles.iterator()
        while (iterator.hasNext()) {
            val projectile = iterator.next()
            projectile.update(deltaTime)
            
            if (projectile.shouldRemove()) {
                iterator.remove()
            }
        }
    }
    
    // Helper data class for enemy info
    private data class Tuple4<T1, T2, T3, T4>(val first: T1, val second: T2, val third: T3, val fourth: T4)
    
    fun draw(canvas: Canvas, cameraX: Float, cameraY: Float) {
        if (!assetsLoaded) return
        
        // Viewport culling for drawing
        val screenWidth = canvas.width
        val screenHeight = canvas.height
        val cullingMargin = 100f // Extra margin to prevent pop-in
        
        // Draw skeletons
        skeletons.forEach { skeleton ->
            val screenX = skeleton.getX() - cameraX
            val screenY = skeleton.getY() - cameraY
            
            if (screenX > -cullingMargin && screenX < screenWidth + cullingMargin &&
                screenY > -cullingMargin && screenY < screenHeight + cullingMargin) {
                skeleton.draw(canvas, cameraX, cameraY)
            }
        }
        
        // Draw demons
        demons.forEach { demon ->
            val screenX = demon.getX() - cameraX
            val screenY = demon.getY() - cameraY
            
            if (screenX > -cullingMargin && screenX < screenWidth + cullingMargin &&
                screenY > -cullingMargin && screenY < screenHeight + cullingMargin) {
                demon.draw(canvas, cameraX, cameraY)
            }
        }
        
        // Draw dragons
        dragons.forEach { dragon ->
            val screenX = dragon.getX() - cameraX
            val screenY = dragon.getY() - cameraY
            
            if (screenX > -cullingMargin && screenX < screenWidth + cullingMargin &&
                screenY > -cullingMargin && screenY < screenHeight + cullingMargin) {
                dragon.draw(canvas, cameraX, cameraY)
            }
        }
        
        // Draw dragon fire breath projectiles
        dragonProjectiles.forEach { projectile ->
            val screenX = projectile.getX() - cameraX
            val screenY = projectile.getY() - cameraY
            
            if (screenX > -cullingMargin && screenX < screenWidth + cullingMargin &&
                screenY > -cullingMargin && screenY < screenHeight + cullingMargin) {
                projectile.draw(canvas, cameraX, cameraY)
            }
        }
    }
    
    // Combat system integration - updated for all enemy types
    fun checkFireballCollisions(fireballs: List<Fireball>): Int {
        var hitCount = 0
        
        fireballs.forEach { fireball ->
            if (!fireball.isExploding() && fireball.isActive()) {
                // Check skeleton collisions
                skeletons.forEach { skeleton ->
                    if (skeleton.isAlive()) {
                        val dx = fireball.getX() - skeleton.getX()
                        val dy = fireball.getY() - skeleton.getY()
                        val distance = sqrt(dx * dx + dy * dy)
                        
                        if (distance <= (skeleton.getWidth() / 2f + 64f)) { // 64f is fireball radius
                            skeleton.takeDamage(10) // 10 damage per fireball hit
                            fireball.triggerExplosion()
                            hitCount++
                            Log.d("EnemyManager", "Fireball hit skeleton! Skeleton health: ${skeleton.getHealth()}")
                        }
                    }
                }
                
                // Check demon collisions
                demons.forEach { demon ->
                    if (demon.isAlive()) {
                        val dx = fireball.getX() - demon.getX()
                        val dy = fireball.getY() - demon.getY()
                        val distance = sqrt(dx * dx + dy * dy)
                        
                        if (distance <= (demon.getWidth() / 2f + 64f)) {
                            demon.takeDamage(10) // 10 damage per fireball hit
                            fireball.triggerExplosion()
                            hitCount++
                            Log.d("EnemyManager", "Fireball hit demon! Demon health: ${demon.getHealth()}")
                        }
                    }
                }
                
                // Check dragon collisions
                dragons.forEach { dragon ->
                    if (dragon.isAlive()) {
                        val dx = fireball.getX() - dragon.getX()
                        val dy = fireball.getY() - dragon.getY()
                        val distance = sqrt(dx * dx + dy * dy)
                        
                        if (distance <= (dragon.getWidth() / 2f + 64f)) {
                            dragon.takeDamage(15) // Dragons take more damage
                            fireball.triggerExplosion()
                            hitCount++
                            Log.d("EnemyManager", "Fireball hit dragon! Dragon health: ${dragon.getHealth()}")
                        }
                    }
                }
            }
        }
        
        return hitCount
    }
    
    // Check attacks from all enemy types
    fun checkEnemyAttacks(playerX: Float, playerY: Float): Int {
        var totalDamage = 0
        
        // Check skeleton attacks
        skeletons.forEach { skeleton ->
            val damage = skeleton.getAndClearLastAttackDamage()
            if (damage > 0) {
                totalDamage += damage
                Log.d("EnemyManager", "Skeleton dealt $damage damage to heroes!")
            }
        }
        
        // Check demon attacks
        demons.forEach { demon ->
            val damage = demon.getAndClearLastAttackDamage()
            if (damage > 0) {
                totalDamage += damage
                Log.d("EnemyManager", "Demon dealt $damage damage to heroes!")
            }
        }
        
        // Check dragon attacks
        dragons.forEach { dragon ->
            val damage = dragon.getAndClearLastAttackDamage()
            if (damage > 0) {
                totalDamage += damage
                Log.d("EnemyManager", "Dragon dealt $damage damage to heroes!")
            }
        }
        
        // Check dragon fire breath projectile collisions with heroes
        dragonProjectiles.forEach { projectile ->
            if (projectile.checkPlayerCollision(playerX, playerY)) {
                totalDamage += 20 // Fire breath does significant damage
                Log.d("EnemyManager", "Dragon fire breath hit heroes for 20 damage!")
            }
        }
        
        return totalDamage
    }
    
    // Legacy method name for backwards compatibility
    fun checkSkeletonAttacks(playerX: Float, playerY: Float): Int {
        return checkEnemyAttacks(playerX, playerY)
    }
    
    // Get enemy counts for UI - updated for all enemy types
    fun getSkeletonCount(): Int = skeletons.size
    fun getAliveSkeletonCount(): Int = skeletons.count { it.isAlive() }
    fun getDeadSkeletonCount(): Int = skeletons.count { it.isDead() }
    
    fun getDemonCount(): Int = demons.size
    fun getAliveDemonCount(): Int = demons.count { it.isAlive() }
    fun getDeadDemonCount(): Int = demons.count { it.isDead() }
    
    fun getDragonCount(): Int = dragons.size
    fun getAliveDragonCount(): Int = dragons.count { it.isAlive() }
    fun getDeadDragonCount(): Int = dragons.count { it.isDead() }
    
    fun getTotalEnemyCount(): Int = skeletons.size + demons.size + dragons.size
    fun getTotalAliveEnemyCount(): Int = getAliveSkeletonCount() + getAliveDemonCount() + getAliveDragonCount()
    fun getTotalDeadEnemyCount(): Int = getDeadSkeletonCount() + getDeadDemonCount() + getDeadDragonCount()
    
    // Clear all enemies (for level transitions)
    fun clearAllEnemies() {
        skeletons.clear()
        skeletonDeathTimes.clear()
        demons.clear()
        demonDeathTimes.clear()
        dragons.clear()
        dragonDeathTimes.clear()
        dragonProjectiles.clear()
        Log.d("EnemyManager", "All enemies cleared")
    }
    
    // Legacy method for backwards compatibility
    fun clearAllSkeletons() {
        clearAllEnemies()
    }
    
    // Spawn initial enemies for level start
    fun spawnInitialEnemies(playerX: Float, playerY: Float, count: Int = 3) {
        repeat(count) {
            spawnRandomEnemy(playerX, playerY)
        }
        Log.d("EnemyManager", "Spawned $count initial enemies")
    }
    
    // Legacy method for backwards compatibility
    fun spawnInitialSkeletons(playerX: Float, playerY: Float, count: Int = 3) {
        spawnInitialEnemies(playerX, playerY, count)
    }
    
    // Get all skeletons (for debug purposes)
    fun getAllSkeletons(): List<Skeleton> = skeletons.toList()
    
    // Get all demons (for debug purposes)
    fun getAllDemons(): List<Demon> = demons.toList()
    
    // Get all dragons (for debug purposes)
    fun getAllDragons(): List<Dragon> = dragons.toList()
    
    // Check if assets are loaded
    fun areAssetsLoaded(): Boolean = assetsLoaded
    
    /**
     * Check arrow collisions with enemies
     */
    fun checkArrowCollisions(arrows: List<com.example.mygame.game.entities.projectiles.Arrow>): Int {
        var hitCount = 0
        
        arrows.forEach { arrow ->
            if (!arrow.isDestroyed()) {
                // Check skeleton collisions
                skeletons.forEach { skeleton ->
                    if (skeleton.isAlive()) {
                        val skeletonBounds = android.graphics.RectF(
                            skeleton.getX(),
                            skeleton.getY(),
                            skeleton.getX() + skeleton.getWidth(),
                            skeleton.getY() + skeleton.getHeight()
                        )
                        
                        if (arrow.checkCollision(skeletonBounds.left, skeletonBounds.top, 
                                skeletonBounds.width(), skeletonBounds.height())) {
                            skeleton.takeDamage(arrow.getDamage().toInt())
                            hitCount++
                            Log.d("EnemyManager", "Arrow hit skeleton! Skeleton health: ${skeleton.getHealth()}")
                        }
                    }
                }
                
                // Check demon collisions
                demons.forEach { demon ->
                    if (demon.isAlive()) {
                        if (arrow.checkCollision(demon.getX(), demon.getY(), demon.getWidth(), demon.getHeight())) {
                            demon.takeDamage(arrow.getDamage().toInt())
                            hitCount++
                            Log.d("EnemyManager", "Arrow hit demon! Demon health: ${demon.getHealth()}")
                        }
                    }
                }
                
                // Check dragon collisions
                dragons.forEach { dragon ->
                    if (dragon.isAlive()) {
                        if (arrow.checkCollision(dragon.getX(), dragon.getY(), dragon.getWidth(), dragon.getHeight())) {
                            dragon.takeDamage(arrow.getDamage().toInt())
                            hitCount++
                            Log.d("EnemyManager", "Arrow hit dragon! Dragon health: ${dragon.getHealth()}")
                        }
                    }
                }
            }
        }
        
        return hitCount
    }
    
    /**
     * Check melee attack collisions with enemies
     */
    fun checkMeleeAttackCollisions(attackHitbox: android.graphics.RectF?): Int {
        if (attackHitbox == null) return 0
        
        var hitCount = 0
        val meleeAttackDamage = 25 // Melee attack damage
        
        // Check skeleton collisions
        skeletons.forEach { skeleton ->
            if (skeleton.isAlive()) {
                val skeletonBounds = android.graphics.RectF(
                    skeleton.getX(),
                    skeleton.getY(),
                    skeleton.getX() + skeleton.getWidth(),
                    skeleton.getY() + skeleton.getHeight()
                )
                
                if (android.graphics.RectF.intersects(attackHitbox, skeletonBounds)) {
                    skeleton.takeDamage(meleeAttackDamage)
                    hitCount++
                    Log.d("EnemyManager", "Melee attack hit skeleton! Skeleton health: ${skeleton.getHealth()}")
                }
            }
        }
        
        // Check demon collisions  
        demons.forEach { demon ->
            if (demon.isAlive()) {
                val demonBounds = android.graphics.RectF(
                    demon.getX(),
                    demon.getY(),
                    demon.getX() + demon.getWidth(),
                    demon.getY() + demon.getHeight()
                )
                
                if (android.graphics.RectF.intersects(attackHitbox, demonBounds)) {
                    demon.takeDamage(meleeAttackDamage)
                    hitCount++
                    Log.d("EnemyManager", "Melee attack hit demon! Demon health: ${demon.getHealth()}")
                }
            }
        }
        
        // Check dragon collisions
        dragons.forEach { dragon ->
            if (dragon.isAlive()) {
                val dragonBounds = android.graphics.RectF(
                    dragon.getX(),
                    dragon.getY(),
                    dragon.getX() + dragon.getWidth(),
                    dragon.getY() + dragon.getHeight()
                )
                
                if (android.graphics.RectF.intersects(attackHitbox, dragonBounds)) {
                    dragon.takeDamage(meleeAttackDamage)
                    hitCount++
                    Log.d("EnemyManager", "Melee attack hit dragon! Dragon health: ${dragon.getHealth()}")
                }
            }
        }
        
        return hitCount
    }
    
    // Cleanup resources
    fun cleanup() {
        // Clean up skeleton assets
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
        
        // Clean up demon assets
        demonWalkFrames.forEach { it.recycle() }
        demonIdleFrames.forEach { it.recycle() }
        demonAttackFrames.forEach { it.recycle() }
        demonHurtFrames.forEach { it.recycle() }
        demonDeadFrames.forEach { it.recycle() }
        
        demonWalkFrames.clear()
        demonIdleFrames.clear()
        demonAttackFrames.clear()
        demonHurtFrames.clear()
        demonDeadFrames.clear()
        
        // Clean up dragon assets
        dragonWalkFrames.forEach { it.recycle() }
        dragonIdleFrames.forEach { it.recycle() }
        dragonAttackFrames.forEach { it.recycle() }
        dragonFireBreathFrames.forEach { it.recycle() }
        dragonHurtFrames.forEach { it.recycle() }
        dragonDeadFrames.forEach { it.recycle() }
        
        dragonWalkFrames.clear()
        dragonIdleFrames.clear()
        dragonAttackFrames.clear()
        dragonFireBreathFrames.clear()
        dragonHurtFrames.clear()
        dragonDeadFrames.clear()
        
        // Clear all enemies and related data
        skeletons.clear()
        skeletonDeathTimes.clear()
        demons.clear()
        demonDeathTimes.clear()
        dragons.clear()
        dragonDeathTimes.clear()
        dragonProjectiles.clear()
        
        Log.d("EnemyManager", "EnemyManager cleaned up")
    }
}