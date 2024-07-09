package com.darblee.flingaid.ui

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.darblee.flingaid.Direction
import com.darblee.flingaid.utilities.mapInRange
import com.darblee.flingaid.utilities.randomBoolean
import com.darblee.flingaid.utilities.randomInRange
import com.darblee.flingaid.utilities.toPx
import kotlin.math.pow

class Particle(
    val color: Color,
    val startRow: Int,
    val startCol: Int,
    val direction: Direction,
    val maxHorizontalDisplacement: Float,
    val maxVerticalDisplacement: Float
) {
    private val velocity = 4 * maxVerticalDisplacement
    private val acceleration = -2 * velocity
    var currentXPosition = 0f
    var currentYPosition = 0f

    private var visibilityThresholdLow = randomInRange(0f, 0.05f)
    private var visibilityThresholdHigh = randomInRange(0f, 0.1f)

    private val initialXDisplacement = 10.dp.toPx() * randomInRange(-1f, 1f)
    private val initialYDisplacement = 10.dp.toPx() * randomInRange(-1f, 1f)

    var alpha = 0f
    var currentRadius = 0f
    private val startRadius = 1.dp.toPx()
    private val endRadius = if (randomBoolean(trueProbabilityPercentage = 20)) {
        randomInRange(startRadius, 2.dp.toPx())
    } else {
        randomInRange(1.dp.toPx(), startRadius)
    }

    fun updateProgress(explosionProgress: Float, gridSize: Float) {
        val trajectoryProgress =
            if (explosionProgress < visibilityThresholdLow || (explosionProgress > (1 - visibilityThresholdHigh))) {
                alpha = 0f; return
            } else (explosionProgress - visibilityThresholdLow).mapInRange(0f,1f - visibilityThresholdHigh - visibilityThresholdLow,0f, 1f)
        alpha = if (trajectoryProgress < 0.7f) 1f else (trajectoryProgress - 0.7f).mapInRange(
            0f,
            0.3f,
            1f,
            0f
        )
        currentRadius = startRadius + (endRadius - startRadius) * trajectoryProgress
        val currentTime = trajectoryProgress.mapInRange(0f, 1f, 0f, 1.4f)
        val verticalDisplacement =
            (currentTime * velocity + 0.5 * acceleration * currentTime.toDouble()
                .pow(2.0)).toFloat()

        var startY = 0f
        var startX = 0f
        var trajectoryX = 0f
        var trajectoryY = 0f

        when (direction) {
            Direction.RIGHT -> {
                startY = (startRow * gridSize + (gridSize / 2))
                startX = startCol * gridSize
                trajectoryY = trajectoryProgress
            }
            Direction.LEFT -> {
                startY = (startRow * gridSize + (gridSize / 2))
                startX = startCol * gridSize + gridSize
                trajectoryY = -trajectoryProgress
            }
            Direction.UP -> {
                startY = startRow * gridSize + gridSize
                startX = startCol * gridSize + (gridSize / 2)
                trajectoryX = trajectoryProgress
            }
            Direction.DOWN -> {
                startY = startRow * gridSize
                startX = (startCol * gridSize + (gridSize / 2))
                trajectoryX = -trajectoryProgress
            }
            else -> assert(true) { "Got unexpected direction in particle calculation"}
        }

        currentYPosition = startY + initialXDisplacement - (verticalDisplacement * trajectoryY)
        currentXPosition = startX + initialYDisplacement + (maxHorizontalDisplacement * trajectoryX)
    }
}