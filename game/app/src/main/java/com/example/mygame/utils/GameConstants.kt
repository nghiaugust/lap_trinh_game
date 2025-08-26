package com.example.mygame.utils

/**
 * Constants cho game
 */
object GameConstants {
    // Screen dimensions
    const val SCREEN_WIDTH = 1920
    const val SCREEN_HEIGHT = 1080

    // Game settings
    const val TARGET_FPS = 60
    const val PHYSICS_STEP = 1f / 60f

    // Player settings
    const val PLAYER_SPEED = 200f
    const val PLAYER_HEALTH = 100
    const val PLAYER_SIZE = 32f

    // Enemy settings
    const val ENEMY_SPEED = 100f
    const val ENEMY_HEALTH = 50

    // Game mechanics
    const val ATTACK_RANGE = 50f
    const val EXPERIENCE_PER_LEVEL = 100

    // Asset paths
    const val TEXTURE_PATH = "textures/"
    const val SOUND_PATH = "sounds/"
    const val MUSIC_PATH = "music/"
}
