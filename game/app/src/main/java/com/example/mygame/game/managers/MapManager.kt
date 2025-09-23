package com.example.mygame.game.managers

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Paint
import android.util.Log
import androidx.core.graphics.scale
import java.io.BufferedReader
import java.io.InputStreamReader

class MapManager(private val context: Context) {
    
    // Map data
    private var mapData: Array<CharArray> = emptyArray()
    private var mapWidth = 0
    private var mapHeight = 0
    
    // Tile settings - increased for better visibility
    private val tileSize = 128f  // Each tile is 128x128 pixels
    
    // Public getter for tile size
    fun getTileSize(): Float = tileSize
    
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
            floorTexture = floorBitmap.scale(
                tileSize.toInt(), 
                tileSize.toInt()
            )
            
            // Load wall texture  
            val wallBitmap = BitmapFactory.decodeStream(
                context.assets.open("textures/environment/walls/wall.png")
            )
            wallTexture = wallBitmap.scale(
                tileSize.toInt(), 
                tileSize.toInt()
            )
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
    
    private fun calculateWorldSize() {
        worldWidth = mapWidth * tileSize
        worldHeight = mapHeight * tileSize
        Log.d("MapManager", "World size calculated: ${worldWidth}x${worldHeight} (${mapWidth}x${mapHeight} tiles, tile size: $tileSize)")
    }
    
    private fun findPlayerStartPosition() {
        // Find entrance position - prioritize leftmost open spaces
        var bestX = -1
        var bestY = -1
        var minX = mapWidth
        
        // First pass: find the leftmost open space
        for (y in 0 until mapHeight) {
            for (x in 0 until mapWidth) {
                if (mapData[y][x] == '0') {
                    if (x < minX) {
                        minX = x
                        bestX = x
                        bestY = y
                    }
                }
            }
        }
        
        // If we found a leftmost position, use it
        if (bestX != -1 && bestY != -1) {
            playerStartX = (bestX + 0.5f) * tileSize
            playerStartY = (bestY + 0.5f) * tileSize
            Log.d("MapManager", "Player start position found at leftmost entrance: ($playerStartX, $playerStartY) at tile ($bestX, $bestY)")
            
            // Validate the spawn position with hero size (updated to realistic size)
            if (!canMoveTo(playerStartX, playerStartY, 64f, 96f)) {
                Log.w("MapManager", "Initial spawn position is invalid! Searching for alternative...")
                findAlternativeSpawnPosition()
            }
            return
        }
        
        // Fallback: find any open space in left quarter
        for (y in 0 until mapHeight) {
            for (x in 0 until mapWidth) {
                if (mapData[y][x] == '0' && x < mapWidth / 4) {
                    playerStartX = (x + 0.5f) * tileSize
                    playerStartY = (y + 0.5f) * tileSize
                    Log.d("MapManager", "Player start position found (left quarter): ($playerStartX, $playerStartY) at tile ($x, $y)")
                    
                    if (!canMoveTo(playerStartX, playerStartY, 64f, 96f)) {
                        Log.w("MapManager", "Left quarter spawn position is invalid! Searching for alternative...")
                        findAlternativeSpawnPosition()
                    }
                    return
                }
            }
        }
        
        // Fallback: find any open space
        for (y in 1 until mapHeight - 1) {
            for (x in 1 until mapWidth - 1) {
                if (mapData[y][x] == '0') {
                    playerStartX = (x + 0.5f) * tileSize
                    playerStartY = (y + 0.5f) * tileSize
                    Log.d("MapManager", "Player start position found (fallback): ($playerStartX, $playerStartY) at tile ($x, $y)")
                    
                    if (!canMoveTo(playerStartX, playerStartY, 64f, 96f)) {
                        Log.w("MapManager", "Fallback spawn position is invalid! Searching for alternative...")
                        findAlternativeSpawnPosition()
                    }
                    return
                }
            }
        }
        
        // Last resort: center of world
        playerStartX = worldWidth / 2f
        playerStartY = worldHeight / 2f
        Log.d("MapManager", "No entrance found, using center: ($playerStartX, $playerStartY)")
    }
    
    private fun findAlternativeSpawnPosition() {
        // Search for a valid spawn position in the center area of the map
        val centerX = mapWidth / 2
        val centerY = mapHeight / 2
        val searchRadius = Math.min(mapWidth, mapHeight) / 4
        
        for (radius in 1..searchRadius) {
            for (angle in 0 until 360 step 45) {
                val radians = Math.toRadians(angle.toDouble())
                val testX = centerX + (radius * Math.cos(radians)).toInt()
                val testY = centerY + (radius * Math.sin(radians)).toInt()
                
                if (testX >= 0 && testX < mapWidth && testY >= 0 && testY < mapHeight) {
                    if (mapData[testY][testX] == '0') {
                        val worldX = (testX + 0.5f) * tileSize
                        val worldY = (testY + 0.5f) * tileSize
                        
                        if (canMoveTo(worldX, worldY, 64f, 96f)) {
                            playerStartX = worldX
                            playerStartY = worldY
                            Log.d("MapManager", "Found valid alternative spawn: ($playerStartX, $playerStartY) at tile ($testX, $testY)")
                            return
                        }
                    }
                }
            }
        }
        
        Log.e("MapManager", "Could not find valid spawn position! Using center anyway.")
        playerStartX = worldWidth / 2f
        playerStartY = worldHeight / 2f
    }
    
