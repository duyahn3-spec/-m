package com.screenai

data class Telemetry(
    val x: Int,
    val y: Int,
    val dx: Int,
    val dy: Int,
    val confidence: Float,
    val timestamp: Long
) {

    fun encode(): String {
        return "T,$x,$y,$dx,$dy,${"%.3f".format(confidence)},$timestamp\n"
    }

    companion object {

        fun decode(value: String): Telemetry? {
            val p = value.trim().split(",")

            if (p.size != 7 || p[0] != "T") {
                return null
            }

            return try {
                Telemetry(
                    x = p[1].toInt(),
                    y = p[2].toInt(),
                    dx = p[3].toInt(),
                    dy = p[4].toInt(),
                    confidence = p[5].toFloat(),
                    timestamp = p[6].toLong()
                )
            } catch (_: Exception) {
                null
            }
        }
    }
}
