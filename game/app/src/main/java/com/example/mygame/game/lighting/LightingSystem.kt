package com.example.mygame.game.lighting

import android.graphics.*
import com.example.mygame.game.managers.MapManager
import kotlin.math.*

class LightingSystem(private val mapManager: MapManager) {
    private var shadowBitmap: Bitmap? = null
    private var shadowCanvas: Canvas? = null
    private val shadowPaint = Paint()
    
    // Light settings - Optimized for better performance
    private val lightRadius = 500f // Giảm từ 600f xuống 500f
    private val rayCount = 64 // Giảm từ 120 xuống 64 (power of 2 for better performance)
    private val shadowOpacity = 0.55f // Giảm nhẹ để compensate cho radius nhỏ hơn
    
    // Performance optimization - Tăng update frequency
    private var lightCache = mutableMapOf<String, List<PointF>>()
    private var lastPlayerTileX = -1
    private var lastPlayerTileY = -1
    private var frameCounter = 0
    private val updateFrequency = 3 // Tăng từ 2 lên 3 - update lighting mỗi 3 frame
    
    // Adaptive quality based on performance
    private var currentRayCount = rayCount
    private var performanceFrames = 0
    private var slowFrameCount = 0
    private val performanceCheckInterval = 60 // Check performance every 60 frames
    
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
        // Create shadow bitmap với kích thước optimized (có thể nhỏ hơn screen)
        val bitmapScale = 0.75f // Use 75% of screen resolution for lighting bitmap
        val lightingWidth = (screenWidth * bitmapScale).toInt()
        val lightingHeight = (screenHeight * bitmapScale).toInt()
        
        // Use RGB_565 instead of ARGB_8888 for better performance (less memory)
        shadowBitmap = Bitmap.createBitmap(lightingWidth, lightingHeight, Bitmap.Config.ARGB_8888)
        shadowCanvas = Canvas(shadowBitmap!!)
        
        // Store scale for later use
        bitmapScaleX = screenWidth.toFloat() / lightingWidth
        bitmapScaleY = screenHeight.toFloat() / lightingHeight
        
