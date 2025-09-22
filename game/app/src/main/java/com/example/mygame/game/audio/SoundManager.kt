package com.example.mygame.game.audio

import android.content.Context
import android.content.res.AssetFileDescriptor
import android.media.MediaPlayer
import android.media.SoundPool
import android.util.Log
import java.io.IOException

class SoundManager(private val context: Context) {
    
    // Background music player
    private var backgroundMusicPlayer: MediaPlayer? = null
    private var isMusicEnabled = true
    private var musicVolume = 0.7f // Default volume 70%
    
    // Sound effects player
    private var soundPool: SoundPool? = null
    private var isSfxEnabled = true
    private var sfxVolume = 0.8f // Default volume 80%
    
    // Sound effect IDs (will be loaded later if needed)
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
    
    // Background music controls
    fun startBackgroundMusic() {
        Log.d("SoundManager", "startBackgroundMusic called - isMusicEnabled: $isMusicEnabled, player null: ${backgroundMusicPlayer == null}")
        if (isMusicEnabled && backgroundMusicPlayer != null) {
            try {
                val isPlaying = backgroundMusicPlayer!!.isPlaying
                Log.d("SoundManager", "Music player status - isPlaying: $isPlaying")
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
                Log.e("SoundManager", "Background music player is null")
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
    
    // Sound effect methods (for future use)
    fun playAttackSound() {
        if (isSfxEnabled && soundPool != null && attackSoundId != -1) {
            soundPool!!.play(attackSoundId, sfxVolume, sfxVolume, 1, 0, 1f)
        }
    }
    
    fun playHurtSound() {
        if (isSfxEnabled && soundPool != null && hurtSoundId != -1) {
            soundPool!!.play(hurtSoundId, sfxVolume, sfxVolume, 1, 0, 1f)
        }
    }
    
    fun playExplosionSound() {
        if (isSfxEnabled && soundPool != null && explosionSoundId != -1) {
            soundPool!!.play(explosionSoundId, sfxVolume, sfxVolume, 1, 0, 1f)
        }
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
                Log.e("SoundManager", "Error checking player status: ${e.message}")
            }
        }
        Log.d("SoundManager", "========================")
    }
    
    // Load sound effects from assets (future enhancement)
    private fun loadSoundEffects() {
        // This method can be enhanced later to load actual sound effect files
        // For now, we'll leave the sound IDs as -1 (not loaded)
        try {
            soundPool?.let { pool ->
                // Example of how to load sound effects:
                // attackSoundId = pool.load(context.assets.openFd("sounds/attack.ogg"), 1)
                // hurtSoundId = pool.load(context.assets.openFd("sounds/hurt.ogg"), 1)
                // explosionSoundId = pool.load(context.assets.openFd("sounds/explosion.ogg"), 1)
                // pickupSoundId = pool.load(context.assets.openFd("sounds/pickup.ogg"), 1)
                
                Log.d("SoundManager", "Sound effects loading completed")
            }
        } catch (e: Exception) {
            Log.e("SoundManager", "Failed to load sound effects: ${e.message}")
        }
    }
}