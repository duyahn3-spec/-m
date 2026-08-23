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
import androidx.core.content.ContextCompat

class MainActivity : Activity() {

    private lateinit var toggleButton: Button
    private lateinit var statusText: TextView

    private val projectionLauncher =
        registerForActivityResult(
            ActivityResultContracts.StartActivityForResult()
        ) { result ->

            if (
                result.resultCode != RESULT_OK ||
                result.data == null
            ) {
                setOff("OFF")
                return@registerForActivityResult
            }

            startCapture(result.data!!)
        }

    private val bluetoothPermissionLauncher =
        registerForActivityResult(
            ActivityResultContracts.RequestMultiplePermissions()
        ) {
            requestProjection()
        }

    override fun onCreate(
        savedInstanceState: Bundle?
    ) {
        super.onCreate(savedInstanceState)

        createInterface()
    }

    private fun createInterface() {

        statusText = TextView(this).apply {
            text = "OFF"
            textSize = 18f
            setPadding(
                0,
                0,
                0,
                24
            )
        }

        toggleButton = Button(this).apply {

            text = "OFF"
            textSize = 22f

            setOnClickListener {

                if (text == "OFF") {
                    turnOn()
                } else {
                    turnOff()
                }
            }
        }

        val root =
            LinearLayout(this).apply {

                orientation =
                    LinearLayout.VERTICAL

                setPadding(
                    40,
                    80,
                    40,
                    40
                )

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

        setContentView(root)
    }

    private fun turnOn() {

        if (
            Build.VERSION.SDK_INT >=
            Build.VERSION_CODES.S
        ) {

            val permissions =
                ArrayList<String>()

            if (
                ContextCompat.checkSelfPermission(
                    this,
                    Manifest.permission.BLUETOOTH_SCAN
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                permissions.add(
                    Manifest.permission.BLUETOOTH_SCAN
                )
            }

            if (
                ContextCompat.checkSelfPermission(
                    this,
                    Manifest.permission.BLUETOOTH_CONNECT
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                permissions.add(
                    Manifest.permission.BLUETOOTH_CONNECT
                )
            }

            if (permissions.isNotEmpty()) {

                bluetoothPermissionLauncher.launch(
                    permissions.toTypedArray()
                )

                return
            }
        }

        requestProjection()
    }

    private fun requestProjection() {

        val manager =
            getSystemService(
                MEDIA_PROJECTION_SERVICE
            ) as MediaProjectionManager

        projectionLauncher.launch(
            manager.createScreenCaptureIntent()
        )
    }

    private fun startCapture(
        data: Intent
    ) {

        val serviceIntent =
            Intent(
                this,
                ScreenCaptureService::class.java
            ).apply {

                putExtra(
                    ScreenCaptureService.EXTRA_RESULT_CODE,
                    RESULT_OK
                )

                putExtra(
                    ScreenCaptureService.EXTRA_DATA,
                    data
                )
            }

        ContextCompat.startForegroundService(
            this,
            serviceIntent
        )

        toggleButton.text = "ON"
        statusText.text =
            "ON • SCREEN ANALYSIS"
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

    private fun setOff(
        text: String
    ) {

        toggleButton.text = "OFF"
        statusText.text = text
    }
}
