package com.example.mygame.game.managers

import android.content.Context
import android.graphics.*
import android.util.Log
import com.example.mygame.game.entities.Fireball
import com.example.mygame.game.entities.projectiles.Arrow
import com.example.mygame.game.entities.BaseHero
import com.example.mygame.game.assets.GameAssetManager

class ProjectileManager(private val context: Context) {
    private val fireballs = mutableListOf<Fireball>()
    private val arrows = mutableListOf<Arrow>()
    private var flameFrames: List<Bitmap> = emptyList()
    private var explosionFrames: List<Bitmap> = emptyList()
    
    // Fireball settings
    private val maxFireballs = 10 // Limit concurrent fireballs
    private var lastFireballTime = 0L
    private val fireballCooldown = 300L // 300ms cooldown between fireballs
    
    fun initialize(assetManager: GameAssetManager) {
        loadFireballTextures()
    }
    
    private fun loadFireballTextures() {
        try {
            // Load flame animation frames
            val flameFramesList = mutableListOf<Bitmap>()
            val flameAssets = context.assets.list("textures/effects/fireball/flame")
            
            flameAssets?.sorted()?.forEach { fileName ->
                if (fileName.endsWith(".png") || fileName.endsWith(".jpg")) {
                    try {
                        val inputStream = context.assets.open("textures/effects/fireball/flame/$fileName")
                        val bitmap = BitmapFactory.decodeStream(inputStream)
                        inputStream.close()
                        
                        if (bitmap != null) {
                            flameFramesList.add(bitmap)
                            Log.d("ProjectileManager", "Loaded flame frame: $fileName")
                        }
                    } catch (e: Exception) {
                        Log.e("ProjectileManager", "Error loading flame frame $fileName: ${e.message}")
                    }
                }
            }
            
            // Load explosion animation frames
            val explosionFramesList = mutableListOf<Bitmap>()
            val explosionAssets = context.assets.list("textures/effects/fireball/explosion")
            
            explosionAssets?.sorted()?.forEach { fileName ->
                if (fileName.endsWith(".png") || fileName.endsWith(".jpg")) {
                    try {
                        val inputStream = context.assets.open("textures/effects/fireball/explosion/$fileName")
                        val bitmap = BitmapFactory.decodeStream(inputStream)
                        inputStream.close()
                        
                        if (bitmap != null) {
                            explosionFramesList.add(bitmap)
                            Log.d("ProjectileManager", "Loaded explosion frame: $fileName")
                        }
                    } catch (e: Exception) {
                        Log.e("ProjectileManager", "Error loading explosion frame $fileName: ${e.message}")
                    }
                }
            }
            
            flameFrames = flameFramesList
            explosionFrames = explosionFramesList
            
            Log.d("ProjectileManager", "Loaded ${flameFrames.size} flame frames and ${explosionFrames.size} explosion frames")
            
        } catch (e: Exception) {
            Log.e("ProjectileManager", "Error loading fireball textures: ${e.message}")
            
            // Create fallback colored rectangles if assets fail to load
            createFallbackTextures()
        }
    }
    
    private fun createFallbackTextures() {
        // Create simple colored bitmaps as fallback
        val flameFramesList = mutableListOf<Bitmap>()
        val explosionFramesList = mutableListOf<Bitmap>()
        
        // Create 4 flame frames with different colors
        val flameColors = intArrayOf(
            Color.rgb(255, 100, 0),  // Orange
            Color.rgb(255, 150, 0),  // Light orange
            Color.rgb(255, 200, 50), // Yellow-orange
            Color.rgb(255, 255, 100) // Yellow
        )
        
        for (color in flameColors) {
            val bitmap = Bitmap.createBitmap(32, 32, Bitmap.Config.ARGB_8888)
            val canvas = Canvas(bitmap)
            val paint = Paint().apply {
                this.color = color
                style = Paint.Style.FILL
            }
            canvas.drawCircle(16f, 16f, 14f, paint)
            flameFramesList.add(bitmap)
        }
        
        // Create 6 explosion frames with different sizes and colors
        val explosionColors = intArrayOf(
            Color.rgb(255, 255, 0),   // Bright yellow
            Color.rgb(255, 200, 0),   // Orange-yellow
            Color.rgb(255, 150, 0),   // Orange
            Color.rgb(255, 100, 50),  // Red-orange
            Color.rgb(200, 100, 50),  // Dark orange
            Color.rgb(150, 150, 150)  // Gray smoke
        )
        
        for (i in explosionColors.indices) {
            val bitmap = Bitmap.createBitmap(64, 64, Bitmap.Config.ARGB_8888)
            val canvas = Canvas(bitmap)
            val paint = Paint().apply {
                color = explosionColors[i]
                style = Paint.Style.FILL
            }
            val radius = 30f - (i * 3f) // Decreasing size
            canvas.drawCircle(32f, 32f, radius, paint)
            explosionFramesList.add(bitmap)
        }
        
        flameFrames = flameFramesList
        explosionFrames = explosionFramesList
        
        Log.d("ProjectileManager", "Created fallback textures: ${flameFrames.size} flame, ${explosionFrames.size} explosion")
    }
    
