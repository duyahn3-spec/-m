package com.screenai

import kotlin.math.abs

data class Detection(
    val x: Float,
    val y: Float,
    val width: Float,
    val height: Float,
    val confidence: Float
)

data class TrackedTarget(
    val x: Float,
    val y: Float,
    val dx: Float,
    val dy: Float,
    val confidence: Float
)

class TargetTracker {

    private var previousX = 0f
    private var previousY = 0f

    private var initialized = false

    fun update(
        detection: Detection
    ): TrackedTarget {

        if (!initialized) {

            previousX = detection.x
            previousY = detection.y

            initialized = true

            return TrackedTarget(
                x = detection.x,
                y = detection.y,
                dx = 0f,
                dy = 0f,
                confidence = detection.confidence
            )
        }

        val rawDx =
            detection.x - previousX

        val rawDy =
            detection.y - previousY

        /*
         * Lightweight smoothing.
         */

        val alpha = 0.65f

        val newX =
            previousX +
                    rawDx * alpha

        val newY =
            previousY +
                    rawDy * alpha

        val dx =
            newX - previousX

        val dy =
            newY - previousY

        previousX = newX
        previousY = newY

        return TrackedTarget(
            x = newX,
            y = newY,
            dx = dx,
            dy = dy,
            confidence = detection.confidence
        )
    }
}
