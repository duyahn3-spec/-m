class KalmanFilter(dt: Float = 0.01f, q: Float = 0.4f, r: Float = 1.5f) {
    // State: [x, y, vx, vy]
    private var x = FloatArray(4)
    private var P = Array(4) { FloatArray(4) { 1f } }
    
    private val dt = dt
    private val Q_val = q
    private val R_val = r

    fun update(measurementX: Float, measurementY: Float) {
        // Simple 2D Kalman filter update step for position & velocity
        val predX = x[0] + x[2] * dt
        val predY = x[1] + x[3] * dt
        
        val innovationX = measurementX - predX
        val innovationY = measurementY - predY
        
        x[0] = predX + 0.5f * innovationX
        x[1] = predY + 0.5f * innovationY
        x[2] = x[2] + (innovationX / dt) * 0.1f
        x[3] = x[3] + (innovationY / dt) * 0.1f
    }

    fun getState(): FloatArray {
        return x
    }
}
