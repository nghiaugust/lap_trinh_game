package com.example.mygame.game.ui

import android.content.Context
import android.graphics.*
import android.util.Log
import com.example.mygame.game.managers.MapManager

class MiniMap(private val context: Context) {
    
    // Minimap settings
    private val minimapSize = 200f  // Size of minimap in pixels
    private val minimapMargin = 20f  // Margin from screen edge
    private var minimapX = 0f
    private var minimapY = 0f
    
    // Fullscreen map settings
    private var isFullscreen = false
    private var fullscreenMapSize = 0f
    private var fullscreenMapX = 0f
    private var fullscreenMapY = 0f
    private var screenWidth = 0
    private var screenHeight = 0
    
    // Fog of war - tracks explored areas
    private var exploredMap: Array<BooleanArray> = emptyArray()
    private var mapWidth = 0
    private var mapHeight = 0
    private var tileSize = 64f  // Will be set from MapManager
    
    // Vision settings
    private val visionRadius = 3  // How many tiles around player are visible
    
    // Colors
    private val unexploredColor = Color.argb(200, 0, 0, 0)  // Semi-transparent black
    private val exploredFloorColor = Color.argb(180, 200, 200, 200)  // Light gray
    private val exploredWallColor = Color.argb(180, 100, 100, 100)   // Dark gray
    private val playerColor = Color.argb(255, 255, 100, 100)  // Red dot for player
    private val borderColor = Color.WHITE
    
    // Paint objects
    private val paint = Paint().apply {
        isAntiAlias = true
    }
    
    fun initialize(mapManager: MapManager, screenWidth: Int, screenHeight: Int) {
        // Store screen dimensions
        this.screenWidth = screenWidth
        this.screenHeight = screenHeight
        
        // Position minimap at top-right corner
        minimapX = screenWidth - minimapSize - minimapMargin
        minimapY = minimapMargin
        
        // Calculate fullscreen map settings
        val mapAspectRatio = mapManager.getWorldWidth() / mapManager.getWorldHeight()
        val screenAspectRatio = screenWidth.toFloat() / screenHeight.toFloat()
        
        if (mapAspectRatio > screenAspectRatio) {
            // Map is wider than screen
            fullscreenMapSize = screenWidth * 0.8f
            fullscreenMapX = screenWidth * 0.1f
            fullscreenMapY = (screenHeight - fullscreenMapSize / mapAspectRatio) / 2f
        } else {
            // Map is taller than screen
            fullscreenMapSize = screenHeight * 0.8f
            fullscreenMapX = (screenWidth - fullscreenMapSize * mapAspectRatio) / 2f
            fullscreenMapY = screenHeight * 0.1f
        }
        
        // Get tile size from MapManager
        tileSize = 64f  // MapManager uses 64f
        
        // Initialize fog of war
        mapWidth = (mapManager.getWorldWidth() / tileSize).toInt()
        mapHeight = (mapManager.getWorldHeight() / tileSize).toInt()
        
        exploredMap = Array(mapHeight) { BooleanArray(mapWidth) { false } }
        
        Log.d("MiniMap", "Initialized minimap: ${mapWidth}x${mapHeight}, tileSize: $tileSize")
    }
    
    fun updateExploration(playerX: Float, playerY: Float) {
        // Convert player world position to tile coordinates
        val playerTileX = (playerX / tileSize).toInt()
        val playerTileY = (playerY / tileSize).toInt()
        
        // Mark tiles around player as explored
        for (dy in -visionRadius..visionRadius) {
            for (dx in -visionRadius..visionRadius) {
                val tileX = playerTileX + dx
                val tileY = playerTileY + dy
                
                // Check if tile is within bounds
                if (tileX >= 0 && tileX < mapWidth && tileY >= 0 && tileY < mapHeight) {
                    // Calculate distance from player
                    val distance = Math.sqrt((dx * dx + dy * dy).toDouble())
                    
                    // Mark as explored if within vision radius
                    if (distance <= visionRadius) {
                        exploredMap[tileY][tileX] = true
                    }
                }
            }
        }
    }
    
    fun draw(canvas: Canvas, mapManager: MapManager, playerX: Float, playerY: Float) {
        if (isFullscreen) {
            drawFullscreenMap(canvas, mapManager, playerX, playerY)
        } else {
            drawMiniMap(canvas, mapManager, playerX, playerY)
        }
    }
    
    private fun drawMiniMap(canvas: Canvas, mapManager: MapManager, playerX: Float, playerY: Float) {
        // Draw minimap background
        paint.color = Color.argb(100, 0, 0, 0)
        paint.style = Paint.Style.FILL
        canvas.drawRect(minimapX, minimapY, minimapX + minimapSize, minimapY + minimapSize, paint)
        
        // Draw minimap border
        paint.color = borderColor
        paint.style = Paint.Style.STROKE
        paint.strokeWidth = 2f
        canvas.drawRect(minimapX, minimapY, minimapX + minimapSize, minimapY + minimapSize, paint)
        
        // Calculate scale factor
        val scaleX = minimapSize / mapWidth
        val scaleY = minimapSize / mapHeight
        val scale = Math.min(scaleX, scaleY)
        
        drawMapContent(canvas, mapManager, playerX, playerY, minimapX, minimapY, scale)
    }
    
