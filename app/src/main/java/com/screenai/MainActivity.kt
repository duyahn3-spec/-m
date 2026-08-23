package com.screenai

import android.Manifest
import android.app.Activity
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothManager
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.media.ImageReader
import android.os.Build
import android.os.Bundle
import android.view.Gravity
import android.widget.Button
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat

class MainActivity :
    Activity(),
    ScreenCaptureManager.Listener,
    BleManager.Listener {

    private lateinit var capture:
        ScreenCaptureManager

    private lateinit var ble:
        BleManager

    private lateinit var engine:
        TFLiteEngine

    private lateinit var status:
        TextView

    private var frameCount = 0L

    private var lastFpsTime = 0L

    private var fps = 0

    override fun onCreate(
        savedInstanceState: Bundle?
    ) {

        super.onCreate(savedInstanceState)

        engine = TFLiteEngine(this)

        capture =
            ScreenCaptureManager(this)

        capture.listener = this

        ble =
            BleManager(this)

        ble.listener = this

        requestBlePermissions()

        buildUi()
    }

    private fun buildUi() {

        val root =
            LinearLayout(this).apply {

                orientation =
                    LinearLayout.VERTICAL

                setPadding(
                    24,
                    24,
                    24,
                    24
                )

                setBackgroundColor(
                    Color.BLACK
                )
            }

        val title =
            TextView(this).apply {

                text =
                    "Screen AI Research"

                textSize = 24f

                gravity =
                    Gravity.CENTER

                setTextColor(
                    Color.WHITE
                )
            }

        val inspect =
            Button(this).apply {

                text =
                    "1. Kiểm tra TFLite"

                setOnClickListener {

                    status.text =
                        engine.describe()
                }
            }

        val captureButton =
            Button(this).apply {

                text =
                    "2. Screen Capture"

                setOnClickListener {

                    capture.requestPermission()
                }
            }

        val scan =
            Button(this).apply {

                text =
                    "3. Tìm ESP32 BLE"

                setOnClickListener {

                    scanForEsp32()
                }
            }

        val send =
            Button(this).apply {

                text =
                    "4. Gửi telemetry test"

                setOnClickListener {

                    val telemetry =
                        Telemetry(
                            x = 120,
                            y = 80,
                            dx = -3,
                            dy = 2,
                            confidence = 0.95f,
                            timestamp =
                                System.currentTimeMillis()
                        )

                    ble.send(
                        telemetry.encode()
                    )
                }
            }

        status =
            TextView(this).apply {

                textSize = 14f

                setTextColor(
                    Color.WHITE
                )

                text =
                    "READY\n"
            }

        root.addView(title)
        root.addView(inspect)
        root.addView(captureButton)
        root.addView(scan)
        root.addView(send)

        val scroll =
            ScrollView(this)

        scroll.addView(status)

        root.addView(
            scroll,
            LinearLayout.LayoutParams(
                -1,
                0,
                1f
            )
        )

        setContentView(root)
    }

    override fun onFrame(
        reader: ImageReader
    ) {

        val image =
            reader.acquireLatestImage()
                ?: return

        try {

            frameCount++

            val now =
                System.currentTimeMillis()

            if (
                lastFpsTime == 0L
            ) {
                lastFpsTime = now
            }

            if (
                now - lastFpsTime >= 1000
            ) {

                fps =
                    frameCount.toInt()

                frameCount = 0

                lastFpsTime = now
            }

            runOnUiThread {

                status.text =
                    "SCREEN CAPTURE\n" +
                    "FPS: $fps\n" +
                    "Image: " +
                    "${image.width}×${image.height}\n" +
                    "TFLite input: " +
                    "${engine.inputWidth}×" +
                    "${engine.inputHeight}\n"
            }

        } finally {

            image.close()
        }
    }

    private fun scanForEsp32() {

        if (
            Build.VERSION.SDK_INT >= 31 &&
            ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.BLUETOOTH_SCAN
            ) !=
            PackageManager.PERMISSION_GRANTED
        ) {

            requestBlePermissions()
            return
        }

        val manager =
            getSystemService(
                BluetoothManager::class.java
            )

        val adapter =
            manager.adapter

        if (adapter == null ||
            !adapter.isEnabled
        ) {

            status.text =
                "Bluetooth chưa bật"

            return
        }

        status.text =
            "BLE scan đang chạy..."

        adapter.bluetoothLeScanner
            .startScan(
                object :
                    android.bluetooth.le.ScanCallback() {

                    override fun onScanResult(
                        callbackType: Int,
                        result:
                        android.bluetooth.le.ScanResult
                    ) {

                        val device =
                            result.device

                        val name =
                            try {
                                device.name
                            } catch (_: SecurityException) {
                                null
                            }

                        if (
                            name != null &&
                            name.contains(
                                "ScreenAI",
                                true
                            )
                        ) {

                            adapter.bluetoothLeScanner
                                .stopScan(this)

                            runOnUiThread {

                                status.text =
                                    "ESP32 found: $name"
                            }

                            ble.connect(device)
                        }
                    }
                }
            )
    }

    private fun requestBlePermissions() {

        if (
            Build.VERSION.SDK_INT >= 31
        ) {

            ActivityCompat.requestPermissions(
                this,
                arrayOf(
                    Manifest.permission.BLUETOOTH_SCAN,
                    Manifest.permission.BLUETOOTH_CONNECT
                ),
                700
            )
        }
    }

    override fun onConnected() {

        runOnUiThread {

            status.append(
                "\nBLE: CONNECTED"
            )
        }
    }

    override fun onDisconnected() {

        runOnUiThread {

            status.append(
                "\nBLE: DISCONNECTED"
            )
        }
    }

    override fun onMessage(
        message: String
    ) {

        runOnUiThread {

            status.append(
                "\nESP32 → Android:\n$message"
            )
        }
    }

    override fun onError(
        message: String
    ) {

        runOnUiThread {

            status.append(
                "\nBLE ERROR:\n$message"
            )
        }
    }

    override fun onActivityResult(
        requestCode: Int,
        resultCode: Int,
        data: Intent?
    ) {

        super.onActivityResult(
            requestCode,
            resultCode,
            data
        )

        if (
            requestCode ==
            ScreenCaptureManager.REQUEST_CAPTURE &&
            resultCode == RESULT_OK &&
            data != null
        ) {

            capture.start(
                resultCode,
                data
            )

            status.text =
                "Screen capture STARTED"
        }
    }

    override fun onDestroy() {

        capture.stop()

        ble.close()

        engine.close()

        super.onDestroy()
    }
    }