    fun updateCamera(playerX: Float, playerY: Float, screenWidth: Int, screenHeight: Int) {
        // Center camera on heroes
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
        
        // Optimized culling with buffer area to prevent pop-in
        val bufferTiles = 1 // Add 1 tile buffer around visible area
        val startTileX = ((cameraX - bufferTiles * tileSize) / tileSize).toInt().coerceAtLeast(0)
        val startTileY = ((cameraY - bufferTiles * tileSize) / tileSize).toInt().coerceAtLeast(0)
        val endTileX = ((cameraX + screenWidth + bufferTiles * tileSize) / tileSize).toInt().coerceAtMost(mapWidth - 1)
        val endTileY = ((cameraY + screenHeight + bufferTiles * tileSize) / tileSize).toInt().coerceAtMost(mapHeight - 1)
        
        // Performance tracking
        val tilesDrawn = (endTileX - startTileX + 1) * (endTileY - startTileY + 1)
        
        // Draw visible tiles with optimized loop
        for (tileY in startTileY..endTileY) {
            for (tileX in startTileX..endTileX) {
                val drawX = (tileX * tileSize) - cameraX
                val drawY = (tileY * tileSize) - cameraY
                
                // Quick bounds check - skip if tile is completely outside screen
                if (drawX + tileSize < 0 || drawX > screenWidth || 
                    drawY + tileSize < 0 || drawY > screenHeight) {
                    continue
                }
                
                val texture = if (mapData[tileY][tileX] == '0') floorTexture else wallTexture
                texture?.let { bitmap ->
                    canvas.drawBitmap(bitmap, drawX, drawY, paint)
                }
            }
        }
        
        // Log performance occasionally for debugging
        frameDrawCount++
        if (frameDrawCount % 120 == 0) { // Every 2 seconds at 60fps
            Log.d("MapManager", "Drawing $tilesDrawn tiles (${startTileX}-${endTileX}, ${startTileY}-${endTileY})")
        }
    }
    
    // Performance tracking
    private var frameDrawCount = 0
    
    // Cache for tile lookups
    private var lastTileX = -1
    private var lastTileY = -1
    private var lastTileResult = false
    
    fun isWall(x: Float, y: Float): Boolean {
        val tileX = (x / tileSize).toInt()
        val tileY = (y / tileSize).toInt()
        
        // Use cache if same tile
        if (tileX == lastTileX && tileY == lastTileY) {
            return lastTileResult
        }
        
        return isWallTile(tileX, tileY)
    }
    
    fun isWallTile(tileX: Int, tileY: Int): Boolean {
        // Update cache
        lastTileX = tileX
        lastTileY = tileY
        
        // Check bounds
        if (tileX < 0 || tileX >= mapWidth || tileY < 0 || tileY >= mapHeight) {
            lastTileResult = true
            return true
        }
        
        // Check tile type
        lastTileResult = mapData[tileY][tileX] == '1'
        return lastTileResult
    }
    
    fun canMoveTo(x: Float, y: Float, width: Float, height: Float): Boolean {
        // Quick bounds check first
        if (x < 0 || y < 0 || x >= worldWidth || y >= worldHeight) {
            Log.d("MapManager", "Movement blocked by world bounds: pos($x, $y), world($worldWidth, $worldHeight)")
            return false
        }
        
        // Fine-tuned collision margin for optimal movement with smaller hero size
        val halfWidth = width / 2f
        val halfHeight = height / 2f
        val margin = 1f  // Very small margin with smaller hero bounds
        
        val left = x - halfWidth + margin
        val right = x + halfWidth - margin
        val top = y - halfHeight + margin
        val bottom = y + halfHeight - margin
        
        // Check bounds after applying margin
        if (left < 0 || right >= worldWidth || top < 0 || bottom >= worldHeight) {
            Log.d("MapManager", "Movement blocked by margin bounds: left=$left, right=$right, top=$top, bottom=$bottom, world($worldWidth, $worldHeight)")
            return false
        }
        
        // Check only 4 corners for simpler and more permissive collision detection
        val checkPoints = arrayOf(
            Pair(left, top),       // Top-left
            Pair(right, top),      // Top-right
            Pair(left, bottom),    // Bottom-left
            Pair(right, bottom)    // Bottom-right
        )
        
        for ((i, point) in checkPoints.withIndex()) {
            if (isWall(point.first, point.second)) {
                // Reduced logging to prevent spam
                return false
            }
        }
        
        return true
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