    private fun drawFullscreenMap(canvas: Canvas, mapManager: MapManager, playerX: Float, playerY: Float) {
        // Draw semi-transparent background overlay
        paint.color = Color.argb(180, 0, 0, 0)
        paint.style = Paint.Style.FILL
        canvas.drawRect(0f, 0f, screenWidth.toFloat(), screenHeight.toFloat(), paint)
        
        // Calculate map dimensions for fullscreen
        val mapAspectRatio = mapWidth.toFloat() / mapHeight.toFloat()
        val availableWidth = fullscreenMapSize
        val availableHeight = fullscreenMapSize
        
        val mapDisplayWidth: Float
        val mapDisplayHeight: Float
        
        if (mapAspectRatio > 1f) {
            // Map is wider than tall
            mapDisplayWidth = availableWidth
            mapDisplayHeight = availableWidth / mapAspectRatio
        } else {
            // Map is taller than wide
            mapDisplayHeight = availableHeight
            mapDisplayWidth = availableHeight * mapAspectRatio
        }
        
        val mapX = fullscreenMapX + (availableWidth - mapDisplayWidth) / 2f
        val mapY = fullscreenMapY + (availableHeight - mapDisplayHeight) / 2f
        
        // Draw map background
        paint.color = Color.argb(200, 20, 20, 20)
        paint.style = Paint.Style.FILL
        canvas.drawRect(mapX, mapY, mapX + mapDisplayWidth, mapY + mapDisplayHeight, paint)
        
        // Draw map border
        paint.color = Color.WHITE
        paint.style = Paint.Style.STROKE
        paint.strokeWidth = 3f
        canvas.drawRect(mapX, mapY, mapX + mapDisplayWidth, mapY + mapDisplayHeight, paint)
        
        // Calculate scale factor for fullscreen
        val scale = Math.min(mapDisplayWidth / mapWidth, mapDisplayHeight / mapHeight)
        
        drawMapContent(canvas, mapManager, playerX, playerY, mapX, mapY, scale)
        
        // Draw close instruction
        paint.color = Color.WHITE
        paint.style = Paint.Style.FILL
        paint.textSize = 24f
        paint.textAlign = Paint.Align.CENTER
        canvas.drawText("Tap anywhere to close", screenWidth / 2f, mapY + mapDisplayHeight + 50f, paint)
    }
    
    private fun drawMapContent(canvas: Canvas, mapManager: MapManager, playerX: Float, playerY: Float, 
                              startX: Float, startY: Float, scale: Float) {
        
        // Draw explored tiles
        paint.style = Paint.Style.FILL
        for (y in 0 until mapHeight) {
            for (x in 0 until mapWidth) {
                if (exploredMap[y][x]) {
                    // Get tile data from map manager
                    val worldX = x * tileSize + tileSize/2f  // Center of tile
                    val worldY = y * tileSize + tileSize/2f
                    val isWall = mapManager.isWall(worldX, worldY)
                    
                    // Set color based on tile type
                    paint.color = if (isWall) exploredWallColor else exploredFloorColor
                    
                    // Calculate position based on passed coordinates
                    val drawX = startX + (x * scale)
                    val drawY = startY + (y * scale)
                    
                    // Draw tile
                    canvas.drawRect(drawX, drawY, drawX + scale, drawY + scale, paint)
                }
            }
        }
        
        // Draw fog of war (unexplored areas)
        paint.color = unexploredColor
        for (y in 0 until mapHeight) {
            for (x in 0 until mapWidth) {
                if (!exploredMap[y][x]) {
                    val drawX = startX + (x * scale)
                    val drawY = startY + (y * scale)
                    canvas.drawRect(drawX, drawY, drawX + scale, drawY + scale, paint)
                }
            }
        }
        
        // Draw player position
        val playerTileX = (playerX / tileSize)
        val playerTileY = (playerY / tileSize)
        val playerDrawX = startX + (playerTileX * scale)
        val playerDrawY = startY + (playerTileY * scale)
        
        paint.color = playerColor
        paint.style = Paint.Style.FILL
        canvas.drawCircle(playerDrawX, playerDrawY, scale / 2f, paint)
        
        // Draw player border
        paint.color = Color.WHITE
        paint.style = Paint.Style.STROKE
        paint.strokeWidth = 1f
        canvas.drawCircle(playerDrawX, playerDrawY, scale / 2f, paint)
    }
    
    fun onTouch(x: Float, y: Float): Boolean {
        if (isFullscreen) {
            // If fullscreen, any touch closes it
            isFullscreen = false
            return true
        } else {
            // Check if touch is within minimap bounds
            if (x >= minimapX && x <= minimapX + minimapSize &&
                y >= minimapY && y <= minimapY + minimapSize) {
                isFullscreen = true
                return true
            }
        }
        return false
    }
    
    fun isPointInMinimap(x: Float, y: Float): Boolean {
        return x >= minimapX && x <= minimapX + minimapSize &&
               y >= minimapY && y <= minimapY + minimapSize
    }
}