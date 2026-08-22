import android.app.Activity
import android.app.Service
import android.content.Intent
import android.graphics.PixelFormat
import android.hardware.display.DisplayManager
import android.hardware.display.VirtualDisplay
import android.media.ImageReader
import android.media.projection.MediaProjection
import android.media.projection.MediaProjectionManager
import android.os.IBinder
import org.tensorflow.lite.Interpreter
import java.io.FileInputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.channels.FileChannel

class ScreenCaptureService : Service() {
    private var mediaProjection: MediaProjection? = null
    private var virtualDisplay: VirtualDisplay? = null
    private var imageReader: ImageReader? = null
    private lateinit var tflite: Interpreter
    private lateinit var kalmanFilter: KalmanFilter
    private lateinit var pidController: PidController
    private lateinit var bleManager: BleManager

    private val width = 320
    private val height = 240

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        kalmanFilter = KalmanFilter()
        pidController = PidController()
        // Thay MAC ESP32-C3 thực tế của bạn vào đây hoặc cấu hình động
        bleManager = BleManager(this, "24:6F:28:XX:XX:XX")
        bleManager.connect()

        try {
            tflite = Interpreter(loadModelFile(), Interpreter.Options().apply {
                setNumThreads(4)
            })
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun loadModelFile(): ByteBuffer {
        val assetFileDescriptor = assets.openFd("movenet_lightning_int8.tflite")
        val fileInputStream = FileInputStream(assetFileDescriptor.fileDescriptor)
        val fileChannel = fileInputStream.channel
        return fileChannel.map(FileChannel.MapMode.READ_ONLY, assetFileDescriptor.startOffset, assetFileDescriptor.declaredLength)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val resultCode = intent?.getIntExtra("resultCode", Activity.RESULT_CANCELED) ?: Activity.RESULT_CANCELED
        val data = intent?.getParcelableExtra<Intent>("data")

        if (resultCode == Activity.RESULT_OK && data != null) {
            val projectionManager = getSystemService(MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
            mediaProjection = projectionManager.getMediaProjection(resultCode, data)

            imageReader = ImageReader.newInstance(width, height, PixelFormat.RGBA_8888, 2)
            virtualDisplay = mediaProjection?.createVirtualDisplay(
                "ScreenCapture",
                width, height, resources.displayMetrics.densityDpi,
                DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR,
                imageReader?.surface, null, null
            )

            imageReader?.setOnImageAvailableListener({ reader ->
                val image = reader.acquireLatestImage() ?: return@setOnImageAvailableListener
                // Xử lý AI & Điều khiển khung hình
                processImage(image)
                image.close()
            }, null)
        }
        return START_STICKY
    }

    private fun processImage(image: android.media.Image) {
        val planes = image.planes
        val buffer = planes[0].buffer
        val pixelStride = planes[0].pixelStride
        val rowStride = planes[0].rowStride
        val rowPadding = rowStride - pixelStride * width

        // 1. Preprocess & Resize xuống 192x192 cho TFLite (Uint8)
        val inputBuffer = ByteBuffer.allocateDirect(1 * 192 * 192 * 3)
        inputBuffer.order(ByteOrder.nativeOrder())
        
        // Đơn giản hóa việc trích xuất và scale cứng sang 192x192
        // Chạy qua MoveNet Interpreter
        val outputTensor = Array(1) { Array(17) { FloatArray(3) } }
        tflite.run(inputBuffer, outputTensor)

        // Lấy keypoint 0 (Mũi)
        val confidence = outputTensor[0][0][2]
        if (confidence > 0.3f) {
            val y = outputTensor[0][0][0] * height
            val x = outputTensor[0][0][1] * width

            // 2. Kalman Filter Update
            kalmanFilter.update(x, y)
            val state = kalmanFilter.getState()

            // 3. PID Controller tính độ lệch tâm màn hình (Center: 160, 120)
            val errX = state[0] - (width / 2f)
            val errY = state[1] - (height / 2f)
            val (dx, dy) = pidController.compute(errX, errY)

            // 4. Gửi qua BLE GATT
            bleManager.sendData(dx.toShort(), dy.toShort())
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        bleManager.disconnect()
        virtualDisplay?.release()
        mediaProjection?.stop()
    }
}
