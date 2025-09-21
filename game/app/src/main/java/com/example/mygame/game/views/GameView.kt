package com.example.mygame.game.views

import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.SurfaceHolder
import android.view.SurfaceView
import com.example.mygame.R
import com.example.mygame.game.entities.Player
import com.example.mygame.game.managers.MapManager
import com.example.mygame.game.assets.GameAssetManager

class GameView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : SurfaceView(context, attrs, defStyleAttr), SurfaceHolder.Callback {

    private var gameThread: GameThread? = null
    private var player: Player? = null
    private var mapManager: MapManager? = null
    private var assetManager: GameAssetManager? = null
    private var paint = Paint()
    
    private var screenWidth = 0
    private var screenHeight = 0
    
    // Touch controls
    private var joystickX = 0f
    private var joystickY = 0f
    private var joystickRadius = 120f  // Tăng từ 100f lên 120f để dễ điều khiển hơn
    private var joystickCenterX = 0f
    private var joystickCenterY = 0f
    private var isJoystickPressed = false
    
    // Performance optimization
    @Volatile
    private var isGameRunning = false

    init {
        holder.addCallback(this)
        paint.isAntiAlias = true
        loadGameResources()
    }

    private fun loadGameResources() {
        try {
            // Initialize asset manager
            assetManager = GameAssetManager(context)
            
            // Initialize map manager
            mapManager = MapManager(context)
            
            // Initialize player with asset manager
            assetManager?.let { assets ->
                player = Player(context, assets)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    override fun surfaceCreated(holder: SurfaceHolder) {
        screenWidth = width
        screenHeight = height
        
        // Position joystick at bottom left with more margin from edges
        val marginFromEdge = 80f  // Tăng khoảng cách từ viền lên 80dp
        joystickCenterX = joystickRadius + marginFromEdge
        joystickCenterY = screenHeight - joystickRadius - marginFromEdge
        joystickX = joystickCenterX
        joystickY = joystickCenterY
        
        // Initialize player position at map start position
        mapManager?.let { map ->
            player?.setPosition(map.playerStartX, map.playerStartY)
        }
        
        startGameThread()
    }

    override fun surfaceChanged(holder: SurfaceHolder, format: Int, width: Int, height: Int) {
        // Handle surface changes if needed
    }

    override fun surfaceDestroyed(holder: SurfaceHolder) {
        stopGameThread()
    }

    private fun startGameThread() {
        if (!isGameRunning) {
            isGameRunning = true
            gameThread = GameThread(holder, this)
            gameThread?.setRunning(true)
            gameThread?.start()
        }
    }

    private fun stopGameThread() {
        isGameRunning = false
        gameThread?.let { thread ->
            thread.setRunning(false)
            var retry = true
            var retryCount = 0
            val maxRetries = 5
            
            while (retry && retryCount < maxRetries) {
                try {
                    thread.join(100) // Timeout 100ms
                    retry = false
                } catch (e: InterruptedException) {
                    retryCount++
                    if (retryCount >= maxRetries) {
                        // Force interrupt nếu không thể join
                        thread.interrupt()
                        retry = false
                    }
                }
            }
        }
        gameThread = null
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        val x = event.x
        val y = event.y

        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                // Check if touch is within joystick area
                val distance = Math.sqrt(
                    Math.pow((x - joystickCenterX).toDouble(), 2.0) +
                    Math.pow((y - joystickCenterY).toDouble(), 2.0)
                ).toFloat()
                
                if (distance <= joystickRadius) {
                    isJoystickPressed = true
                    joystickX = x
                    joystickY = y
                }
            }
            
            MotionEvent.ACTION_MOVE -> {
                if (isJoystickPressed) {
                    val distance = Math.sqrt(
                        Math.pow((x - joystickCenterX).toDouble(), 2.0) +
                        Math.pow((y - joystickCenterY).toDouble(), 2.0)
                    ).toFloat()
                    
                    if (distance <= joystickRadius) {
                        joystickX = x
                        joystickY = y
                    } else {
                        // Limit joystick to radius
                        val angle = Math.atan2((y - joystickCenterY).toDouble(), (x - joystickCenterX).toDouble())
                        joystickX = joystickCenterX + (joystickRadius * Math.cos(angle)).toFloat()
                        joystickY = joystickCenterY + (joystickRadius * Math.sin(angle)).toFloat()
                    }
                    
                    // Calculate movement direction
                    val deltaX = joystickX - joystickCenterX
                    val deltaY = joystickY - joystickCenterY
                    player?.setMovementDirection(deltaX, deltaY)
                }
            }
            
            MotionEvent.ACTION_UP -> {
                if (isJoystickPressed) {
                    isJoystickPressed = false
                    joystickX = joystickCenterX
                    joystickY = joystickCenterY
                    player?.stopMovement()
                }
            }
        }
        
        return true
    }

    fun update() {
        // Chỉ update khi có player và mapManager
        player?.let { p ->
            mapManager?.let { map ->
                if (screenWidth > 0 && screenHeight > 0) {
                    // Update player with collision detection
                    p.update(map.getWorldWidth(), map.getWorldHeight(), map)
                    
                    // Update camera to follow player
                    map.updateCamera(p.getX(), p.getY(), screenWidth, screenHeight)
                }
            }
        }
    }

    fun renderGame(canvas: Canvas) {
        // Clear canvas
        canvas.drawColor(Color.BLACK)
        
        // Draw map with camera offset
        mapManager?.draw(canvas, paint, screenWidth, screenHeight)
        
        // Draw player with camera offset
        player?.let { p ->
            mapManager?.let { map ->
                p.draw(canvas, paint, map.cameraX, map.cameraY)
            }
        }
        
        // Draw joystick (always on top, not affected by camera)
        drawJoystick(canvas)
    }

    private fun drawJoystick(canvas: Canvas) {
        // Draw joystick base
        paint.color = Color.argb(100, 255, 255, 255)
        paint.style = Paint.Style.FILL
        canvas.drawCircle(joystickCenterX, joystickCenterY, joystickRadius, paint)
        
        // Draw joystick border
        paint.color = Color.WHITE
        paint.style = Paint.Style.STROKE
        paint.strokeWidth = 5f
        canvas.drawCircle(joystickCenterX, joystickCenterY, joystickRadius, paint)
        
        // Draw joystick knob
        paint.color = Color.argb(180, 255, 255, 255)
        paint.style = Paint.Style.FILL
        canvas.drawCircle(joystickX, joystickY, 50f, paint)  // Tăng từ 40f lên 50f
        
        // Draw joystick knob border
        paint.color = Color.WHITE
        paint.style = Paint.Style.STROKE
        paint.strokeWidth = 3f
        canvas.drawCircle(joystickX, joystickY, 50f, paint)  // Tăng từ 40f lên 50f
    }
    
    // Lifecycle methods
    fun onResume() {
        // Game thread will start automatically when surface is created
    }
    
    fun onPause() {
        // Stop game thread to save battery and avoid ANR
        if (isGameRunning) {
            stopGameThread()
        }
    }
    
    fun onDestroy() {
        // Clean up resources
        stopGameThread()
        mapManager?.destroy()
        mapManager = null
        assetManager?.dispose()
        assetManager = null
    }

    inner class GameThread(
        private val surfaceHolder: SurfaceHolder,
        private val gameView: GameView
    ) : Thread() {
        
        @Volatile
        private var running = false
        private val targetFPS = 60
        private val targetTime = (1000.0 / targetFPS).toLong()

        fun setRunning(isRunning: Boolean) {
            synchronized(this) {
                running = isRunning
            }
        }

        override fun run() {
            var startTime: Long
            var timeMillis: Long
            var waitTime: Long
            var frameCount = 0
            var lastFPSTime = System.currentTimeMillis()
            
            while (running) {
                startTime = System.currentTimeMillis()
                var canvas: Canvas? = null
                
                try {
                    // Chỉ vẽ khi surface holder có sẵn
                    if (surfaceHolder.surface.isValid) {
                        canvas = surfaceHolder.lockCanvas()
                        canvas?.let { c ->
                            // Update game logic
                            gameView.update()
                            // Render game
                            gameView.renderGame(c)
                        }
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                } finally {
                    canvas?.let { c ->
                        try {
                            surfaceHolder.unlockCanvasAndPost(c)
                        } catch (e: Exception) {
                            e.printStackTrace()
                        }
                    }
                }
                
                // Tính toán FPS và sleep time
                timeMillis = System.currentTimeMillis() - startTime
                waitTime = targetTime - timeMillis
                
                // Sleep để duy trì FPS ổn định
                if (waitTime > 0) {
                    try {
                        sleep(waitTime)
                    } catch (e: InterruptedException) {
                        Thread.currentThread().interrupt()
                        return
                    }
                } else {
                    // Nếu frame quá chậm, yield để tránh blocking
                    yield()
                }
                
                // Debug FPS (optional)
                frameCount++
                if (System.currentTimeMillis() - lastFPSTime >= 1000) {
                    // Log.d("GameThread", "FPS: $frameCount")
                    frameCount = 0
                    lastFPSTime = System.currentTimeMillis()
                }
            }
        }
    }
}
