package com.screenai

import android.content.Context
import android.graphics.PixelFormat
import android.hardware.display.DisplayManager
import android.hardware.display.VirtualDisplay
import android.media.Image
import android.media.ImageReader
import android.media.projection.MediaProjection
import android.os.Handler
import android.os.HandlerThread
import android.view.WindowManager

class ScreenCaptureEngine(
    private val context: Context,
    private val projection: MediaProjection
) {

    private var display:
        VirtualDisplay? = null

    private var reader:
        ImageReader? = null

    private var thread:
        HandlerThread? = null

    private var handler:
        Handler? = null

    private var detector:
        TFLiteDetector? = null

    private var tracker:
        TargetTracker? = null

    private var ble:
        BleManager? = null

    @Volatile
    private var running = false

    private val projectionCallback =
        object : MediaProjection.Callback() {

            override fun onStop() {
                stop()
            }
        }

    fun start() {

        if (running) {
            return
        }

        running = true

        projection.registerCallback(
            projectionCallback,
            Handler(
                context.mainLooper
            )
        )

        thread =
            HandlerThread(
                "ScreenAI"
            ).also {
                it.start()
            }

        handler =
            Handler(
                thread!!.looper
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

        startProjection()
    }

    private fun startProjection() {

        val metrics =
            context.resources.displayMetrics

        val screenWidth =
            metrics.widthPixels

        val screenHeight =
            metrics.heightPixels

        val density =
            metrics.densityDpi

        /*
         * ImageReader chỉ giữ 2 ảnh.
         *
         * Khi frame mới đến,
         * acquireLatestImage() bỏ frame cũ
         * để không tạo backlog.
         */

        reader =
            ImageReader.newInstance(
                screenWidth,
                screenHeight,
                PixelFormat.RGBA_8888,
                2
            )

        reader?.setOnImageAvailableListener(
            { imageReader ->

                if (!running) {
                    return@setOnImageAvailableListener
                }

                /*
                 * LẤY FRAME MỚI NHẤT.
                 */

                val image =
                    imageReader.acquireLatestImage()
                        ?: return@setOnImageAvailableListener

                try {

                    processImage(
                        image
                    )

                } catch (
                    _: Throwable
                ) {

                    /*
                     * Không để một frame lỗi
                     * làm chết toàn bộ service.
                     */

                } finally {

                    image.close()
                }

            },
            handler
        )

        display =
            projection.createVirtualDisplay(
                "ScreenAI",
                screenWidth,
                screenHeight,
                density,

                DisplayManager
                    .VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR,

                reader!!.surface,

                null,
                handler
            )
    }

    private fun processImage(
        image: Image
    ) {

        val detection =
            detector?.detect(
                image
            ) ?: return

        val target =
            tracker?.update(
                detection
            ) ?: return

        ble?.sendTelemetry(
            target
        )
    }

    fun stop() {

        if (!running) {
            return
        }

        running = false

        try {

            projection.unregisterCallback(
                projectionCallback
            )

        } catch (
            _: Throwable
        ) {
        }

        display?.release()
        display = null

        reader?.close()
        reader = null

        detector?.close()
        detector = null

        ble?.stop()
        ble = null

        thread?.quitSafely()

        thread = null
        handler = null

        try {

            projection.stop()

        } catch (
            _: Throwable
        ) {
        }
    }
}
