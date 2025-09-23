package com.example.mygame.game.views

import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.util.Log
import android.view.MotionEvent
import android.view.SurfaceHolder
import android.view.SurfaceView
import com.example.mygame.R
import com.example.mygame.game.entities.heroes.SamuraiArcher
import com.example.mygame.game.managers.MapManager
import com.example.mygame.game.assets.GameAssetManager
import com.example.mygame.game.ui.MiniMap
import com.example.mygame.game.managers.ProjectileManager
import com.example.mygame.game.managers.EnemyManager
import com.example.mygame.game.managers.HeroManager
import com.example.mygame.game.systems.PlayerHealthSystem
import com.example.mygame.game.ui.GameOverUI
import com.example.mygame.game.ui.SettingsUI
import com.example.mygame.game.ui.HeroHealthUI
import com.example.mygame.game.audio.SoundManager

class GameView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : SurfaceView(context, attrs, defStyleAttr), SurfaceHolder.Callback {

    // Game states
    enum class GameState {
        PLAYING,
        PAUSED,
        GAME_OVER
    }
    
    private var currentGameState = GameState.PLAYING

    private var gameThread: GameThread? = null
    private var heroManager: HeroManager? = null
    private var mapManager: MapManager? = null
    private var assetManager: GameAssetManager? = null
    private var miniMap: MiniMap? = null
    private var projectileManager: ProjectileManager? = null
    private var enemyManager: EnemyManager? = null
    private var playerHealthSystem: PlayerHealthSystem? = null
    private var heroHealthUI: HeroHealthUI? = null
    private var gameOverUI: GameOverUI? = null
    private var settingsUI: SettingsUI? = null
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
    
    // Shot button (bottom right, above attack button)
    private var shotButtonX = 0f
    private var shotButtonY = 0f
    private val shotButtonRadius = 100f
    private var isShotButtonPressed = false
    
    // Settings button (next to health bar)
    private var settingsButtonX = 0f
    private var settingsButtonY = 0f
    private val settingsButtonSize = 60f
    private var isSettingsButtonPressed = false
    
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
            
            // Initialize hero manager
            heroManager = HeroManager(context)
            
            // Initialize heroes health system
            playerHealthSystem = PlayerHealthSystem()
            
            // Initialize hero health UI
            heroHealthUI = HeroHealthUI()
            
            // Initialize game over UI
            gameOverUI = GameOverUI()
            
            // Initialize settings UI
            settingsUI = SettingsUI()
            
            // Initialize sound manager
            Log.d("GameView", "Initializing SoundManager...")
            soundManager = SoundManager(context)
            Log.d("GameView", "SoundManager initialized")
            
