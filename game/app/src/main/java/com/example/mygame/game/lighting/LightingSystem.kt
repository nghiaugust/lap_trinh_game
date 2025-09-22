package com.example.mygame.game.lighting

import android.graphics.*
import com.example.mygame.game.managers.MapManager
import kotlin.math.*

class LightingSystem(private val mapManager: MapManager) {
    private var shadowBitmap: Bitmap? = null
    private var shadowCanvas: Canvas? = null
    private val shadowPaint = Paint()
    
    // Light settings
    private val lightRadius = 600f // Tăng lên 600f để vùng sáng rộng hơn
    private val rayCount = 120 // Giảm từ 180 xuống 120 để tăng performance
    private val shadowOpacity = 0.6f // Tăng độ tối xung quanh để làm nổi bật vùng sáng
    
    // Performance optimization
    private var lightCache = mutableMapOf<String, List<PointF>>()
    private var lastPlayerTileX = -1
    private var lastPlayerTileY = -1
    private var frameCounter = 0
    private val updateFrequency = 2 // Update lighting mỗi 2 frame thay vì mỗi frame
    
    // Bitmap pooling để tránh garbage collection
    private var tempPath: Path? = null
    
    init {
        shadowPaint.apply {
            isAntiAlias = false // Tắt anti-aliasing để tăng performance
            style = Paint.Style.FILL
            isDither = false
            isFilterBitmap = false
        }
        tempPath = Path()
    }
    
    fun initialize(screenWidth: Int, screenHeight: Int) {
        // Create shadow bitmap với kích thước screen
        shadowBitmap = Bitmap.createBitmap(screenWidth, screenHeight, Bitmap.Config.ARGB_8888)
        shadowCanvas = Canvas(shadowBitmap!!)
    }
    
    fun updateLighting(playerX: Float, playerY: Float, cameraX: Float, cameraY: Float) {
        frameCounter++
        
        // Only update lighting every few frames to improve performance
        if (frameCounter % updateFrequency != 0) {
            return
        }
        
        val shadowBmp = shadowBitmap ?: return
        val canvas = shadowCanvas ?: return
        
        // Clear shadow bitmap
        canvas.drawColor(Color.TRANSPARENT, PorterDuff.Mode.CLEAR)
        
        // Check if we need to recalculate lighting (player moved to different tile)
        val playerTileX = (playerX / mapManager.getTileSize()).toInt()
        val playerTileY = (playerY / mapManager.getTileSize()).toInt()
        
        val cacheKey = "${playerTileX}_${playerTileY}"
        val lightPoints = if (lightCache.containsKey(cacheKey) && 
                             playerTileX == lastPlayerTileX && 
                             playerTileY == lastPlayerTileY) {
            lightCache[cacheKey]!!
        } else {
            calculateLightArea(playerX, playerY).also { points ->
                // Limit cache size để tránh memory leak
                if (lightCache.size > 50) {
                    lightCache.clear()
                }
                lightCache[cacheKey] = points
                lastPlayerTileX = playerTileX
                lastPlayerTileY = playerTileY
            }
        }
        
        // Draw dark overlay everywhere first
        shadowPaint.color = Color.argb((255 * currentShadowOpacity).toInt(), 0, 0, 0)
        canvas.drawRect(0f, 0f, shadowBmp.width.toFloat(), shadowBmp.height.toFloat(), shadowPaint)
        
        // Create light area by cutting out illuminated regions (với ray casting để tính tường)
        shadowPaint.xfermode = PorterDuffXfermode(PorterDuff.Mode.DST_OUT)
        shadowPaint.color = Color.WHITE // Vùng sáng hoàn toàn không có bộ lọc
        
        // Draw illuminated area using ray-casting (bị chặn bởi tường)
        val path = tempPath ?: Path()
        path.reset()
        
        if (lightPoints.isNotEmpty()) {
            val firstPoint = convertWorldToScreen(lightPoints[0], cameraX, cameraY, shadowBmp.width, shadowBmp.height)
            path.moveTo(firstPoint.x, firstPoint.y)
            
            for (i in 1 until lightPoints.size) {
                val screenPoint = convertWorldToScreen(lightPoints[i], cameraX, cameraY, shadowBmp.width, shadowBmp.height)
                path.lineTo(screenPoint.x, screenPoint.y)
            }
            path.close()
        }
        
        val playerScreenPos = convertWorldToScreen(PointF(playerX, playerY), cameraX, cameraY, shadowBmp.width, shadowBmp.height)
        
        // Debug log để kiểm tra vị trí (chỉ log mỗi 60 frame)
        if (frameCounter % 60 == 0) {
            android.util.Log.d("LightingSystem", "Player world: ($playerX, $playerY), Camera: ($cameraX, $cameraY), Screen: (${playerScreenPos.x}, ${playerScreenPos.y})")
        }
        
        // Draw the light area (không có gradient, chỉ vùng sáng hoàn toàn)
        canvas.drawPath(path, shadowPaint)
        
        // Reset paint
        shadowPaint.xfermode = null
    }
    
