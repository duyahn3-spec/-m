package com.screenai

import android.app.Activity
import android.content.Intent
import android.media.projection.MediaProjectionManager
import android.os.Bundle
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView

class MainActivity : Activity() {

    companion object {
        private const val REQUEST_MEDIA_PROJECTION = 1001
    }

    private lateinit var statusText: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val layout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(32, 32, 32, 32)
        }

        statusText = TextView(this).apply {
            text = "Screen AI\n\nSẵn sàng"
            textSize = 18f
        }

        val captureButton = Button(this).apply {
            text = "Bắt đầu Screen Capture"
            setOnClickListener {
                requestScreenCapture()
            }
        }

        layout.addView(statusText)
        layout.addView(captureButton)

        setContentView(layout)
    }

    private fun requestScreenCapture() {
        val manager =
            getSystemService(MEDIA_PROJECTION_SERVICE) as MediaProjectionManager

        val intent = manager.createScreenCaptureIntent()

        startActivityForResult(
            intent,
            REQUEST_MEDIA_PROJECTION
        )
    }

    @Deprecated("Deprecated in Android API")
    override fun onActivityResult(
        requestCode: Int,
        resultCode: Int,
        data: Intent?
    ) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode != REQUEST_MEDIA_PROJECTION) {
            return
        }

        if (resultCode == RESULT_OK && data != null) {
            statusText.text =
                "Screen Capture permission: OK\n\n" +
                "Bước tiếp theo: ImageReader"
        } else {
            statusText.text =
                "Screen Capture permission bị từ chối."
        }
    }
}
