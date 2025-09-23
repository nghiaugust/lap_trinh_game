package com.example.mygame.game.audio

import android.content.Context
import android.content.res.AssetFileDescriptor
import android.media.MediaPlayer
import android.media.SoundPool
import android.util.Log
import java.io.IOException

class SoundManager(private val context: Context) {
    
    // Background music heroes
    private var backgroundMusicPlayer: MediaPlayer? = null
    private var isMusicEnabled = true
    private var musicVolume = 0.7f // Default volume 70%
    
    // Sound effects heroes
    private var soundPool: SoundPool? = null
    private var isSfxEnabled = true
    private var sfxVolume = 0.8f // Default volume 80%
    
    // Sound effect IDs (will be loaded later if needed)
    private var fireballShootSoundId = -1  // ban_lua.mp3
    private var fireballExplodeSoundId = -1  // lua_no.mp3
    private var walkSoundId = -1  // walk.mp3
    private var gameOverSoundId = -1  // game-over.mp3
    private var attackSoundId = -1
    private var hurtSoundId = -1
    private var explosionSoundId = -1
    private var pickupSoundId = -1
    
    companion object {
        private const val MAX_SOUND_STREAMS = 10
    }
    
    init {
        initializeSoundPool()
        loadBackgroundMusic()
        loadSoundEffects()
    }
    
    private fun initializeSoundPool() {
        try {
            soundPool = SoundPool.Builder()
                .setMaxStreams(MAX_SOUND_STREAMS)
                .build()
            Log.d("SoundManager", "SoundPool initialized successfully")
        } catch (e: Exception) {
            Log.e("SoundManager", "Failed to initialize SoundPool: ${e.message}")
        }
    }
    
    private fun loadBackgroundMusic() {
        try {
            Log.d("SoundManager", "Starting to load background music...")
            backgroundMusicPlayer = MediaPlayer()
            
            // Load the background music from assets
            Log.d("SoundManager", "Attempting to open music file: music/DreamscapeDrift.mp3")
            val assetFileDescriptor: AssetFileDescriptor = context.assets.openFd("music/DreamscapeDrift.mp3")
            Log.d("SoundManager", "Successfully opened music file")
            
            backgroundMusicPlayer?.apply {
                setDataSource(assetFileDescriptor.fileDescriptor, assetFileDescriptor.startOffset, assetFileDescriptor.length)
                isLooping = true // Loop the background music
                setVolume(musicVolume, musicVolume)
                Log.d("SoundManager", "MediaPlayer configured, preparing asynchronously...")
                prepareAsync() // Prepare asynchronously to avoid blocking
                
                setOnPreparedListener {
                    Log.d("SoundManager", "Background music prepared successfully, isMusicEnabled: $isMusicEnabled")
                    if (isMusicEnabled) {
                        startBackgroundMusic()
                    }
                }
                
                setOnErrorListener { mp, what, extra ->
                    Log.e("SoundManager", "MediaPlayer error: what=$what, extra=$extra")
                    true
                }
            }
            
            assetFileDescriptor.close()
            
        } catch (e: IOException) {
            Log.e("SoundManager", "Failed to load background music (IOException): ${e.message}")
            e.printStackTrace()
        } catch (e: Exception) {
            Log.e("SoundManager", "Unexpected error loading background music: ${e.message}")
            e.printStackTrace()
        }
    }
    
    private fun loadSoundEffects() {
        soundPool?.let { pool ->
            try {
                Log.d("SoundManager", "Loading sound effects...")
                
                // Load fireball shoot sound
                fireballShootSoundId = pool.load(context.assets.openFd("music/fireball/ban_lua.mp3"), 1)
                Log.d("SoundManager", "Fireball shoot sound loaded with ID: $fireballShootSoundId")
                
                // Load fireball explode sound
                fireballExplodeSoundId = pool.load(context.assets.openFd("music/fireball/lua_no.mp3"), 1)
                Log.d("SoundManager", "Fireball explode sound loaded with ID: $fireballExplodeSoundId")
                
                // Load walk sound
                walkSoundId = pool.load(context.assets.openFd("music/character/walk.mp3"), 1)
                Log.d("SoundManager", "Walk sound loaded with ID: $walkSoundId")
                
                // Load game over sound
                gameOverSoundId = pool.load(context.assets.openFd("music/game-over.mp3"), 1)
                Log.d("SoundManager", "Game over sound loaded with ID: $gameOverSoundId")
                
                Log.d("SoundManager", "All sound effects loaded successfully")
                
            } catch (e: IOException) {
                Log.e("SoundManager", "Failed to load sound effects (IOException): ${e.message}")
                e.printStackTrace()
            } catch (e: Exception) {
                Log.e("SoundManager", "Unexpected error loading sound effects: ${e.message}")
                e.printStackTrace()
            }
        } ?: Log.e("SoundManager", "SoundPool is null, cannot load sound effects")
    }
    