    fun createFireball(startX: Float, startY: Float, targetX: Float, targetY: Float): Boolean {
        val currentTime = System.currentTimeMillis()
        
        // Check cooldown
        if (currentTime - lastFireballTime < fireballCooldown) {
            return false
        }
        
        // Check max limit
        if (fireballs.size >= maxFireballs) {
            return false
        }
        
        // Create new fireball
        val fireball = Fireball(startX, startY, targetX, targetY)
        fireball.loadTextures(flameFrames, explosionFrames)
        fireballs.add(fireball)
        lastFireballTime = currentTime
        
        Log.d("ProjectileManager", "Created fireball #${fireballs.size} from ($startX, $startY) to ($targetX, $targetY)")
        return true
    }
    
    // Bắn đạn theo hướng nhân vật di chuyển
    fun createFireballByDirection(startX: Float, startY: Float, facingDirection: BaseHero.Direction): Boolean {
        val currentTime = System.currentTimeMillis()
        
        // Check cooldown
        if (currentTime - lastFireballTime < fireballCooldown) {
            return false
        }
        
        // Check max limit
        if (fireballs.size >= maxFireballs) {
            return false
        }
        
        // Tính toán điểm đích dựa trên hướng di chuyển
        val fireballRange = 800f // Tầm xa của đạn
        val targetX = when (facingDirection) {
            BaseHero.Direction.LEFT -> startX - fireballRange
            BaseHero.Direction.RIGHT -> startX + fireballRange
            else -> startX
        }
        val targetY = when (facingDirection) {
            BaseHero.Direction.BACK -> startY - fireballRange
            BaseHero.Direction.FRONT -> startY + fireballRange
            else -> startY
        }
        
        // Create new fireball
        val fireball = Fireball(startX, startY, targetX, targetY)
        fireball.loadTextures(flameFrames, explosionFrames)
        fireballs.add(fireball)
        lastFireballTime = currentTime
        
        Log.d("ProjectileManager", "Created directional fireball #${fireballs.size} from ($startX, $startY) facing $facingDirection to ($targetX, $targetY)")
        return true
    }
    
    fun update(deltaTime: Float, mapManager: MapManager) {
        // Update all fireballs
        fireballs.forEach { fireball ->
            fireball.update(deltaTime, mapManager)
        }
        
        // Remove inactive fireballs
        val iterator = fireballs.iterator()
        while (iterator.hasNext()) {
            val fireball = iterator.next()
            if (!fireball.isActive()) {
                iterator.remove()
                Log.d("ProjectileManager", "Removed inactive fireball, remaining: ${fireballs.size}")
            }
        }
    }
    
    fun draw(canvas: Canvas, cameraX: Float, cameraY: Float) {
        fireballs.forEach { fireball ->
            fireball.draw(canvas, cameraX, cameraY)
        }
    }
    
    fun getActiveFireballCount(): Int = fireballs.size
    
    fun getActiveFireballs(): List<Fireball> = fireballs.toList()
    
    fun canCreateFireball(): Boolean {
        val currentTime = System.currentTimeMillis()
        return (currentTime - lastFireballTime >= fireballCooldown) && (fireballs.size < maxFireballs)
    }
    
    fun cleanup() {
        fireballs.clear()
        
        // Recycle bitmaps if they're not from assets
        flameFrames.forEach { bitmap ->
            if (!bitmap.isRecycled) {
                bitmap.recycle()
            }
        }
        explosionFrames.forEach { bitmap ->
            if (!bitmap.isRecycled) {
                bitmap.recycle()
            }
        }
        
        flameFrames = emptyList()
        explosionFrames = emptyList()
    }
    
    // Clear all projectiles (for game restart)
    fun clearAllProjectiles() {
        fireballs.clear()
        Log.d("ProjectileManager", "All projectiles cleared")
    }
    
    // Get fireball collision info for damage system (future use)
    fun getFireballsInArea(centerX: Float, centerY: Float, radius: Float): List<Fireball> {
        return fireballs.filter { fireball ->
            if (!fireball.isExploding() && fireball.isActive()) {
                val dx = fireball.getX() - centerX
                val dy = fireball.getY() - centerY
                val distance = kotlin.math.sqrt(dx * dx + dy * dy)
                distance <= radius
            } else {
                false
            }
        }
    }
}