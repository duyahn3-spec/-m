package com.screenai

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.media.projection.MediaProjection
import android.media.projection.MediaProjectionManager
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat

class ScreenCaptureService : Service() {

    companion object {

        const val EXTRA_RESULT_CODE =
            "screen_capture_result_code"

        const val EXTRA_DATA =
            "screen_capture_data"

        private const val CHANNEL_ID =
            "screen_ai_capture"

        private const val NOTIFICATION_ID = 1001
    }

    private var engine: ScreenCaptureEngine? = null

    override fun onCreate() {
        super.onCreate()

        createNotificationChannel()

        startForeground(
            NOTIFICATION_ID,
            createNotification()
        )
    }

    override fun onStartCommand(
        intent: Intent?,
        flags: Int,
        startId: Int
    ): Int {

        if (intent == null) {
            stopSelf()
            return START_NOT_STICKY
        }

        val resultCode =
            intent.getIntExtra(
                EXTRA_RESULT_CODE,
                -1
            )

        val data =
            if (Build.VERSION.SDK_INT >= 33) {
                intent.getParcelableExtra(
                    EXTRA_DATA,
                    Intent::class.java
                )
            } else {
                @Suppress("DEPRECATION")
                intent.getParcelableExtra(EXTRA_DATA)
            }

        if (resultCode < 0 || data == null) {
            stopSelf()
            return START_NOT_STICKY
        }

        if (engine == null) {

            val manager =
                getSystemService(
                    MEDIA_PROJECTION_SERVICE
                ) as MediaProjectionManager

            val projection =
                manager.getMediaProjection(
                    resultCode,
                    data
                )

            if (projection == null) {
                stopSelf()
                return START_NOT_STICKY
            }

            engine = ScreenCaptureEngine(
                context = this,
                projection = projection
            )

            engine?.start()
        }

        return START_STICKY
    }

    override fun onDestroy() {

        engine?.stop()
        engine = null

        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }

    private fun createNotificationChannel() {

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {

            val channel =
                NotificationChannel(
                    CHANNEL_ID,
                    "Screen AI",
                    NotificationManager.IMPORTANCE_LOW
                )

            val manager =
                getSystemService(
                    Context.NOTIFICATION_SERVICE
                ) as NotificationManager

            manager.createNotificationChannel(channel)
        }
    }

    private fun createNotification(): Notification {

        return NotificationCompat.Builder(
            this,
            CHANNEL_ID
        )
            .setContentTitle("Screen AI")
            .setContentText(
                "Screen analysis is running"
            )
            .setSmallIcon(
                android.R.drawable.ic_menu_view
            )
            .setOngoing(true)
            .build()
    }
}
