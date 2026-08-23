package com.screenai

import android.content.Context
import android.graphics.PixelFormat
import android.hardware.display.DisplayManager
import android.hardware.display.VirtualDisplay
import android.media.ImageReader
import android.media.projection.MediaProjection
import android.os.Handler
import android.os.HandlerThread
import android.view.WindowManager

class ScreenCaptureEngine(
    private val context: Context,
    private val projection: MediaProjection
) {

    private var virtualDisplay: VirtualDisplay? = null
    private var imageReader: ImageReader? = null

    private var workerThread: HandlerThread? = null
    private var workerHandler: Handler? = null

    private var detector: TFLiteDetector? = null
    private var tracker: TargetTracker? = null
    private var ble: BleManager? = null

    @Volatile
    private var running = false

    private val projectionCallback =
        object : MediaProjection.Callback() {

            override fun onStop() {
                stop()
            }
        }

    fun start() {

        if (running) return

        running = true

        projection.registerCallback(
            projectionCallback,
            Handler(
                context.mainLooper
            )
        )

        workerThread =
            HandlerThread(
                "ScreenAI-Capture"
            ).also {
                it.start()
            }

        workerHandler =
            Handler(
                workerThread!!.looper
            )

        detector =
            TFLiteDetector(
                context
            )

        tracker =
            TargetTracker()

        ble =
            BleManager(
                context
            )

        ble?.start()

        startCapture()
    }

    private fun startCapture() {

        val wm =
            context.getSystemService(
                Context.WINDOW_SERVICE
            ) as WindowManager

        val metrics =
            context.resources.displayMetrics

        val width =
            metrics.widthPixels

        val height =
            metrics.heightPixels

        val density =
            metrics.densityDpi

        /*
         * Không encode video.
         * ImageReader chỉ giữ tối đa 2 frame.
         */

        imageReader =
            ImageReader.newInstance(
                width,
                height,
                PixelFormat.RGBA_8888,
                2
            )

        imageReader?.setOnImageAvailableListener(
            { reader ->

                if (!running) return@setOnImageAvailableListener

                /*
                 * acquireLatestImage:
                 * luôn ưu tiên frame mới nhất,
                 * tránh backlog và latency tăng dần.
                 */

                val image =
                    reader.acquireLatestImage()
                        ?: return@setOnImageAvailableListener

                workerHandler?.post {

                    try {

                        processFrame(image)

                    } finally {

                        image.close()
                    }
                }

            },
            workerHandler
        )

        virtualDisplay =
            projection.createVirtualDisplay(
                "ScreenAI",
                width,
                height,
                density,
                DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR,
                imageReader!!.surface,
                null,
                workerHandler
            )
    }

    private fun processFrame(
        image: android.media.Image
    ) {

        val result =
            detector?.detect(
                image
            )

        if (result == null) {
            return
        }

        val tracked =
            tracker?.update(
                result
            ) ?: return

        ble?.sendTelemetry(
            tracked
        )
    }

    fun stop() {

        if (!running) return

        running = false

        try {
            projection.unregisterCallback(
                projectionCallback
            )
        } catch (_: Exception) {
        }

        virtualDisplay?.release()
        virtualDisplay = null

        imageReader?.close()
        imageReader = null

        detector?.close()
        detector = null

        ble?.stop()
        ble = null

        workerThread?.quitSafely()

        workerThread = null
        workerHandler = null

        projection.stop()
    }
}