            // Initialize managers with asset manager
            assetManager?.let { assets ->
                
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
        
        // Position shot button above attack button
        shotButtonX = screenWidth - shotButtonRadius - marginFromEdge - 30f // Slightly offset
        shotButtonY = screenHeight - attackButtonRadius * 2 - shotButtonRadius - marginFromEdge - 20f
        
        // Position settings button away from health bars
        val healthUIHeight = heroHealthUI?.getTotalHeight() ?: 200f // Tăng từ 100f lên 200f
        settingsButtonX = 450f  // Tăng từ 250f lên 450f để tránh thanh máu lớn hơn
        settingsButtonY = healthUIHeight + 20f   // Tăng margin từ 10f lên 20f
        
        // Initialize minimap
        mapManager?.let { map ->
            miniMap?.initialize(map, screenWidth, screenHeight)
            
            // Initialize projectile manager
            assetManager?.let { assets ->
                projectileManager?.initialize(assets)
            }
            
            // Initialize hero manager with starting position
            assetManager?.let { assets ->
                heroManager?.initialize(assets, map.playerStartX, map.playerStartY)
            }
            
            // Spawn initial skeletons
            heroManager?.let { hm ->
                enemyManager?.spawnInitialSkeletons(hm.getCameraTargetX(), hm.getCameraTargetY(), 3)
            }
        }
        
        // Initialize game over UI
        gameOverUI?.initialize(screenWidth.toFloat(), screenHeight.toFloat())
        
        // Initialize settings UI
        settingsUI?.initialize(
            screenWidth.toFloat(), 
            screenHeight.toFloat(), 
            soundManager?.isMusicEnabled() ?: true,
            soundManager?.isSfxEnabled() ?: true,
            assetManager
        )
        
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

        // Handle settings UI touch first (when paused)
        if (currentGameState == GameState.PAUSED) {
            settingsUI?.let { ui ->
                val buttonPressed = ui.handleTouch(x, y, event.action == MotionEvent.ACTION_DOWN)
                if (event.action == MotionEvent.ACTION_UP && buttonPressed != null) {
                    handleSettingsButtonClick(buttonPressed)
                }
            }
            return true // Block all other touches during pause
        }

        // Handle game over UI touch
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
                // Check if touch is on settings button first
                val settingsDistance = Math.sqrt(
                    Math.pow((x - settingsButtonX).toDouble(), 2.0) +
                    Math.pow((y - settingsButtonY).toDouble(), 2.0)
                ).toFloat()
                
                if (settingsDistance <= settingsButtonSize) {
                    isSettingsButtonPressed = true
                    Log.d("GameView", "Settings button pressed")
                    return true
                }
                
                // Check if touch is on minimap
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
                
                // Check if touch is on shot button
                val shotDistance = Math.sqrt(
                    Math.pow((x - shotButtonX).toDouble(), 2.0) +
                    Math.pow((y - shotButtonY).toDouble(), 2.0)
                ).toFloat()
                
                if (shotDistance <= shotButtonRadius) {
                    isShotButtonPressed = true
                    performShot()
                    Log.d("GameView", "Shot button pressed")
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
                        // Use HeroManager for movement
                        heroManager?.setMovementDirection(deltaX, deltaY)
                    }
                }
            }
            
            MotionEvent.ACTION_UP -> {
                if (isSettingsButtonPressed) {
                    isSettingsButtonPressed = false
                    // Open settings menu and pause game
                    currentGameState = GameState.PAUSED
                    Log.d("GameView", "Settings button released - Game paused")
                }
                
                if (isJoystickPressed) {
                    isJoystickPressed = false
                    joystickX = joystickCenterX
                    joystickY = joystickCenterY
                    // Use HeroManager to stop movement
                    heroManager?.stopMovement()
                    Log.d("GameView", "Joystick released")
                }
                
                if (isAttackButtonPressed) {
                    isAttackButtonPressed = false
                    
                    // Perform combo attack with HeroManager
                    val attackPerformed = heroManager?.performAttack() ?: false
                    if (attackPerformed) {
                        Log.d("GameView", "Hero performed attack")
                        soundManager?.playAttackSound()
                    }
                    
                    Log.d("GameView", "Attack button released")
                }
                
                if (isShotButtonPressed) {
                    isShotButtonPressed = false
                    
                    // Perform bow shot with HeroManager
                    val shotPerformed = heroManager?.performSpecialAttack() ?: false
                    if (shotPerformed) {
                        Log.d("GameView", "Hero performed special attack")
                        soundManager?.playFireballShootSound()
                    }
                    
                    Log.d("GameView", "Shot button released")
                }
            }
        }
        
        return true
    }
    
    private fun performAttack(touchX: Float, touchY: Float) {
        // Perform normal attack with HeroManager
        val attackPerformed = heroManager?.performAttack() ?: false
        if (attackPerformed) {
            Log.d("GameView", "Samurai performed normal attack")
            soundManager?.playAttackSound()
        } else {
            Log.d("GameView", "Cannot perform attack (cooldown or not available)")
        }
    }
    
    private fun performShot() {
        // Perform bow shot with HeroManager
        val shotPerformed = heroManager?.performSpecialAttack() ?: false
        if (shotPerformed) {
            Log.d("GameView", "Hero performed bow shot")
            soundManager?.playFireballShootSound() // Reuse sound or add arrow sound
        } else {
            Log.d("GameView", "Cannot perform shot (cooldown or not available)")
        }
    }

    fun update() {
        // Only update game logic when playing (skip update when paused or game over)
        if (currentGameState != GameState.PLAYING) {
            return
        }
        
        val updateStartTime = System.currentTimeMillis()
        val maxUpdateTime = 12L // Increased to prevent ANR
        
        // Update hero using HeroManager
        heroManager?.let { hm ->
            mapManager?.let { map ->
                if (screenWidth > 0 && screenHeight > 0) {
                    // Update hero with collision detection
                    hm.update(1f/60f, map.getWorldWidth(), map.getWorldHeight(), map)
                    
                    // Check time after hero update (most important)
                    if (System.currentTimeMillis() - updateStartTime > maxUpdateTime) {
                        Log.w("GameView", "Skipping remaining updates - hero update took too long")
                        return
                    }
                    
                    // Update camera to follow hero
                    map.updateCamera(hm.getCameraTargetX(), hm.getCameraTargetY(), screenWidth, screenHeight)
                    
                    // Update minimap exploration (lightweight)
                    miniMap?.updateExploration(hm.getCameraTargetX(), hm.getCameraTargetY())
                    
                    // Check time before medium-cost operations
                    val currentTime = System.currentTimeMillis()
                    val timeUsed = currentTime - updateStartTime
                    
                    if (timeUsed < maxUpdateTime - 2) { // Leave 2ms buffer
                        // Update projectiles (medium cost)
                        projectileManager?.update(1f/60f, map)
                        
                        // Update heroes health system (lightweight)
                        playerHealthSystem?.update(1f/60f)
                    }
                    
                    // Check if we have time left for expensive enemy updates
                    val remainingTime = maxUpdateTime - (System.currentTimeMillis() - updateStartTime)
                    if (remainingTime > 4) { // Need at least 4ms for enemy updates
                        // Update enemies (most expensive operation)
                        enemyManager?.update(1f/60f, hm.getCameraTargetX(), hm.getCameraTargetY())
                        
                        // Check combat interactions if we still have time
                        if (System.currentTimeMillis() - updateStartTime < maxUpdateTime - 1) {
                            checkCombatInteractions()
                        }
                    } else {
                        // Skip enemy updates this frame to prevent ANR
                        if (remainingTime < 0) {
                            Log.w("GameView", "Update frame overran by ${-remainingTime}ms")
                        }
                    }
                    
                    // Performance monitoring
                    val totalUpdateTime = System.currentTimeMillis() - updateStartTime
                    if (totalUpdateTime > maxUpdateTime) {
                        Log.w("GameView", "Frame update took ${totalUpdateTime}ms (target: ${maxUpdateTime}ms)")
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
        
        // Draw hero with camera offset
        heroManager?.let { hm ->
            mapManager?.let { map ->
                // Draw hero
                hm.render(canvas, map.cameraX, map.cameraY)
                
                // Draw projectiles (before lighting so they get shadowed)
                projectileManager?.draw(canvas, map.cameraX, map.cameraY)
                
                // Draw enemies
                enemyManager?.draw(canvas, map.cameraX, map.cameraY)
            }
        }
        
        // Draw UI elements (always on top, not affected by camera)
        drawJoystick(canvas)
        drawAttackButton(canvas)
        drawShotButton(canvas)
        drawSettingsButton(canvas)
        
        // Draw hero health UI in top-left corner
        heroManager?.let { hm ->
            heroHealthUI?.draw(canvas, hm)
        }
        
        // Draw minimap (always on top)
        heroManager?.let { hm ->
            mapManager?.let { map ->
                miniMap?.draw(canvas, map, hm.getCameraTargetX(), hm.getCameraTargetY())
            }
        }
        
        // Draw game over UI if game is over
        if (currentGameState == GameState.GAME_OVER) {
            gameOverUI?.draw(canvas)
        }
        
        // Draw settings UI if paused
        if (currentGameState == GameState.PAUSED) {
            settingsUI?.draw(canvas)
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
            Color.argb(150, 150, 150, 150) // Gray when pressed
        } else {
            Color.argb(120, 200, 200, 200) // Light gray when not pressed
        }
        paint.style = Paint.Style.FILL
        canvas.drawCircle(attackButtonX, attackButtonY, attackButtonRadius, paint)
        
        // Draw attack button border
        paint.color = Color.WHITE
        paint.style = Paint.Style.STROKE
        paint.strokeWidth = 4f
        canvas.drawCircle(attackButtonX, attackButtonY, attackButtonRadius, paint)
        
        // Draw sword symbol (replacing fire symbol)
        paint.color = Color.WHITE
        paint.style = Paint.Style.STROKE
        paint.strokeWidth = 5f

        // Draw sword blade (vertical line)
        val swordLength = attackButtonRadius * 0.6f
        canvas.drawLine(
            attackButtonX,
            attackButtonY - swordLength * 0.7f,
            attackButtonX,
            attackButtonY + swordLength * 0.3f,
            paint
        )

        // Draw sword crossguard (horizontal line)
        val crossguardLength = attackButtonRadius * 0.4f
        canvas.drawLine(
            attackButtonX - crossguardLength / 2f,
            attackButtonY - swordLength * 0.2f,
            attackButtonX + crossguardLength / 2f,
            attackButtonY - swordLength * 0.2f,
            paint
        )

        // Draw sword hilt (grip)
        paint.strokeWidth = 8f
        canvas.drawLine(
            attackButtonX,
            attackButtonY + swordLength * 0.1f,
            attackButtonX,
            attackButtonY + swordLength * 0.3f,
            paint
        )

        // Draw sword pommel (small circle at the bottom)
        paint.style = Paint.Style.FILL
        canvas.drawCircle(attackButtonX, attackButtonY + swordLength * 0.4f, 6f, paint)

        // Draw text label
        paint.textSize = 18f
        paint.textAlign = Paint.Align.CENTER
        canvas.drawText("ATTACK", attackButtonX, attackButtonY + attackButtonRadius + 25f, paint)
    }
    
    private fun drawShotButton(canvas: Canvas) {
        // Draw shot button base
        paint.color = if (isShotButtonPressed) {
            Color.argb(150, 100, 150, 255) // Blue when pressed
        } else {
            Color.argb(120, 50, 100, 200) // Dark blue when not pressed
        }
        paint.style = Paint.Style.FILL
        canvas.drawCircle(shotButtonX, shotButtonY, shotButtonRadius, paint)
        
        // Draw shot button border
        paint.color = Color.WHITE
        paint.style = Paint.Style.STROKE
        paint.strokeWidth = 3f
        canvas.drawCircle(shotButtonX, shotButtonY, shotButtonRadius, paint)
        
        // Draw bow and arrow symbol
        paint.color = Color.WHITE
        paint.style = Paint.Style.STROKE
        paint.strokeWidth = 4f
        
        // Draw bow shape (arc)
        val bowRadius = shotButtonRadius * 0.4f
        val bowRect = RectF(
            shotButtonX - bowRadius,
            shotButtonY - bowRadius,
            shotButtonX + bowRadius,
            shotButtonY + bowRadius
        )
        canvas.drawArc(bowRect, -45f, 90f, false, paint)
        
        // Draw arrow
        paint.strokeWidth = 3f
        val arrowLength = bowRadius * 0.8f
        canvas.drawLine(
            shotButtonX - arrowLength * 0.5f,
            shotButtonY,
            shotButtonX + arrowLength * 0.8f,
            shotButtonY,
            paint
        )
        
        // Draw arrow head
        paint.style = Paint.Style.FILL
        val arrowPath = Path()
        val arrowHeadX = shotButtonX + arrowLength * 0.8f
        arrowPath.moveTo(arrowHeadX, shotButtonY)
        arrowPath.lineTo(arrowHeadX - 8f, shotButtonY - 6f)
        arrowPath.lineTo(arrowHeadX - 8f, shotButtonY + 6f)
        arrowPath.close()
        canvas.drawPath(arrowPath, paint)
        
        // Draw text label
        paint.textSize = 20f
        paint.textAlign = Paint.Align.CENTER
        canvas.drawText("SHOT", shotButtonX, shotButtonY + shotButtonRadius + 25f, paint)
    }
    
    private fun drawSettingsButton(canvas: Canvas) {
        // Draw settings button background
        paint.color = if (isSettingsButtonPressed) {
            Color.argb(180, 100, 100, 100) // Darker when pressed
        } else {
            Color.argb(120, 150, 150, 150) // Gray when not pressed
        }
        paint.style = Paint.Style.FILL
        val buttonRect = RectF(
            settingsButtonX - settingsButtonSize / 2f,
            settingsButtonY - settingsButtonSize / 2f,
            settingsButtonX + settingsButtonSize / 2f,
            settingsButtonY + settingsButtonSize / 2f
        )
        canvas.drawRoundRect(buttonRect, 8f, 8f, paint)
        
        // Draw settings button border
        paint.color = Color.WHITE
        paint.style = Paint.Style.STROKE
        paint.strokeWidth = 2f
        canvas.drawRoundRect(buttonRect, 8f, 8f, paint)
        
        // Draw gear icon (⚙️) or three lines
        paint.color = Color.WHITE
        paint.style = Paint.Style.FILL
        paint.textSize = 32f
        paint.textAlign = Paint.Align.CENTER
        
        // Draw gear symbol
        canvas.drawText("⚙", settingsButtonX, settingsButtonY + 10f, paint)
        
        // Alternative: draw three horizontal lines for settings
        paint.strokeWidth = 3f
        paint.style = Paint.Style.STROKE
        val lineLength = settingsButtonSize * 0.4f
        val lineSpacing = 6f
        
        // Top line
        canvas.drawLine(
            settingsButtonX - lineLength / 2f, settingsButtonY - lineSpacing,
            settingsButtonX + lineLength / 2f, settingsButtonY - lineSpacing,
            paint
        )
        // Middle line
        canvas.drawLine(
            settingsButtonX - lineLength / 2f, settingsButtonY,
            settingsButtonX + lineLength / 2f, settingsButtonY,
            paint
        )
        // Bottom line
        canvas.drawLine(
            settingsButtonX - lineLength / 2f, settingsButtonY + lineSpacing,
            settingsButtonX + lineLength / 2f, settingsButtonY + lineSpacing,
            paint
        )
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
        projectileManager?.cleanup()
        projectileManager = null
        soundManager?.cleanup()
        soundManager = null
    }
    
    private fun handleSettingsButtonClick(buttonType: SettingsUI.ButtonType) {
        Log.d("GameView", "Settings button clicked: $buttonType")
        
        when (buttonType) {
            SettingsUI.ButtonType.CONTINUE -> {
                // Resume game
                currentGameState = GameState.PLAYING
                Log.d("GameView", "Game resumed")
            }
            
            SettingsUI.ButtonType.MUSIC_TOGGLE -> {
                // Toggle music on/off
                soundManager?.let { sm ->
                    if (sm.isMusicEnabled()) {
                        sm.stopBackgroundMusic()
                        sm.setMusicEnabled(false)
                        Log.d("GameView", "Background music disabled")
                    } else {
                        sm.setMusicEnabled(true)
                        sm.startBackgroundMusic()
                        Log.d("GameView", "Background music enabled")
                    }
                    // Update settings UI with new music state
                    settingsUI?.updateMusicState(sm.isMusicEnabled())
                }
            }
            
            SettingsUI.ButtonType.SFX_TOGGLE -> {
                // Toggle sound effects on/off
                soundManager?.let { sm ->
                    if (sm.isSfxEnabled()) {
                        sm.setSfxEnabled(false)
                        Log.d("GameView", "Sound effects disabled")
                    } else {
                        sm.setSfxEnabled(true)
                        Log.d("GameView", "Sound effects enabled")
                    }
                    // Update settings UI with new SFX state
                    settingsUI?.updateSfxState(sm.isSfxEnabled())
                }
            }
            
            SettingsUI.ButtonType.EXIT -> {
                // Exit to main menu - go to game over state for now
                // In a real game, this would return to the main menu activity
                currentGameState = GameState.GAME_OVER
                Log.d("GameView", "Exiting to main menu")
            }
        }
    }
    
    // Public methods for game state control
    fun pauseGame() {
        if (currentGameState == GameState.PLAYING) {
            currentGameState = GameState.PAUSED
        }
    }
    
    fun resumeGame() {
        if (currentGameState == GameState.PAUSED) {
            currentGameState = GameState.PLAYING
        }
    }
    
    fun isGamePaused(): Boolean {
        return currentGameState == GameState.PAUSED
    }

    inner class GameThread(
        private val surfaceHolder: SurfaceHolder,
        private val gameView: GameView
    ) : Thread() {
        
        @Volatile
        private var running = false
        private var targetFPS = 60
        private var targetTime = (1000.0 / targetFPS).toLong()
        private val maxFrameSkip = 3 // Reduced from 5
        
        // Adaptive FPS
        private var frameTimeHistory = mutableListOf<Long>()
        private var adaptiveFpsCounter = 0
        private val adaptiveFpsCheckInterval = 60 // Check every 60 frames
        
        // Performance monitoring
        private var slowFrameCount = 0
        private var lastAdaptiveCheck = System.currentTimeMillis()

        fun setRunning(isRunning: Boolean) {
            synchronized(this) {
                running = isRunning
            }
        }

        override fun run() {
            var nextGameTick = System.currentTimeMillis()
            var loops: Int
            var frameStartTime: Long
            
            // ANR Prevention: More aggressive timeout
            val maxFrameTime = 20L // Maximum time per frame to prevent ANR
            
            while (running) {
                frameStartTime = System.currentTimeMillis()
                loops = 0
                
                // Update game logic with enhanced time management and ANR prevention
                while (System.currentTimeMillis() > nextGameTick && loops < maxFrameSkip) {
                    try {
                        val updateStart = System.currentTimeMillis()
                        gameView.update()
                        val updateTime = System.currentTimeMillis() - updateStart
                        
                        // More aggressive time limits to prevent ANR
                        val updateTimeLimit = if (targetFPS >= 60) 15 else 20 // Increased limits
                        if (updateTime > updateTimeLimit) {
                            Log.w("GameThread", "Update took ${updateTime}ms (limit: ${updateTimeLimit}ms), breaking to prevent ANR")
                            break
                        }
                    } catch (e: Exception) {
                        Log.e("GameThread", "Error in update", e)
                        // Don't break on exceptions, just log and continue
                    }
                    nextGameTick += targetTime
                    loops++
                    
                    // ANR Prevention: Break if frame is taking too long
                    if (System.currentTimeMillis() - frameStartTime > maxFrameTime) {
                        Log.w("GameThread", "Breaking update loop - frame taking too long")
                        break
                    }
                }
                
                // Render frame with enhanced time management
                var canvas: Canvas? = null
                try {
                    if (surfaceHolder.surface.isValid) {
                        val renderStart = System.currentTimeMillis()
                        
                        // Skip render if we're already taking too long
                        if (renderStart - frameStartTime > maxFrameTime) {
                            Log.w("GameThread", "Skipping render to prevent ANR")
                            continue
                        }
                        
                        canvas = surfaceHolder.lockCanvas()
                        canvas?.let { c ->
                            synchronized(surfaceHolder) {
                                gameView.renderGame(c)
                            }
                        }
                        val renderTime = System.currentTimeMillis() - renderStart
                        
                        // Track render performance
                        if (renderTime > 18) { // More lenient render time limit
                            slowFrameCount++
                            Log.w("GameThread", "Slow render: ${renderTime}ms")
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
                
                // Enhanced frame timing with adaptive FPS and ANR prevention
                val frameTime = System.currentTimeMillis() - frameStartTime
                frameTimeHistory.add(frameTime)
                
                // Keep only recent frame times
                if (frameTimeHistory.size > 10) {
                    frameTimeHistory.removeAt(0)
                }
                
                // ANR Prevention: Force sleep if frame took too long
                if (frameTime > maxFrameTime) {
                    Log.w("GameThread", "Frame took ${frameTime}ms - forcing sleep to prevent ANR")
                    try {
                        Thread.sleep(5) // Force a break
                    } catch (e: InterruptedException) {
                        Log.w("GameThread", "Sleep interrupted")
                    }
                }
                
                // Adaptive FPS adjustment
                adaptiveFpsCounter++
                if (adaptiveFpsCounter >= adaptiveFpsCheckInterval) {
                    adjustTargetFPS()
                    adaptiveFpsCounter = 0
                    slowFrameCount = 0
                }
                
                // Sleep to maintain target FPS
                val sleepTime = targetTime - frameTime
                if (sleepTime > 0) {
                    try {
                        Thread.sleep(sleepTime)
                    } catch (e: InterruptedException) {
                        Log.w("GameThread", "Sleep interrupted")
                    }
                }
            }
        }
        
        private fun adjustTargetFPS() {
            val currentTime = System.currentTimeMillis()
            if (currentTime - lastAdaptiveCheck < 2000) return // Don't adjust too frequently
            
            val avgFrameTime = frameTimeHistory.average()
            val slowFrameRatio = slowFrameCount.toFloat() / adaptiveFpsCheckInterval
            
            when {
                slowFrameRatio > 0.2f && targetFPS > 30 -> {
                    // Too many slow frames, reduce target FPS
                    targetFPS = 45
                    targetTime = (1000.0 / targetFPS).toLong()
                    Log.d("GameThread", "Reducing target FPS to $targetFPS (slow frames: $slowFrameCount)")
                }
                slowFrameRatio < 0.05f && avgFrameTime < 12 && targetFPS < 60 -> {
                    // Performance is good, can increase FPS
                    targetFPS = 60
                    targetTime = (1000.0 / targetFPS).toLong()
                    Log.d("GameThread", "Increasing target FPS to $targetFPS")
                }
            }
            
            lastAdaptiveCheck = currentTime
        }
    }
    
    private fun checkCombatInteractions() {
        heroManager?.let { hm ->
            enemyManager?.let { em ->
                // Check projectile hits on enemies (from hero)
                var projectileHits = 0
                if (hm.getCurrentHero() is SamuraiArcher) {
                    val samurai = hm.getCurrentHero() as SamuraiArcher
                    projectileHits = em.checkArrowCollisions(samurai.getArrows())
                }
                
                // Check melee attack hits on enemies (from hero)
                var meleeHits = 0
                if (hm.getCurrentHero() is SamuraiArcher) {
                    val samurai = hm.getCurrentHero() as SamuraiArcher
                    meleeHits = em.checkMeleeAttackCollisions(samurai.getAttackHitbox())
                }
                
                // Check if enemies attack hero
                val damageToHero = em.checkSkeletonAttacks(hm.getCameraTargetX(), hm.getCameraTargetY())
                
                if (damageToHero > 0) {
                    hm.takeDamage(damageToHero.toFloat())
                    soundManager?.playHurtSound() // Play hurt sound when hero takes damage
                    Log.d("GameView", "Hero takes $damageToHero damage from enemies!")
                }
                
                if (projectileHits > 0) {
                    soundManager?.playFireballExplodeSound() // Use sound for projectile hits
                    Log.d("GameView", "Projectiles hit $projectileHits enemies!")
                }
                
                if (meleeHits > 0) {
                    soundManager?.playFireballExplodeSound() // Use same sound for melee hits  
                    Log.d("GameView", "Melee attacks hit $meleeHits enemies!")
                }
                
                // Check if hero died
                if (hm.isDead() && currentGameState == GameState.PLAYING) {
                    Log.d("GameView", "Game Over - Hero died!")
                    triggerGameOver()
                }
            }
        }
    }
    
    private fun triggerGameOver() {
        currentGameState = GameState.GAME_OVER
        soundManager?.playGameOverSound() // Play game over sound
        Log.d("GameView", "Game state changed to GAME_OVER")
    }
    
    private fun restartGame() {
        Log.d("GameView", "Restarting game...")
        
        // Reset game state
        currentGameState = GameState.PLAYING
        
        // Reset hero through HeroManager
        heroManager?.reset()
        
        // Reset hero position
        mapManager?.let { map ->
            heroManager?.setCurrentHeroPosition(map.playerStartX, map.playerStartY)
        }
        
        // Clear all enemies and projectiles
        enemyManager?.clearAllSkeletons()
        projectileManager?.clearAllProjectiles()
        
        // Spawn new enemies
        heroManager?.let { hm ->
            enemyManager?.spawnInitialSkeletons(hm.getCameraTargetX(), hm.getCameraTargetY(), 3)
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