        android.util.Log.d("LightingSystem", "Lighting bitmap: ${lightingWidth}x${lightingHeight} (scale: $bitmapScale)")
    }
    
    // Bitmap scaling factors
    private var bitmapScaleX = 1f
    private var bitmapScaleY = 1f
    
    fun updateLighting(playerX: Float, playerY: Float, cameraX: Float, cameraY: Float) {
        frameCounter++
        performanceFrames++
        
        // Adaptive performance monitoring
        val frameStartTime = System.currentTimeMillis()
        
        // Only update lighting every few frames to improve performance
        if (frameCounter % updateFrequency != 0) {
            return
        }
        
        val shadowBmp = shadowBitmap ?: return
        val canvas = shadowCanvas ?: return
        
        // Clear shadow bitmap
        canvas.drawColor(Color.TRANSPARENT, PorterDuff.Mode.CLEAR)
        
        // Check if we need to recalculate lighting (heroes moved to different tile)
        val playerTileX = (playerX / mapManager.getTileSize()).toInt()
        val playerTileY = (playerY / mapManager.getTileSize()).toInt()
        
        val cacheKey = "${playerTileX}_${playerTileY}_${currentRayCount}" // Include rayCount in cache key
        val lightPoints = if (lightCache.containsKey(cacheKey) && 
                             playerTileX == lastPlayerTileX && 
                             playerTileY == lastPlayerTileY) {
            lightCache[cacheKey]!!
        } else {
            calculateLightArea(playerX, playerY).also { points ->
                // Limit cache size để tránh memory leak
                if (lightCache.size > 30) { // Giảm từ 50 xuống 30
                    // Remove oldest entries
                    val toRemove = lightCache.keys.take(10)
                    toRemove.forEach { lightCache.remove(it) }
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
            
            // Optimize path creation - skip every other point for very close points
            var lastScreenPoint = firstPoint
            for (i in 1 until lightPoints.size) {
                val screenPoint = convertWorldToScreen(lightPoints[i], cameraX, cameraY, shadowBmp.width, shadowBmp.height)
                
                // Skip point if it's very close to the last one (< 2 pixels)
                val distance = kotlin.math.sqrt(
                    (screenPoint.x - lastScreenPoint.x) * (screenPoint.x - lastScreenPoint.x) +
                    (screenPoint.y - lastScreenPoint.y) * (screenPoint.y - lastScreenPoint.y)
                )
                
                if (distance >= 2f || i == lightPoints.size - 1) {
                    path.lineTo(screenPoint.x, screenPoint.y)
                    lastScreenPoint = screenPoint
                }
            }
            path.close()
        }
        
        val playerScreenPos = convertWorldToScreen(PointF(playerX, playerY), cameraX, cameraY, shadowBmp.width, shadowBmp.height)
        
        // Draw the light area (không có gradient, chỉ vùng sáng hoàn toàn)
        canvas.drawPath(path, shadowPaint)
        
        // Reset paint
        shadowPaint.xfermode = null
        
        // Performance monitoring and adaptive quality
        val frameTime = System.currentTimeMillis() - frameStartTime
        if (frameTime > 8) { // Frame took longer than 8ms
            slowFrameCount++
        }
        
        // Adjust quality based on performance every 60 frames
        if (performanceFrames >= performanceCheckInterval) {
            adjustLightingQuality()
            performanceFrames = 0
            slowFrameCount = 0
        }
        
        // Debug log (reduced frequency)
        if (frameCounter % 180 == 0) { // Every 3 seconds at 60fps
            android.util.Log.d("LightingSystem", "Rays: $currentRayCount, Update freq: $updateFrequency, Frame time: ${frameTime}ms")
        }
    }
    
    private fun adjustLightingQuality() {
        val slowFrameRatio = slowFrameCount.toFloat() / performanceCheckInterval
        
        when {
            slowFrameRatio > 0.3f && currentRayCount > 32 -> {
                // Too many slow frames, reduce quality
                currentRayCount = (currentRayCount * 0.8f).toInt().coerceAtLeast(32)
                lightCache.clear() // Clear cache since ray count changed
                android.util.Log.d("LightingSystem", "Reducing lighting quality to $currentRayCount rays (slow frames: $slowFrameCount)")
            }
            slowFrameRatio < 0.1f && currentRayCount < rayCount -> {
                // Performance is good, can increase quality
                currentRayCount = (currentRayCount * 1.2f).toInt().coerceAtMost(rayCount)
                lightCache.clear() // Clear cache since ray count changed
                android.util.Log.d("LightingSystem", "Increasing lighting quality to $currentRayCount rays")
            }
        }
    }
    
    private fun calculateLightArea(playerX: Float, playerY: Float): List<PointF> {
        val lightPoints = mutableListOf<PointF>()
        val angleStep = 360f / currentRayCount // Use adaptive ray count
        
        for (i in 0 until currentRayCount) {
            val angle = Math.toRadians((i * angleStep).toDouble())
            val rayEnd = castRay(playerX, playerY, angle, currentLightRadius)
            lightPoints.add(rayEnd)
        }
        
        // Thêm một vài điểm gần heroes để đảm bảo vùng sáng bao quanh heroes
        val playerRadius = 50f // Tăng từ 40f lên 50f để vùng gần heroes sáng hơn
        val nearPointCount = 8 // Giảm từ 12 xuống 8 để tăng performance
        for (i in 0 until nearPointCount) {
            val angle = Math.toRadians((i * 45).toDouble()) // Mỗi 45 độ
            val nearPoint = PointF(
                playerX + cos(angle).toFloat() * playerRadius,
                playerY + sin(angle).toFloat() * playerRadius
            )
            lightPoints.add(nearPoint)
        }
        
        return lightPoints
    }
    
    private fun castRay(startX: Float, startY: Float, angle: Double, maxDistance: Float): PointF {
        val stepSize = 6f // Tăng từ 4f lên 6f để giảm số lần check collision
        val dx = cos(angle).toFloat() * stepSize
        val dy = sin(angle).toFloat() * stepSize
        
        var currentX = startX
        var currentY = startY
        var distance = 0f
        
        // Pre-calculate tile size for performance
        val tileSize = mapManager.getTileSize()
        
        while (distance < maxDistance) {
            currentX += dx
            currentY += dy
            distance += stepSize
            
            // Optimized collision check - calculate tile coordinates once
            val tileX = (currentX / tileSize).toInt()
            val tileY = (currentY / tileSize).toInt()
            
            if (mapManager.isWallTile(tileX, tileY)) {
                // Optimized backtrack - use single step back
                currentX -= dx * 0.5f // Tăng từ 0.3f lên 0.5f để đơn giản hóa calculation
                currentY -= dy * 0.5f
                break
            }
        }
        
        return PointF(currentX, currentY)
    }
    
    private fun convertWorldToScreen(worldPoint: PointF, cameraX: Float, cameraY: Float, 
                                   screenWidth: Int, screenHeight: Int): PointF {
        // Convert to screen coordinates and scale for bitmap resolution
        return PointF(
            (worldPoint.x - cameraX) / bitmapScaleX,
            (worldPoint.y - cameraY) / bitmapScaleY
        )
    }
    
    fun drawShadows(canvas: Canvas) {
        shadowBitmap?.let { bitmap ->
            // Draw scaled bitmap back to full screen size
            val destRect = Rect(0, 0, canvas.width, canvas.height)
            val srcRect = Rect(0, 0, bitmap.width, bitmap.height)
            canvas.drawBitmap(bitmap, srcRect, destRect, null)
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