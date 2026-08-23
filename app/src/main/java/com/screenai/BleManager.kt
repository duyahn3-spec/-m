package com.screenai

import android.Manifest
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothManager
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanResult
import android.content.Context
import android.content.pm.PackageManager
import androidx.core.content.ContextCompat

class BleManager(
    private val context: Context
) {

    private val bluetoothManager =
        context.getSystemService(
            Context.BLUETOOTH_SERVICE
        ) as BluetoothManager

    private val adapter: BluetoothAdapter?
        get() = bluetoothManager.adapter

    private var scanning = false

    private val callback =
        object : ScanCallback() {

            override fun onScanResult(
                callbackType: Int,
                result: ScanResult
            ) {

                /*
                 * Chỉ phát hiện thiết bị.
                 *
                 * Không tự động gửi dữ liệu
                 * cho thiết bị chưa được chọn/kết nối.
                 */
            }

            override fun onScanFailed(
                errorCode: Int
            ) {
                scanning = false
            }
        }

    fun start() {

        if (
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.BLUETOOTH_SCAN
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            return
        }

        val scanner =
            adapter?.bluetoothLeScanner
                ?: return

        if (scanning) return

        scanner.startScan(callback)

        scanning = true
    }

    fun sendTelemetry(
        target: TrackedTarget
    ) {

        /*
         * Ở đây sẽ gửi:
         *
         * X
         * Y
         * DX
         * DY
         * confidence
         *
         * qua characteristic GATT của ESP32.
         *
         * Chưa hard-code UUID vì firmware
         * ESP32-C3 của bạn chưa được cung cấp.
         */
    }

    fun stop() {

        if (
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.BLUETOOTH_SCAN
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            return
        }

        if (!scanning) return

        adapter
            ?.bluetoothLeScanner
            ?.stopScan(callback)

        scanning = false
    }
}