    private fun calculateLightArea(playerX: Float, playerY: Float): List<PointF> {
        val lightPoints = mutableListOf<PointF>()
        val angleStep = 360f / rayCount
        
        for (i in 0 until rayCount) {
            val angle = Math.toRadians((i * angleStep).toDouble())
            val rayEnd = castRay(playerX, playerY, angle, currentLightRadius)
            lightPoints.add(rayEnd)
        }
        
        // Thêm một vài điểm gần player để đảm bảo vùng sáng bao quanh player
        val playerRadius = 40f // Tăng từ 20f lên 40f để vùng gần player sáng hơn
        for (i in 0 until 12) { // Tăng từ 8 lên 12 điểm để mượt hơn
            val angle = Math.toRadians((i * 30).toDouble()) // Mỗi 30 độ thay vì 45 độ
            val nearPoint = PointF(
                playerX + cos(angle).toFloat() * playerRadius,
                playerY + sin(angle).toFloat() * playerRadius
            )
            lightPoints.add(nearPoint)
        }
        
        return lightPoints
    }
    
    private fun castRay(startX: Float, startY: Float, angle: Double, maxDistance: Float): PointF {
        val stepSize = 4f // Tăng từ 3f lên 4f vì vùng sáng to hơn
        val dx = cos(angle).toFloat() * stepSize
        val dy = sin(angle).toFloat() * stepSize
        
        var currentX = startX
        var currentY = startY
        var distance = 0f
        
        while (distance < maxDistance) {
            currentX += dx
            currentY += dy
            distance += stepSize
            
            // Check collision with walls - sử dụng tile-based check để tăng performance
            val tileX = (currentX / mapManager.getTileSize()).toInt()
            val tileY = (currentY / mapManager.getTileSize()).toInt()
            
            if (mapManager.isWallTile(tileX, tileY)) {
                // Backtrack slightly to get point just before wall
                currentX -= dx * 0.3f // Giảm backtrack để gần tường hơn
                currentY -= dy * 0.3f
                break
            }
        }
        
        return PointF(currentX, currentY)
    }
    
    private fun convertWorldToScreen(worldPoint: PointF, cameraX: Float, cameraY: Float, 
                                   screenWidth: Int, screenHeight: Int): PointF {
        // Sử dụng cùng công thức với MapManager để đồng bộ vị trí
        return PointF(
            worldPoint.x - cameraX,
            worldPoint.y - cameraY
        )
    }
    
    fun drawShadows(canvas: Canvas) {
        shadowBitmap?.let { bitmap ->
            canvas.drawBitmap(bitmap, 0f, 0f, null)
        }
    }
    
    fun cleanup() {
        shadowBitmap?.recycle()
        shadowBitmap = null
        shadowCanvas = null
        lightCache.clear()
        tempPath = null
    }
    
    // Các phương thức để điều chỉnh lighting settings
    private var currentLightRadius = lightRadius
    private var currentShadowOpacity = shadowOpacity
    
    fun setLightRadius(radius: Float) {
        currentLightRadius = radius
        lightCache.clear()
    }
    
    fun setShadowOpacity(opacity: Float) {
        currentShadowOpacity = opacity
        lightCache.clear()
    }
    
    fun getLightRadius(): Float = currentLightRadius
    fun getShadowOpacity(): Float = currentShadowOpacity
    
    // Phương thức để kiểm tra performance
    fun getPerformanceInfo(): String {
        return "Cache size: ${lightCache.size}, Frame: $frameCounter, Radius: $currentLightRadius"
    }
    
    // Phương thức debug để test lighting
    fun debugLighting(playerX: Float, playerY: Float): String {
        val playerTileX = (playerX / mapManager.getTileSize()).toInt()
        val playerTileY = (playerY / mapManager.getTileSize()).toInt()
        return "Player at tile ($playerTileX, $playerTileY), Light radius: $currentLightRadius, Shadow opacity: $currentShadowOpacity"
    }
    
    // Preset lighting configurations
    fun setLightingPreset(preset: String) {
        when (preset) {
            "bright" -> {
                setLightRadius(700f)  // Vùng sáng rộng
                setShadowOpacity(0.3f) // Shadow nhẹ
            }
            "normal" -> {
                setLightRadius(600f)  // Vùng sáng tiêu chuẩn
                setShadowOpacity(0.6f) // Shadow vừa phải
            }
            "dark" -> {
                setLightRadius(400f)  // Vùng sáng hẹp
                setShadowOpacity(0.8f) // Shadow đậm
            }
            "nofilter" -> { // Preset mới: vùng sáng hoàn toàn không có bộ lọc
                setLightRadius(800f)  // Vùng sáng rất rộng
                setShadowOpacity(0.7f) // Shadow đậm để tương phản
            }
            "explorer" -> { // Cho khám phá
                setLightRadius(750f)
                setShadowOpacity(0.4f)
            }
        }
    }
}