    // Background music controls
    fun startBackgroundMusic() {
        Log.d("SoundManager", "startBackgroundMusic called - isMusicEnabled: $isMusicEnabled, heroes null: ${backgroundMusicPlayer == null}")
        if (isMusicEnabled && backgroundMusicPlayer != null) {
            try {
                val isPlaying = backgroundMusicPlayer!!.isPlaying
                Log.d("SoundManager", "Music heroes status - isPlaying: $isPlaying")
                if (!isPlaying) {
                    backgroundMusicPlayer!!.start()
                    Log.d("SoundManager", "Background music started successfully")
                } else {
                    Log.d("SoundManager", "Background music is already playing")
                }
            } catch (e: Exception) {
                Log.e("SoundManager", "Failed to start background music: ${e.message}")
                e.printStackTrace()
            }
        } else {
            if (!isMusicEnabled) {
                Log.d("SoundManager", "Music is disabled")
            }
            if (backgroundMusicPlayer == null) {
                Log.e("SoundManager", "Background music heroes is null")
            }
        }
    }
    
    fun pauseBackgroundMusic() {
        try {
            backgroundMusicPlayer?.let { player ->
                if (player.isPlaying) {
                    player.pause()
                    Log.d("SoundManager", "Background music paused")
                }
            }
        } catch (e: Exception) {
            Log.e("SoundManager", "Failed to pause background music: ${e.message}")
        }
    }
    
    fun stopBackgroundMusic() {
        try {
            backgroundMusicPlayer?.let { player ->
                if (player.isPlaying) {
                    player.stop()
                    player.prepare() // Prepare for next play
                    Log.d("SoundManager", "Background music stopped")
                }
            }
        } catch (e: Exception) {
            Log.e("SoundManager", "Failed to stop background music: ${e.message}")
        }
    }
    
    fun resumeBackgroundMusic() {
        startBackgroundMusic() // Same as start for our use case
    }
    
    // Volume controls
    fun setMusicVolume(volume: Float) {
        musicVolume = volume.coerceIn(0f, 1f)
        backgroundMusicPlayer?.setVolume(musicVolume, musicVolume)
        Log.d("SoundManager", "Music volume set to $musicVolume")
    }
    
    fun setSfxVolume(volume: Float) {
        sfxVolume = volume.coerceIn(0f, 1f)
        Log.d("SoundManager", "SFX volume set to $sfxVolume")
    }
    
    // Enable/disable controls
    fun enableMusic(enabled: Boolean) {
        isMusicEnabled = enabled
        if (enabled) {
            startBackgroundMusic()
        } else {
            pauseBackgroundMusic()
        }
        Log.d("SoundManager", "Music enabled: $enabled")
    }
    
    fun enableSfx(enabled: Boolean) {
        isSfxEnabled = enabled
        Log.d("SoundManager", "SFX enabled: $enabled")
    }
    
    // Sound effect methods
    fun playFireballShootSound() {
        if (isSfxEnabled && soundPool != null && fireballShootSoundId != -1) {
            soundPool!!.play(fireballShootSoundId, sfxVolume, sfxVolume, 1, 0, 1f)
            Log.d("SoundManager", "Playing fireball shoot sound")
        }
    }
    
