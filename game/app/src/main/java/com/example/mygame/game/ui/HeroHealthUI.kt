package com.example.mygame.game.ui

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import com.example.mygame.game.managers.HeroManager

/**
 * HeroHealthUI - Displays hero's health and armor bars in the top-left corner
 */
class HeroHealthUI {
    
    // UI positioning
    private val marginLeft = 20f
    private val marginTop = 20f
    private val barWidth = 400f  // Tăng từ 200f lên 400f (gấp đôi)
    private val barHeight = 32f  // Tăng từ 16f lên 32f (gấp đôi)
    private val barSpacing = 16f // Tăng từ 8f lên 16f (gấp đôi)
    
    // Paint objects for rendering
    private val backgroundPaint = Paint().apply {
        style = Paint.Style.FILL
        isAntiAlias = true
    }
    
    private val fillPaint = Paint().apply {
        style = Paint.Style.FILL
        isAntiAlias = true
    }
    
    private val borderPaint = Paint().apply {
        style = Paint.Style.STROKE
        strokeWidth = 2f
        color = Color.WHITE
        isAntiAlias = true
    }
    
    private val textPaint = Paint().apply {
        style = Paint.Style.FILL
        color = Color.WHITE
        textSize = 24f  // Tăng từ 12f lên 24f (gấp đôi)
        isAntiAlias = true
    }
    
    /**
     * Draw the health and armor UI
     */
    fun draw(canvas: Canvas, heroManager: HeroManager) {
        val currentHealth = heroManager.getCurrentHealth()
        val maxHealth = heroManager.getMaxHealth()
        val currentArmor = heroManager.getCurrentArmor()
        val maxArmor = heroManager.getMaxArmor()
        
        // Draw health bar
        drawHealthBar(canvas, currentHealth, maxHealth, marginLeft, marginTop)
        
        // Draw armor bar below health bar
        val armorY = marginTop + barHeight + barSpacing + 30f // Tăng từ 15f lên 30f cho text space
        drawArmorBar(canvas, currentArmor, maxArmor, marginLeft, armorY)
    }
    
    private fun drawHealthBar(canvas: Canvas, currentHealth: Float, maxHealth: Float, x: Float, y: Float) {
        val healthPercentage = if (maxHealth > 0) currentHealth / maxHealth else 0f
        
        // Background (dark red)
        backgroundPaint.color = Color.argb(180, 60, 20, 20)
        canvas.drawRoundRect(x, y, x + barWidth, y + barHeight, 16f, 16f, backgroundPaint) // Tăng corner radius từ 8f lên 16f
        
        // Health fill (green to red gradient based on percentage)
        val red = ((1f - healthPercentage) * 255).toInt().coerceIn(0, 255)
        val green = (healthPercentage * 255).toInt().coerceIn(0, 255)
        fillPaint.color = Color.argb(255, red, green, 0)
        
        val fillWidth = barWidth * healthPercentage
        if (fillWidth > 0) {
            canvas.drawRoundRect(x, y, x + fillWidth, y + barHeight, 16f, 16f, fillPaint) // Tăng corner radius từ 8f lên 16f
        }
        
        // Border
        canvas.drawRoundRect(x, y, x + barWidth, y + barHeight, 16f, 16f, borderPaint) // Tăng corner radius từ 8f lên 16f
        
        // Text - Health values
        val healthText = "${currentHealth.toInt()}/${maxHealth.toInt()}"
        val textX = x + 16f // Tăng từ 8f lên 16f
        val textY = y + barHeight + 24f // Tăng từ 12f lên 24f
        canvas.drawText("HP: $healthText", textX, textY, textPaint)
    }
    
    private fun drawArmorBar(canvas: Canvas, currentArmor: Float, maxArmor: Float, x: Float, y: Float) {
        val armorPercentage = if (maxArmor > 0) currentArmor / maxArmor else 0f
        
        // Background (dark blue)
        backgroundPaint.color = Color.argb(180, 20, 30, 60)
        canvas.drawRoundRect(x, y, x + barWidth, y + barHeight, 16f, 16f, backgroundPaint) // Tăng corner radius từ 8f lên 16f
        
        // Armor fill (blue gradient)
        val blue = (armorPercentage * 255).toInt().coerceIn(0, 255)
        val lightBlue = (armorPercentage * 150 + 100).toInt().coerceIn(0, 255)
        fillPaint.color = Color.argb(255, 50, lightBlue, blue)
        
        val fillWidth = barWidth * armorPercentage
        if (fillWidth > 0) {
            canvas.drawRoundRect(x, y, x + fillWidth, y + barHeight, 16f, 16f, fillPaint) // Tăng corner radius từ 8f lên 16f
        }
        
        // Border
        canvas.drawRoundRect(x, y, x + barWidth, y + barHeight, 16f, 16f, borderPaint) // Tăng corner radius từ 8f lên 16f
        
        // Text - Armor values
        val armorText = "${currentArmor.toInt()}/${maxArmor.toInt()}"
        val textX = x + 16f // Tăng từ 8f lên 16f
        val textY = y + barHeight + 24f // Tăng từ 12f lên 24f
        canvas.drawText("Armor: $armorText", textX, textY, textPaint)
    }
    
    /**
     * Get the total height of the UI component
     */
    fun getTotalHeight(): Float {
        return marginTop + (barHeight + barSpacing + 30f) * 2 + 30f // Tăng từ 15f lên 30f cho text spaces
    }
    
    /**
     * Get the total width of the UI component
     */
    fun getTotalWidth(): Float {
        return marginLeft + barWidth + 40f // Tăng từ 20f lên 40f cho extra margin
    }
}