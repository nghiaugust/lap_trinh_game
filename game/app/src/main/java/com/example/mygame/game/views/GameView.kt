package com.example.mygame.game.views

import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.util.Log
import android.view.MotionEvent
import android.view.SurfaceHolder
import android.view.SurfaceView
import com.example.mygame.R
import com.example.mygame.game.entities.Player
import com.example.mygame.game.managers.MapManager
import com.example.mygame.game.assets.GameAssetManager
import com.example.mygame.game.ui.MiniMap
import com.example.mygame.game.lighting.LightingSystem

class GameView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : SurfaceView(context, attrs, defStyleAttr), SurfaceHolder.Callback {

    private var gameThread: GameThread? = null
    private var player: Player? = null
    private var mapManager: MapManager? = null
    private var assetManager: GameAssetManager? = null
    private var miniMap: MiniMap? = null
    private var lightingSystem: LightingSystem? = null
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
            
            // Initialize minimap
            miniMap = MiniMap(context)
            
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
        
        // Initialize minimap
        mapManager?.let { map ->
            miniMap?.initialize(map, screenWidth, screenHeight)
            
            // Initialize lighting system
            lightingSystem = LightingSystem(map)
            lightingSystem?.initialize(screenWidth, screenHeight)
            
            // Initialize player position at map start position
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
                // Check if touch is on minimap first
                miniMap?.let { map ->
                    if (map.onTouch(x, y)) {
                        return true  // Minimap handled the touch
                    }
                }
                
                // Check if touch is within joystick area
                val distance = Math.sqrt(
                    Math.pow((x - joystickCenterX).toDouble(), 2.0) +
                    Math.pow((y - joystickCenterY).toDouble(), 2.0)
                ).toFloat()
                
                if (distance <= joystickRadius) {
                    isJoystickPressed = true
                    joystickX = x
                    joystickY = y
                    Log.d("GameView", "Joystick pressed")
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
                    
                    // Calculate movement direction with lower sensitivity threshold
                    val deltaX = joystickX - joystickCenterX
                    val deltaY = joystickY - joystickCenterY
                    val movementDistance = Math.sqrt((deltaX * deltaX + deltaY * deltaY).toDouble()).toFloat()
                    
                    // Only send movement if joystick is moved enough (reduce from 10 to 5)
                    if (movementDistance > 5f) {
                        player?.setMovementDirection(deltaX, deltaY)
                    }
                }
            }
            
            MotionEvent.ACTION_UP -> {
                if (isJoystickPressed) {
                    isJoystickPressed = false
                    joystickX = joystickCenterX
                    joystickY = joystickCenterY
                    player?.stopMovement()
                    Log.d("GameView", "Joystick released")
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
                    
                    // Update minimap exploration
                    miniMap?.updateExploration(p.getX(), p.getY())
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
                
                // Update and draw lighting system
                lightingSystem?.updateLighting(p.getX(), p.getY(), map.cameraX, map.cameraY)
                lightingSystem?.drawShadows(canvas)
            }
        }
        
        // Draw joystick (always on top, not affected by camera)
        drawJoystick(canvas)
        
        // Draw minimap (always on top)
        player?.let { p ->
            mapManager?.let { map ->
                miniMap?.draw(canvas, map, p.getX(), p.getY())
            }
        }
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
        miniMap = null
        lightingSystem?.cleanup()
        lightingSystem = null
    }

    inner class GameThread(
        private val surfaceHolder: SurfaceHolder,
        private val gameView: GameView
    ) : Thread() {
        
        @Volatile
        private var running = false
        private val targetFPS = 60
        private val targetTime = (1000.0 / targetFPS).toLong()
        private val maxFrameSkip = 5

        fun setRunning(isRunning: Boolean) {
            synchronized(this) {
                running = isRunning
            }
        }

        override fun run() {
            var nextGameTick = System.currentTimeMillis()
            var loops: Int
            
            while (running) {
                loops = 0
                
                // Update game logic
                while (System.currentTimeMillis() > nextGameTick && loops < maxFrameSkip) {
                    try {
                        gameView.update()
                    } catch (e: Exception) {
                        Log.e("GameThread", "Error in update", e)
                    }
                    nextGameTick += targetTime
                    loops++
                }
                
                // Render frame
                var canvas: Canvas? = null
                try {
                    if (surfaceHolder.surface.isValid) {
                        canvas = surfaceHolder.lockCanvas()
                        canvas?.let { c ->
                            synchronized(surfaceHolder) {
                                gameView.renderGame(c)
                            }
                        }
                    }
                } catch (e: Exception) {
                    Log.e("GameThread", "Error in render", e)
                } finally {
                    canvas?.let { c ->
                        try {
                            surfaceHolder.unlockCanvasAndPost(c)
                        } catch (e: Exception) {
                            Log.e("GameThread", "Error unlocking canvas", e)
                        }
                    }
                }
                
                // Sleep to maintain target FPS
                val sleepTime = nextGameTick - System.currentTimeMillis()
                if (sleepTime > 0) {
                    try {
                        sleep(sleepTime)
                    } catch (e: InterruptedException) {
                        Thread.currentThread().interrupt()
                        break
                    }
                }
            }
        }
    }
}
