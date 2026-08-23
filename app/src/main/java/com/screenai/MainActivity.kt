package com.screenai

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.media.projection.MediaProjectionManager
import android.os.Build
import android.os.Bundle
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat

class MainActivity : AppCompatActivity() {

    private lateinit var toggleButton: Button
    private lateinit var statusText: TextView

    private val projectionLauncher =
        registerForActivityResult(
            ActivityResultContracts.StartActivityForResult()
        ) { result ->

            if (result.resultCode != Activity.RESULT_OK || result.data == null) {
                setOff("Screen capture permission denied")
                return@registerForActivityResult
            }

            startCapture(result.data!!)
        }

    private val permissionLauncher =
        registerForActivityResult(
            ActivityResultContracts.RequestMultiplePermissions()
        ) {
            requestProjection()
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        createUi()
    }

    private fun createUi() {

        statusText = TextView(this).apply {
            text = "OFF"
            textSize = 18f
            setPadding(32, 32, 32, 16)
        }

        toggleButton = Button(this).apply {
            text = "OFF"
            textSize = 20f

            setOnClickListener {

                if (text.toString() == "OFF") {
                    turnOn()
                } else {
                    turnOff()
                }
            }
        }

        val layout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(32, 64, 32, 32)

            addView(
                statusText,
                LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                )
            )

            addView(
                toggleButton,
                LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    100
                )
            )
        }

        setContentView(layout)
    }

    private fun turnOn() {

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {

            val permissions = mutableListOf<String>()

            if (
                ContextCompat.checkSelfPermission(
                    this,
                    Manifest.permission.BLUETOOTH_SCAN
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                permissions.add(Manifest.permission.BLUETOOTH_SCAN)
            }

            if (
                ContextCompat.checkSelfPermission(
                    this,
                    Manifest.permission.BLUETOOTH_CONNECT
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                permissions.add(Manifest.permission.BLUETOOTH_CONNECT)
            }

            if (permissions.isNotEmpty()) {
                permissionLauncher.launch(permissions.toTypedArray())
                return
            }
        }

        requestProjection()
    }

    private fun requestProjection() {

        val manager =
            getSystemService(MEDIA_PROJECTION_SERVICE)
                    as MediaProjectionManager

        projectionLauncher.launch(
            manager.createScreenCaptureIntent()
        )
    }

    private fun startCapture(data: Intent) {

        val serviceIntent = Intent(
            this,
            ScreenCaptureService::class.java
        ).apply {
            putExtra(ScreenCaptureService.EXTRA_RESULT_CODE, RESULT_OK)
            putExtra(ScreenCaptureService.EXTRA_DATA, data)
        }

        ContextCompat.startForegroundService(
            this,
            serviceIntent
        )

        toggleButton.text = "ON"
        statusText.text = "ON • MediaProjection"
    }

    private fun turnOff() {

        stopService(
            Intent(
                this,
                ScreenCaptureService::class.java
            )
        )

        setOff("OFF")
    }

    private fun setOff(message: String) {
        toggleButton.text = "OFF"
        statusText.text = message
    }

    override fun onDestroy() {

        super.onDestroy()
    }
}
