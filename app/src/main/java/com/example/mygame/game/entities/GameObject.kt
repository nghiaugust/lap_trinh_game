package com.example.mygame.game.entities

/**
 * Base class cho tất cả game objects
 */
abstract class GameObject(
    var x: Float = 0f,
    var y: Float = 0f,
    var width: Float = 0f,
    var height: Float = 0f
) {
    var isActive: Boolean = true

    abstract fun update(deltaTime: Float)
    abstract fun render()

    fun getBounds(): Bounds {
        return Bounds(x, y, width, height)
    }
}

data class Bounds(
    val x: Float,
    val y: Float,
    val width: Float,
    val height: Float
) {
    fun intersects(other: Bounds): Boolean {
        return x < other.x + other.width &&
                x + width > other.x &&
                y < other.y + other.height &&
                y + height > other.y
    }
}
