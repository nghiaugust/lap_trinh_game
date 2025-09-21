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
import com.example.mygame.game.managers.ProjectileManager
import com.example.mygame.game.managers.EnemyManager
import com.example.mygame.game.systems.PlayerHealthSystem
import com.example.mygame.game.ui.GameOverUI
import com.example.mygame.game.audio.SoundManager

class GameView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : SurfaceView(context, attrs, defStyleAttr), SurfaceHolder.Callback {

    // Game states
    enum class GameState {
        PLAYING,
        GAME_OVER
    }
    
    private var currentGameState = GameState.PLAYING

    private var gameThread: GameThread? = null
    private var player: Player? = null
    private var mapManager: MapManager? = null
    private var assetManager: GameAssetManager? = null
    private var miniMap: MiniMap? = null
    private var lightingSystem: LightingSystem? = null
    private var projectileManager: ProjectileManager? = null
    private var enemyManager: EnemyManager? = null
    private var playerHealthSystem: PlayerHealthSystem? = null
    private var gameOverUI: GameOverUI? = null
    private var soundManager: SoundManager? = null
    private var paint = Paint()
    
    private var screenWidth = 0
    private var screenHeight = 0
    
    // Touch controls
    private var joystickX = 0f
    private var joystickY = 0f
    private var joystickRadius = 180f  // Increased for better visibility and easier control
    private var joystickCenterX = 0f
    private var joystickCenterY = 0f
    private var isJoystickPressed = false
    
    // Attack button (bottom right)
    private var attackButtonX = 0f
    private var attackButtonY = 0f
    private val attackButtonRadius = 120f // Increased for better visibility
    private var isAttackButtonPressed = false
    
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
            
            // Initialize projectile manager
            projectileManager = ProjectileManager(context)
            
            // Initialize player health system
            playerHealthSystem = PlayerHealthSystem()
            
            // Initialize game over UI
            gameOverUI = GameOverUI()
            
            // Initialize sound manager
            Log.d("GameView", "Initializing SoundManager...")
            soundManager = SoundManager(context)
            Log.d("GameView", "SoundManager initialized")
            
            // Initialize player with asset manager
            assetManager?.let { assets ->
                player = Player(context, assets)
                
                // Initialize enemy manager (needs mapManager)
                mapManager?.let { map ->
                    enemyManager = EnemyManager(context, assets, map)
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    override fun surfaceCreated(holder: SurfaceHolder) {
        screenWidth = width
        screenHeight = height
        
        // Position joystick at bottom left with more margin from edges
        val marginFromEdge = 120f  // Increased margin for larger UI elements
        joystickCenterX = joystickRadius + marginFromEdge
        joystickCenterY = screenHeight - joystickRadius - marginFromEdge
        joystickX = joystickCenterX
        joystickY = joystickCenterY
        
        // Position attack button at bottom right
        attackButtonX = screenWidth - attackButtonRadius - marginFromEdge
        attackButtonY = screenHeight - attackButtonRadius - marginFromEdge
        
        // Initialize minimap
        mapManager?.let { map ->
            miniMap?.initialize(map, screenWidth, screenHeight)
            
            // Initialize lighting system
            lightingSystem = LightingSystem(map)
            lightingSystem?.initialize(screenWidth, screenHeight)
            
            // Initialize projectile manager
            assetManager?.let { assets ->
                projectileManager?.initialize(assets)
            }
            
            // Initialize player position at map start position
            player?.setPosition(map.playerStartX, map.playerStartY)
            
            // Spawn initial skeletons
            player?.let { p ->
                enemyManager?.spawnInitialSkeletons(p.getX(), p.getY(), 3)
            }
        }
        
        // Initialize game over UI
        gameOverUI?.initialize(screenWidth.toFloat(), screenHeight.toFloat())
        
        // Start background music
        Log.d("GameView", "Starting background music in surfaceCreated...")
        soundManager?.startBackgroundMusic()
        
        // Debug music status after a short delay
        soundManager?.debugMusicStatus()
        
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

        // Handle game over UI touch first
        if (currentGameState == GameState.GAME_OVER) {
            gameOverUI?.let { ui ->
                if (ui.handleTouch(x, y, event.action)) {
                    if (event.action == MotionEvent.ACTION_UP) {
                        // Continue button was clicked - restart game
                        restartGame()
                    }
                    return true
                }
            }
            return true // Block all other touches during game over
        }

        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                // Check if touch is on minimap first
                miniMap?.let { map ->
                    if (map.onTouch(x, y)) {
                        return true  // Minimap handled the touch
                    }
                }
                
                // Check if touch is on attack button
                val attackDistance = Math.sqrt(
                    Math.pow((x - attackButtonX).toDouble(), 2.0) +
                    Math.pow((y - attackButtonY).toDouble(), 2.0)
                ).toFloat()
                
                if (attackDistance <= attackButtonRadius) {
                    isAttackButtonPressed = true
                    performAttack(x, y)
                    Log.d("GameView", "Attack button pressed")
                    return true
                }
                
                // Check if touch is within joystick area
                val joystickDistance = Math.sqrt(
                    Math.pow((x - joystickCenterX).toDouble(), 2.0) +
                    Math.pow((y - joystickCenterY).toDouble(), 2.0)
                ).toFloat()
                
                if (joystickDistance <= joystickRadius) {
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
                
                if (isAttackButtonPressed) {
                    isAttackButtonPressed = false
                    Log.d("GameView", "Attack button released")
                }
            }
        }
        
        return true
    }
    
    private fun performAttack(touchX: Float, touchY: Float) {
        player?.let { p ->
            projectileManager?.let { pm ->
                // Bắn đạn theo hướng nhân vật đang di chuyển/quay mặt
                val success = pm.createFireballByDirection(p.getX(), p.getY(), p.getFacingDirection())
                
                if (success) {
                    soundManager?.playAttackSound() // Play attack sound when fireball is created
                    Log.d("GameView", "Fireball created from (${p.getX()}, ${p.getY()}) facing direction ${p.getFacingDirection()}")
                } else {
                    Log.d("GameView", "Cannot create fireball (cooldown or limit reached)")
                }
            }
        }
    }

    fun update() {
        // Only update game logic when playing
        if (currentGameState != GameState.PLAYING) {
            return
        }
        
        val updateStartTime = System.currentTimeMillis()
        
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
                    
                    // Update projectiles
                    projectileManager?.update(1f/60f, map) // Assuming 60 FPS
                    
                    // Update player health system
                    playerHealthSystem?.update(1f/60f)
                    
                    // Check if we have time left for enemy updates
                    val currentTime = System.currentTimeMillis()
                    if (currentTime - updateStartTime < 8) { // Max 8ms for update
                        // Update enemies
                        lightingSystem?.let { lighting ->
                            enemyManager?.update(1f/60f, p.getX(), p.getY(), lighting)
                        }
                        
                        // Check combat interactions
                        checkCombatInteractions()
                    }
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
                
                // Draw projectiles (before lighting so they get shadowed)
                projectileManager?.draw(canvas, map.cameraX, map.cameraY)
                
                // Draw enemies (before lighting so they get shadowed)
                enemyManager?.draw(canvas, map.cameraX, map.cameraY)
                
                // Update and draw lighting system
                lightingSystem?.updateLighting(p.getX(), p.getY(), map.cameraX, map.cameraY)
                lightingSystem?.drawShadows(canvas)
            }
        }
        
