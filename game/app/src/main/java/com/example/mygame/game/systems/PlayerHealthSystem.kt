package com.example.mygame.game.systems

import android.graphics.*
import android.util.Log

class PlayerHealthSystem {
    // Player stats
    private var currentHealth = 50
    private var maxHealth = 50
    private var currentArmor = 100
    private var maxArmor = 100
    
    // Armor regeneration
    private var lastArmorRegenTime = 0L
    private val armorRegenInterval = 5000L // 5 seconds
    private val armorRegenAmount = 10
    
    // Damage system
    private var lastDamageTime = 0L
    private val damageCooldown = 1000L // 1 second invincibility after taking damage
    
    // Damage flash effect
    private var damageFlashTimer = 0f
    private val damageFlashDuration = 300f // 300ms flash
    private var isFlashing = false
    
    // UI settings
    private val barWidth = 200f
    private val barHeight = 25f
    private val barSpacing = 35f
    private val uiMargin = 20f
    
    // Paint objects
    private val borderPaint = Paint().apply {
        color = Color.BLACK
        style = Paint.Style.STROKE
        strokeWidth = 3f
        isAntiAlias = true
    }
    
    private val backgroundPaint = Paint().apply {
        color = Color.argb(150, 50, 50, 50)
        style = Paint.Style.FILL
    }
    
    private val healthPaint = Paint().apply {
        style = Paint.Style.FILL
        isAntiAlias = true
    }
    
    private val armorPaint = Paint().apply {
        color = Color.argb(255, 100, 150, 255) // Blue armor
        style = Paint.Style.FILL
        isAntiAlias = true
    }
    
    private val textPaint = Paint().apply {
        color = Color.WHITE
        textSize = 20f
        textAlign = Paint.Align.LEFT
        isAntiAlias = true
        setShadowLayer(2f, 1f, 1f, Color.BLACK)
    }
    
    private val labelPaint = Paint().apply {
        color = Color.WHITE
        textSize = 16f
        textAlign = Paint.Align.LEFT
        isAntiAlias = true
        setShadowLayer(2f, 1f, 1f, Color.BLACK)
    }
    
    fun update(deltaTime: Float) {
        // Update damage flash effect
        if (isFlashing) {
            damageFlashTimer += deltaTime * 1000f
            if (damageFlashTimer >= damageFlashDuration) {
                isFlashing = false
                damageFlashTimer = 0f
            }
        }
        
        // Handle armor regeneration
        val currentTime = System.currentTimeMillis()
        if (currentTime - lastArmorRegenTime >= armorRegenInterval) {
            regenerateArmor()
            lastArmorRegenTime = currentTime
        }
    }
    
    fun takeDamage(damage: Int) {
        // Record damage time for cooldown
        lastDamageTime = System.currentTimeMillis()
        
        if (currentArmor > 0) {
            // Armor absorbs damage first
            val armorDamage = minOf(damage, currentArmor)
            currentArmor -= armorDamage
            val remainingDamage = damage - armorDamage
            
            Log.d("PlayerHealth", "Armor absorbed $armorDamage damage, remaining armor: $currentArmor")
            
            // If there's remaining damage after armor is depleted
            if (remainingDamage > 0) {
                currentHealth -= remainingDamage
                currentHealth = maxOf(0, currentHealth)
                Log.d("PlayerHealth", "Health took $remainingDamage damage, remaining health: $currentHealth")
            }
        } else {
            // No armor, damage goes directly to health
            currentHealth -= damage
            currentHealth = maxOf(0, currentHealth)
            Log.d("PlayerHealth", "Health took $damage damage, remaining health: $currentHealth")
        }
        
        // Trigger flash effect
        isFlashing = true
        damageFlashTimer = 0f
        
        // Check if heroes died
        if (currentHealth <= 0) {
            Log.d("PlayerHealth", "Player died!")
            // Handle heroes death (will be implemented later)
        }
    }
    
    private fun regenerateArmor() {
        if (currentArmor < maxArmor) {
            val oldArmor = currentArmor
            currentArmor = minOf(maxArmor, currentArmor + armorRegenAmount)
            Log.d("PlayerHealth", "Armor regenerated: $oldArmor -> $currentArmor")
        }
    }
    