    fun playFireballExplodeSound() {
        if (isSfxEnabled && soundPool != null && fireballExplodeSoundId != -1) {
            soundPool!!.play(fireballExplodeSoundId, sfxVolume, sfxVolume, 1, 0, 1f)
            Log.d("SoundManager", "Playing fireball explode sound")
        }
    }
    
    fun playWalkSound() {
        if (isSfxEnabled && soundPool != null && walkSoundId != -1) {
            soundPool!!.play(walkSoundId, sfxVolume * 0.6f, sfxVolume * 0.6f, 1, 0, 1f) // Lower volume for walk
            Log.d("SoundManager", "Playing walk sound")
        }
    }
    
    fun playGameOverSound() {
        if (isSfxEnabled && soundPool != null && gameOverSoundId != -1) {
            soundPool!!.play(gameOverSoundId, sfxVolume, sfxVolume, 1, 0, 1f)
            Log.d("SoundManager", "Playing game over sound")
        }
    }
    
    // Legacy sound effect methods (keep for compatibility)
    fun playAttackSound() {
        // Use fireball shoot sound as attack sound
        playFireballShootSound()
    }
    
    fun playHurtSound() {
        if (isSfxEnabled && soundPool != null && hurtSoundId != -1) {
            soundPool!!.play(hurtSoundId, sfxVolume, sfxVolume, 1, 0, 1f)
        }
    }
    
    fun playExplosionSound() {
        // Use fireball explode sound as explosion sound
        playFireballExplodeSound()
    }
    
    fun playPickupSound() {
        if (isSfxEnabled && soundPool != null && pickupSoundId != -1) {
            soundPool!!.play(pickupSoundId, sfxVolume, sfxVolume, 1, 0, 1f)
        }
    }
    
    // Getters for current state
    fun isMusicEnabled(): Boolean = isMusicEnabled
    fun isSfxEnabled(): Boolean = isSfxEnabled
    fun getMusicVolume(): Float = musicVolume
    fun getSfxVolume(): Float = sfxVolume
    fun isMusicPlaying(): Boolean = backgroundMusicPlayer?.isPlaying ?: false
    
    // Setters for music and sound effects state
    fun setMusicEnabled(enabled: Boolean) {
        isMusicEnabled = enabled
        if (enabled) {
            startBackgroundMusic()
        } else {
            stopBackgroundMusic()
        }
        Log.d("SoundManager", "Music enabled set to: $enabled")
    }
    
    fun setSfxEnabled(enabled: Boolean) {
        isSfxEnabled = enabled
        Log.d("SoundManager", "SFX enabled set to: $enabled")
    }
    
    // Lifecycle management
    fun onPause() {
        pauseBackgroundMusic()
        Log.d("SoundManager", "SoundManager paused")
    }
    
    fun onResume() {
        if (isMusicEnabled) {
            resumeBackgroundMusic()
        }
        Log.d("SoundManager", "SoundManager resumed")
    }
    
    fun cleanup() {
        try {
            // Stop and release background music
            backgroundMusicPlayer?.let { player ->
                if (player.isPlaying) {
                    player.stop()
                }
                player.release()
            }
            backgroundMusicPlayer = null
            
            // Release sound pool
            soundPool?.release()
            soundPool = null
            
            Log.d("SoundManager", "SoundManager cleaned up")
        } catch (e: Exception) {
            Log.e("SoundManager", "Error during cleanup: ${e.message}")
        }
    }
    
    // Debug method to check music status
    fun debugMusicStatus() {
        Log.d("SoundManager", "=== Music Status Debug ===")
        Log.d("SoundManager", "isMusicEnabled: $isMusicEnabled")
        Log.d("SoundManager", "musicVolume: $musicVolume")
        Log.d("SoundManager", "backgroundMusicPlayer is null: ${backgroundMusicPlayer == null}")
        
        backgroundMusicPlayer?.let { player ->
            try {
                Log.d("SoundManager", "Music isPlaying: ${player.isPlaying}")
                Log.d("SoundManager", "Music currentPosition: ${player.currentPosition}")
                Log.d("SoundManager", "Music duration: ${player.duration}")
            } catch (e: Exception) {
                Log.e("SoundManager", "Error checking heroes status: ${e.message}")
            }
        }
        Log.d("SoundManager", "========================")
    }
}