        // Draw UI elements (always on top, not affected by camera)
        drawJoystick(canvas)
        drawAttackButton(canvas)
        
        // Draw player health and armor UI
        playerHealthSystem?.draw(canvas)
        
        // Draw minimap (always on top)
        player?.let { p ->
            mapManager?.let { map ->
                miniMap?.draw(canvas, map, p.getX(), p.getY())
            }
        }
        
        // Draw game over UI if game is over
        if (currentGameState == GameState.GAME_OVER) {
            gameOverUI?.draw(canvas)
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
    
    private fun drawAttackButton(canvas: Canvas) {
        // Draw attack button base
        paint.color = if (isAttackButtonPressed) {
            Color.argb(150, 255, 100, 100) // Red when pressed
        } else {
            Color.argb(120, 255, 150, 50) // Orange when not pressed
        }
        paint.style = Paint.Style.FILL
        canvas.drawCircle(attackButtonX, attackButtonY, attackButtonRadius, paint)
        
        // Draw attack button border
        paint.color = Color.WHITE
        paint.style = Paint.Style.STROKE
        paint.strokeWidth = 4f
        canvas.drawCircle(attackButtonX, attackButtonY, attackButtonRadius, paint)
        
        // Draw fire symbol in the center
        paint.color = Color.WHITE
        paint.style = Paint.Style.FILL
        paint.textSize = 40f
        paint.textAlign = Paint.Align.CENTER
        
        // Draw flame emoji or text
        val fireText = "🔥" // You can use text or draw custom flame shape
        canvas.drawText(fireText, attackButtonX, attackButtonY + 15f, paint)
        
        // Alternative: draw simple flame shape if emoji doesn't work
        paint.color = Color.YELLOW
        paint.style = Paint.Style.FILL
        val flameSize = 20f
        canvas.drawCircle(attackButtonX, attackButtonY - 5f, flameSize * 0.6f, paint)
        
        paint.color = Color.RED
        val flamePath = Path()
        flamePath.moveTo(attackButtonX - flameSize * 0.3f, attackButtonY + 5f)
        flamePath.lineTo(attackButtonX, attackButtonY - flameSize * 0.8f)
        flamePath.lineTo(attackButtonX + flameSize * 0.3f, attackButtonY + 5f)
        flamePath.close()
        canvas.drawPath(flamePath, paint)
    }
    
    // Lifecycle methods
    fun onResume() {
        // Game thread will start automatically when surface is created
        soundManager?.onResume()
    }
    
    fun onPause() {
        // Stop game thread to save battery and avoid ANR
        if (isGameRunning) {
            stopGameThread()
        }
        soundManager?.onPause()
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
        projectileManager?.cleanup()
        projectileManager = null
        soundManager?.cleanup()
        soundManager = null
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
            var frameStartTime: Long
            
            while (running) {
                frameStartTime = System.currentTimeMillis()
                loops = 0
                
                // Update game logic with time limit
                while (System.currentTimeMillis() > nextGameTick && loops < maxFrameSkip) {
                    try {
                        val updateStart = System.currentTimeMillis()
                        gameView.update()
                        val updateTime = System.currentTimeMillis() - updateStart
                        
                        // If update takes too long, skip additional updates to prevent ANR
                        if (updateTime > 10) {
                            Log.w("GameThread", "Update took ${updateTime}ms, skipping additional updates")
                            break
                        }
                    } catch (e: Exception) {
                        Log.e("GameThread", "Error in update", e)
                    }
                    nextGameTick += targetTime
                    loops++
                }
                
                // Render frame with time limit
                var canvas: Canvas? = null
                try {
                    if (surfaceHolder.surface.isValid) {
                        val renderStart = System.currentTimeMillis()
                        canvas = surfaceHolder.lockCanvas()
                        canvas?.let { c ->
                            synchronized(surfaceHolder) {
                                gameView.renderGame(c)
                            }
                        }
                        val renderTime = System.currentTimeMillis() - renderStart
                        
                        // Log if render takes too long
                        if (renderTime > 12) {
                            Log.w("GameThread", "Render took ${renderTime}ms")
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
                
                // Sleep to maintain target FPS and prevent overheating
                val frameTime = System.currentTimeMillis() - frameStartTime
                val sleepTime = targetTime - frameTime
                if (sleepTime > 0) {
                    try {
                        Thread.sleep(sleepTime)
                    } catch (e: InterruptedException) {
                        Log.w("GameThread", "Sleep interrupted")
                    }
                } else if (frameTime > targetTime + 5) {
                    // Log if frame took significantly longer than target
                    Log.w("GameThread", "Frame took ${frameTime}ms (target: ${targetTime}ms)")
                }
            }
        }
    }
    
    private fun checkCombatInteractions() {
        player?.let { p ->
            projectileManager?.let { pm ->
                enemyManager?.let { em ->
                    playerHealthSystem?.let { phs ->
                        // Check fireball hits on enemies
                        val hitCount = em.checkFireballCollisions(pm.getActiveFireballs())
                        
                        // Check if enemies attack player
                        val damageToPlayer = em.checkSkeletonAttacks(p.getX(), p.getY())
                        
                        if (damageToPlayer > 0 && phs.canTakeDamage()) {
                            phs.takeDamage(damageToPlayer)
                            soundManager?.playHurtSound() // Play hurt sound when player takes damage
                            Log.d("GameView", "Player takes $damageToPlayer damage from skeletons!")
                        }
                        
                        if (hitCount > 0) {
                            soundManager?.playExplosionSound() // Play explosion sound when fireball hits
                            Log.d("GameView", "Player hit $hitCount enemies!")
                        }
                        
                        // Check if player died
                        if (phs.isDead() && currentGameState == GameState.PLAYING) {
                            Log.d("GameView", "Game Over - Player died!")
                            triggerGameOver()
                        }
                    }
                }
            }
        }
    }
    
    private fun triggerGameOver() {
        currentGameState = GameState.GAME_OVER
        Log.d("GameView", "Game state changed to GAME_OVER")
    }
    
    private fun restartGame() {
        Log.d("GameView", "Restarting game...")
        
        // Reset game state
        currentGameState = GameState.PLAYING
        
        // Reset player health
        playerHealthSystem?.reset()
        
        // Reset player position
        mapManager?.let { map ->
            player?.setPosition(map.playerStartX, map.playerStartY)
        }
        
        // Clear all enemies and projectiles
        enemyManager?.clearAllSkeletons()
        projectileManager?.clearAllProjectiles()
        
        // Spawn new enemies
        player?.let { p ->
            enemyManager?.spawnInitialSkeletons(p.getX(), p.getY(), 3)
        }
        
        // Reset UI
        gameOverUI?.reset()
        
        Log.d("GameView", "Game restarted successfully")
    }
    
    // Sound control methods
    fun toggleMusic() {
        soundManager?.let { sound ->
            sound.enableMusic(!sound.isMusicEnabled())
        }
    }
    
    fun setMusicVolume(volume: Float) {
        soundManager?.setMusicVolume(volume)
    }
    
    fun setSfxVolume(volume: Float) {
        soundManager?.setSfxVolume(volume)
    }
    
    fun enableMusic(enabled: Boolean) {
        soundManager?.enableMusic(enabled)
    }
    
    fun enableSfx(enabled: Boolean) {
        soundManager?.enableSfx(enabled)
    }
    
    fun isMusicEnabled(): Boolean = soundManager?.isMusicEnabled() ?: true
    fun isSfxEnabled(): Boolean = soundManager?.isSfxEnabled() ?: true
    fun getMusicVolume(): Float = soundManager?.getMusicVolume() ?: 0.7f
    fun getSfxVolume(): Float = soundManager?.getSfxVolume() ?: 0.8f
    fun isMusicPlaying(): Boolean = soundManager?.isMusicPlaying() ?: false
}