    fun draw(canvas: Canvas) {
        val startX = uiMargin
        val startY = uiMargin
        
        // Draw health bar
        drawHealthBar(canvas, startX, startY)
        
        // Draw armor bar
        drawArmorBar(canvas, startX, startY + barSpacing)
        
        // Apply flash effect if taking damage
        if (isFlashing) {
            val flashAlpha = (128 * (1f - damageFlashTimer / damageFlashDuration)).toInt()
            val flashPaint = Paint().apply {
                color = Color.argb(flashAlpha, 255, 0, 0)
                style = Paint.Style.FILL
            }
            canvas.drawRect(0f, 0f, canvas.width.toFloat(), canvas.height.toFloat(), flashPaint)
        }
    }
    
    private fun drawHealthBar(canvas: Canvas, x: Float, y: Float) {
        // Label
        canvas.drawText("Health", x, y - 5f, labelPaint)
        
        // Background
        canvas.drawRect(x, y, x + barWidth, y + barHeight, backgroundPaint)
        
        // Health fill
        val healthPercent = currentHealth.toFloat() / maxHealth.toFloat()
        
        // Color based on health percentage
        val healthColor = when {
            healthPercent > 0.6f -> Color.GREEN
            healthPercent > 0.3f -> Color.YELLOW
            else -> Color.RED
        }
        healthPaint.color = healthColor
        
        canvas.drawRect(
            x, y,
            x + barWidth * healthPercent, y + barHeight,
            healthPaint
        )
        
        // Border
        canvas.drawRect(x, y, x + barWidth, y + barHeight, borderPaint)
        
        // Text
        canvas.drawText(
            "$currentHealth/$maxHealth",
            x + 5f,
            y + barHeight - 5f,
            textPaint
        )
    }
    
    private fun drawArmorBar(canvas: Canvas, x: Float, y: Float) {
        // Label
        canvas.drawText("Armor", x, y - 5f, labelPaint)
        
        // Background
        canvas.drawRect(x, y, x + barWidth, y + barHeight, backgroundPaint)
        
        // Armor fill
        val armorPercent = currentArmor.toFloat() / maxArmor.toFloat()
        
        // Armor color with transparency based on amount
        val armorAlpha = (255 * (0.5f + 0.5f * armorPercent)).toInt()
        armorPaint.alpha = armorAlpha
        
        canvas.drawRect(
            x, y,
            x + barWidth * armorPercent, y + barHeight,
            armorPaint
        )
        
        // Border
        canvas.drawRect(x, y, x + barWidth, y + barHeight, borderPaint)
        
        // Text
        canvas.drawText(
            "$currentArmor/$maxArmor",
            x + 5f,
            y + barHeight - 5f,
            textPaint
        )
    }
    
    // Getters
    fun getCurrentHealth(): Int = currentHealth
    fun getMaxHealth(): Int = maxHealth
    fun getCurrentArmor(): Int = currentArmor
    fun getMaxArmor(): Int = maxArmor
    fun isAlive(): Boolean = currentHealth > 0
    fun isDead(): Boolean = currentHealth <= 0
    
    // Setters for testing/cheats
    fun setHealth(health: Int) {
        currentHealth = maxOf(0, minOf(maxHealth, health))
    }
    
    fun setArmor(armor: Int) {
        currentArmor = maxOf(0, minOf(maxArmor, armor))
    }
    
    fun healHealth(amount: Int) {
        currentHealth = minOf(maxHealth, currentHealth + amount)
        Log.d("PlayerHealth", "Healed $amount health, current: $currentHealth")
    }
    
    fun restoreArmor(amount: Int) {
        currentArmor = minOf(maxArmor, currentArmor + amount)
        Log.d("PlayerHealth", "Restored $amount armor, current: $currentArmor")
    }
    
    // Reset for new game
    fun reset() {
        currentHealth = maxHealth
        currentArmor = maxArmor
        isFlashing = false
        damageFlashTimer = 0f
        lastArmorRegenTime = System.currentTimeMillis()
        Log.d("PlayerHealth", "Player health system reset")
    }
    
    // Get health percentage for other systems
    fun getHealthPercentage(): Float = currentHealth.toFloat() / maxHealth.toFloat()
    fun getArmorPercentage(): Float = currentArmor.toFloat() / maxArmor.toFloat()
    
    // Check if heroes should take damage (for invincibility frames)
    fun canTakeDamage(): Boolean {
        val currentTime = System.currentTimeMillis()
        return currentTime - lastDamageTime >= damageCooldown
    }
}