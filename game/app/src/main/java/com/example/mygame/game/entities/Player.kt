package com.example.mygame.game.entities

/**
 * Player entity cho game
 */
class Player(
    x: Float = 0f,
    y: Float = 0f,
    width: Float = 32f,
    height: Float = 32f
) : GameObject(x, y, width, height) {

    var health: Int = 100
    var maxHealth: Int = 100
    var level: Int = 1
    var experience: Int = 0
    var attack: Int = 10
    var defense: Int = 5
    var speed: Float = 100f

    var velocityX: Float = 0f
    var velocityY: Float = 0f

    override fun update(deltaTime: Float) {
        // Cập nhật vị trí dựa trên velocity
        x += velocityX * deltaTime
        y += velocityY * deltaTime

        // Giới hạn player trong màn hình
        // TODO: Implement bounds checking
    }

    override fun render() {
        // TODO: Implement rendering logic
    }

    fun takeDamage(damage: Int) {
        health = (health - damage).coerceAtLeast(0)
    }

    fun heal(amount: Int) {
        health = (health + amount).coerceAtMost(maxHealth)
    }

    fun isAlive(): Boolean = health > 0

    fun move(directionX: Float, directionY: Float) {
        velocityX = directionX * speed
        velocityY = directionY * speed
    }

    fun stop() {
        velocityX = 0f
        velocityY = 0f
    }
}
