package com.example.mygame.game.assets

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.media.SoundPool
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.IOException

/**
 * Asset Manager cho game Dungeon Crawler
 * Quản lý việc load và cache các tài nguyên từ assets folder
 */
class GameAssetManager(private val context: Context) {

    // Cache cho textures
    private val textureCache = mutableMapOf<String, ImageBitmap>()

    // Sound pool cho sound effects
    private val soundPool = SoundPool.Builder()
        .setMaxStreams(10)
        .build()

    // Cache cho sounds
    private val soundCache = mutableMapOf<String, Int>()

    /**
     * Load texture từ assets/textures/
     */
    suspend fun loadTexture(path: String): ImageBitmap? = withContext(Dispatchers.IO) {
        if (textureCache.containsKey(path)) {
            return@withContext textureCache[path]
        }

        try {
            val fullPath = "textures/$path"
            val inputStream = context.assets.open(fullPath)
            val bitmap = BitmapFactory.decodeStream(inputStream)
            val imageBitmap = bitmap.asImageBitmap()

            textureCache[path] = imageBitmap
            inputStream.close()

            imageBitmap
        } catch (e: IOException) {
            e.printStackTrace()
            null
        }
    }

    /**
     * Load sound effect từ assets/sounds/
     */
    fun loadSound(path: String): Int? {
        if (soundCache.containsKey(path)) {
            return soundCache[path]
        }

        return try {
            val fullPath = "sounds/$path"
            val soundId = soundPool.load(context.assets.openFd(fullPath), 1)
            soundCache[path] = soundId
            soundId
        } catch (e: IOException) {
            e.printStackTrace()
            null
        }
    }

    /**
     * Play sound effect
     */
    fun playSound(path: String, volume: Float = 1.0f) {
        val soundId = loadSound(path)
        soundId?.let {
            soundPool.play(it, volume, volume, 0, 0, 1.0f)
        }
    }

    /**
     * Load file text từ assets/data/
     */
    suspend fun loadDataFile(path: String): String? = withContext(Dispatchers.IO) {
        try {
            val fullPath = "data/$path"
            context.assets.open(fullPath).bufferedReader().use { it.readText() }
        } catch (e: IOException) {
            e.printStackTrace()
            null
        }
    }

    /**
     * Preload essential assets
     */
    suspend fun preloadEssentialAssets() {
        // Load player textures
        loadTexture("characters/player_idle.png")
        loadTexture("characters/player_walk_01.png")

        // Load UI sounds
        loadSound("ui_button_click.wav")
        loadSound("item_pickup.wav")

        // Load common environment textures
        loadTexture("environment/dungeon_floor.png")
        loadTexture("environment/dungeon_wall.png")
    }

    /**
     * Clear cache để giải phóng memory
     */
    fun clearCache() {
        textureCache.clear()
        soundCache.clear()
    }

    /**
     * Cleanup resources
     */
    fun dispose() {
        clearCache()
        soundPool.release()
    }
}
