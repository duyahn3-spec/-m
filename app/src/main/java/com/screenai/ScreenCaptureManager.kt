package com.screenai

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.graphics.PixelFormat
import android.hardware.display.DisplayManager
import android.hardware.display.VirtualDisplay
import android.media.ImageReader
import android.media.projection.MediaProjection
import android.media.projection.MediaProjectionManager
import android.os.Handler
import android.os.Looper
import android.util.DisplayMetrics

class ScreenCaptureManager(
    private val activity: Activity
) {

    companion object {
        const val REQUEST_CAPTURE = 9001
    }

    interface Listener {
        fun onFrame(
            reader: ImageReader
        )
    }

    var listener: Listener? = null

    private var projection: MediaProjection? = null
    private var virtualDisplay: VirtualDisplay? = null
    private var reader: ImageReader? = null

    private val handler =
        Handler(Looper.getMainLooper())

    fun requestPermission() {

        val manager =
            activity.getSystemService(
                Context.MEDIA_PROJECTION_SERVICE
            ) as MediaProjectionManager

        activity.startActivityForResult(
            manager.createScreenCaptureIntent(),
            REQUEST_CAPTURE
        )
    }

    fun start(
        resultCode: Int,
        data: Intent
    ) {

        val manager =
            activity.getSystemService(
                Context.MEDIA_PROJECTION_SERVICE
            ) as MediaProjectionManager

        projection =
            manager.getMediaProjection(
                resultCode,
                data
            )

        val metrics =
            DisplayMetrics()

        @Suppress("DEPRECATION")
        activity.windowManager
            .defaultDisplay
            .getRealMetrics(metrics)

        val width =
            metrics.widthPixels

        val height =
            metrics.heightPixels

        val density =
            metrics.densityDpi

        reader =
            ImageReader.newInstance(
                width,
                height,
                PixelFormat.RGBA_8888,
                2
            )

        reader?.setOnImageAvailableListener(
            { imageReader ->

                listener?.onFrame(
                    imageReader
                )

            },
            handler
        )

        virtualDisplay =
            projection?.createVirtualDisplay(
                "ScreenAI",
                width,
                height,
                density,
                DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR,
                reader!!.surface,
                null,
                handler
            )
    }

    fun stop() {

        reader?.setOnImageAvailableListener(
            null,
            null
        )

        reader?.close()
        reader = null

        virtualDisplay?.release()
        virtualDisplay = null

        projection?.stop()
        projection = null
    }
}
