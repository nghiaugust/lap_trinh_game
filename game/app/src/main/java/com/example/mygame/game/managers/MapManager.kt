package com.example.mygame.game.managers

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Paint
import com.example.mygame.R
import java.io.BufferedReader
import java.io.InputStreamReader
import kotlin.math.floor

class MapManager(private val context: Context) {
    
    // Map data
    private var mapData: Array<CharArray> = emptyArray()
    private var mapWidth = 0
    private var mapHeight = 0
    
    // Tile settings
    private val tileSize = 64f  // Each tile is 64x64 pixels
    
    // Tile textures
    private var floorTexture: Bitmap? = null
    private var wallTexture: Bitmap? = null
    
    // Camera position
    var cameraX = 0f
    var cameraY = 0f
    
    // World size in pixels
    private var worldWidth = 0f
    private var worldHeight = 0f
    
    // Player spawn position
    var playerStartX = 0f
    var playerStartY = 0f
    
    init {
        loadMapData()
        loadTextures()
        calculateWorldSize()
        findPlayerStartPosition()
    }
    
    private fun loadMapData() {
        try {
            val inputStream = context.assets.open("data/maze_map_data.txt")
            val reader = BufferedReader(InputStreamReader(inputStream))
            val lines = mutableListOf<String>()
            
            reader.useLines { sequence ->
                sequence.forEach { line ->
                    if (line.isNotEmpty()) {
                        lines.add(line)
                    }
                }
            }
            
            if (lines.isNotEmpty()) {
                mapHeight = lines.size
                mapWidth = lines[0].length
                mapData = Array(mapHeight) { CharArray(mapWidth) }
                
                for (y in 0 until mapHeight) {
                    val line = lines[y]
                    for (x in 0 until mapWidth) {
                        mapData[y][x] = if (x < line.length) line[x] else '1'
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
            // Create a default small map if loading fails
            createDefaultMap()
        }
    }
    
    private fun createDefaultMap() {
        mapWidth = 10
        mapHeight = 10
        mapData = Array(mapHeight) { CharArray(mapWidth) }
        
        // Fill with walls except center
        for (y in 0 until mapHeight) {
            for (x in 0 until mapWidth) {
                mapData[y][x] = if (x == 0 || y == 0 || x == mapWidth - 1 || y == mapHeight - 1) '1' else '0'
            }
        }
    }
    
    private fun loadTextures() {
        try {
            // Load floor texture
            val floorBitmap = BitmapFactory.decodeStream(
                context.assets.open("textures/environment/floors/floor.png")
            )
            floorTexture = Bitmap.createScaledBitmap(
                floorBitmap, 
                tileSize.toInt(), 
                tileSize.toInt(), 
                false
            )
            
            // Load wall texture  
            val wallBitmap = BitmapFactory.decodeStream(
                context.assets.open("textures/environment/walls/wall.png")
            )
            wallTexture = Bitmap.createScaledBitmap(
                wallBitmap, 
                tileSize.toInt(), 
                tileSize.toInt(), 
                false
            )
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
    
    private fun calculateWorldSize() {
        worldWidth = mapWidth * tileSize
        worldHeight = mapHeight * tileSize
    }
    
    private fun findPlayerStartPosition() {
        // Find entrance position (first '0' on the left side)
        for (y in 0 until mapHeight) {
            for (x in 0 until mapWidth) {
                if (mapData[y][x] == '0' && x < mapWidth / 4) { // Look in left quarter
                    playerStartX = (x + 0.5f) * tileSize
                    playerStartY = (y + 0.5f) * tileSize
                    return
                }
            }
        }
        
        // Fallback to center if no entrance found
        playerStartX = worldWidth / 2f
        playerStartY = worldHeight / 2f
    }
    
    fun updateCamera(playerX: Float, playerY: Float, screenWidth: Int, screenHeight: Int) {
        // Center camera on player
        cameraX = playerX - screenWidth / 2f
        cameraY = playerY - screenHeight / 2f
        
        // Clamp camera to world bounds
        val maxCameraX = worldWidth - screenWidth
        val maxCameraY = worldHeight - screenHeight
        
        cameraX = cameraX.coerceIn(0f, maxCameraX.coerceAtLeast(0f))
        cameraY = cameraY.coerceIn(0f, maxCameraY.coerceAtLeast(0f))
    }
    
    fun draw(canvas: Canvas, paint: Paint, screenWidth: Int, screenHeight: Int) {
        if (floorTexture == null || wallTexture == null) return
        
        // Calculate visible tile range
        val startTileX = (cameraX / tileSize).toInt().coerceAtLeast(0)
        val startTileY = (cameraY / tileSize).toInt().coerceAtLeast(0)
        val endTileX = ((cameraX + screenWidth) / tileSize).toInt().coerceAtMost(mapWidth - 1)
        val endTileY = ((cameraY + screenHeight) / tileSize).toInt().coerceAtMost(mapHeight - 1)
        
        // Draw visible tiles
        for (tileY in startTileY..endTileY) {
            for (tileX in startTileX..endTileX) {
                if (tileY < mapHeight && tileX < mapWidth) {
                    val drawX = (tileX * tileSize) - cameraX
                    val drawY = (tileY * tileSize) - cameraY
                    
                    val texture = if (mapData[tileY][tileX] == '0') floorTexture else wallTexture
                    texture?.let { bitmap ->
                        canvas.drawBitmap(bitmap, drawX, drawY, paint)
                    }
                }
            }
        }
    }
    
    fun isWall(x: Float, y: Float): Boolean {
        val tileX = (x / tileSize).toInt()
        val tileY = (y / tileSize).toInt()
        
        // Check bounds
        if (tileX < 0 || tileX >= mapWidth || tileY < 0 || tileY >= mapHeight) {
            return true // Treat out-of-bounds as walls
        }
        
        return mapData[tileY][tileX] == '1'
    }
    
    fun canMoveTo(x: Float, y: Float, width: Float, height: Float): Boolean {
        // Check all corners of the player's bounding box
        val halfWidth = width / 2f
        val halfHeight = height / 2f
        
        val left = x - halfWidth
        val right = x + halfWidth
        val top = y - halfHeight
        val bottom = y + halfHeight
        
        // Check four corners
        return !isWall(left, top) && 
               !isWall(right, top) && 
               !isWall(left, bottom) && 
               !isWall(right, bottom)
    }
    
    fun getWorldWidth(): Float = worldWidth
    fun getWorldHeight(): Float = worldHeight
    
    fun destroy() {
        floorTexture?.recycle()
        wallTexture?.recycle()
        floorTexture = null
        wallTexture = null
    }
}