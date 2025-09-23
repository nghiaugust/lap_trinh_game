package com.example.mygame.game.managers

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Paint
import com.example.mygame.R
import kotlin.math.floor

class BackgroundManager(private val context: Context) {
    
    private var tileBitmaps = mutableListOf<Bitmap>()
    private var tileWidth = 0
    private var tileHeight = 0
    
    // Tile pattern for variety
    private val tilePattern = Array(20) { Array(15) { 0 } }  // 20x15 grid of tile types
    
    // Camera position for scrolling background
    var cameraX = 0f
    var cameraY = 0f
    
    // World size (larger than screen)
    private val worldWidth = 3000f
    private val worldHeight = 2000f
    
    init {
        loadTileTextures()
        generateTilePattern()
    }
    
    private fun loadTileTextures() {
        try {
            // Load and create multiple tile variations from the same background
            val originalBitmap = BitmapFactory.decodeResource(context.resources, R.drawable.bg)
            
            // Create different tile sizes and rotations for variety
            val tileSize = 256
            tileWidth = tileSize
            tileHeight = tileSize
            
            // Original tile
            tileBitmaps.add(Bitmap.createScaledBitmap(originalBitmap, tileSize, tileSize, true))
            
            // Rotated variations
            val rotated90 = rotateBitmap(originalBitmap, 90f)
            tileBitmaps.add(Bitmap.createScaledBitmap(rotated90, tileSize, tileSize, true))
            
            val rotated180 = rotateBitmap(originalBitmap, 180f)
            tileBitmaps.add(Bitmap.createScaledBitmap(rotated180, tileSize, tileSize, true))
            
            val rotated270 = rotateBitmap(originalBitmap, 270f)
            tileBitmaps.add(Bitmap.createScaledBitmap(rotated270, tileSize, tileSize, true))
            
            // Flipped variations
            val flippedH = flipBitmap(originalBitmap, true, false)
            tileBitmaps.add(Bitmap.createScaledBitmap(flippedH, tileSize, tileSize, true))
            
            val flippedV = flipBitmap(originalBitmap, false, true)
            tileBitmaps.add(Bitmap.createScaledBitmap(flippedV, tileSize, tileSize, true))
            
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
    
    private fun rotateBitmap(bitmap: Bitmap, degrees: Float): Bitmap {
        val matrix = android.graphics.Matrix()
        matrix.postRotate(degrees)
        return Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
    }
    
    private fun flipBitmap(bitmap: Bitmap, horizontal: Boolean, vertical: Boolean): Bitmap {
        val matrix = android.graphics.Matrix()
        if (horizontal) matrix.preScale(-1f, 1f)
        if (vertical) matrix.preScale(1f, -1f)
        return Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
    }
    
    private fun generateTilePattern() {
        // Create a random but consistent pattern of tile types
        for (y in tilePattern.indices) {
            for (x in tilePattern[y].indices) {
                // Use a simple algorithm to create varied but not random pattern
                val value = (x * 3 + y * 5 + x * y) % tileBitmaps.size
                tilePattern[y][x] = value
            }
        }
    }
    
    fun updateCamera(playerX: Float, playerY: Float, screenWidth: Int, screenHeight: Int) {
        // Center camera on heroes
        cameraX = playerX - screenWidth / 2f
        cameraY = playerY - screenHeight / 2f
        
        // Clamp camera to world bounds
        val maxCameraX = worldWidth - screenWidth
        val maxCameraY = worldHeight - screenHeight
        
        cameraX = cameraX.coerceIn(0f, maxCameraX)
        cameraY = cameraY.coerceIn(0f, maxCameraY)
    }
    
    fun draw(canvas: Canvas, paint: Paint, screenWidth: Int, screenHeight: Int) {
        if (tileBitmaps.isEmpty()) return
        
        // Calculate which tiles are visible
        val startTileX = kotlin.math.floor((cameraX / tileWidth)).toInt()
        val startTileY = kotlin.math.floor((cameraY / tileHeight)).toInt()
        val endTileX = startTileX + (screenWidth / tileWidth) + 2
        val endTileY = startTileY + (screenHeight / tileHeight) + 2
        
        // Draw visible tiles with pattern
        for (tileY in startTileY..endTileY) {
            for (tileX in startTileX..endTileX) {
                val drawX = (tileX * tileWidth) - cameraX
                val drawY = (tileY * tileHeight) - cameraY
                
                // Only draw if tile is within screen bounds
                if (drawX > -tileWidth && drawX < screenWidth + tileWidth &&
                    drawY > -tileHeight && drawY < screenHeight + tileHeight) {
                    
                    // Get tile type from pattern (with wrapping)
                    val patternX = ((tileX % tilePattern[0].size) + tilePattern[0].size) % tilePattern[0].size
                    val patternY = ((tileY % tilePattern.size) + tilePattern.size) % tilePattern.size
                    val tileType = tilePattern[patternY][patternX]
                    
                    // Draw the appropriate tile
                    if (tileType < tileBitmaps.size) {
                        canvas.drawBitmap(tileBitmaps[tileType], drawX, drawY, paint)
                    }
                }
            }
        }
    }
    
    fun getWorldWidth(): Float = worldWidth
    fun getWorldHeight(): Float = worldHeight
    
    fun destroy() {
        tileBitmaps.forEach { it.recycle() }
        tileBitmaps.clear()
    }
}
