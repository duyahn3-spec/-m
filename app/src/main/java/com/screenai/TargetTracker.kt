package com.screenai

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

    private var x = 0f
    private var y = 0f

    private var initialized = false

    fun update(
        detection: Detection
    ): TrackedTarget {

        if (!initialized) {

            x = detection.x
            y = detection.y

            initialized = true

            return TrackedTarget(
                x,
                y,
                0f,
                0f,
                detection.confidence
            )
        }

        val oldX = x
        val oldY = y

        val alpha = 0.70f

        x +=
            (detection.x - x) *
                    alpha

        y +=
            (detection.y - y) *
                    alpha

        return TrackedTarget(
            x = x,
            y = y,
            dx = x - oldX,
            dy = y - oldY,
            confidence =
                detection.confidence
        )
    }
}
