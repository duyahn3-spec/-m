package com.screenai

import android.Manifest
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothGattCallback
import android.bluetooth.BluetoothGattCharacteristic
import android.bluetooth.BluetoothManager
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanResult
import android.content.Context
import android.content.pm.PackageManager
import androidx.core.content.ContextCompat
import java.util.UUID

class BleManager(
    private val context: Context
) {

    companion object {

        /*
         * Đổi thành UUID của firmware ESP32-C3.
         */

        val SERVICE_UUID: UUID =
            UUID.fromString(
                "0000FFF0-0000-1000-8000-00805F9B34FB"
            )

        val CHARACTERISTIC_UUID: UUID =
            UUID.fromString(
                "0000FFF1-0000-1000-8000-00805F9B34FB"
            )
    }

    private val bluetoothManager =
        context.getSystemService(
            Context.BLUETOOTH_SERVICE
        ) as BluetoothManager

    private val adapter:
        BluetoothAdapter?
        get() = bluetoothManager.adapter

    private var scanning = false

    private var gatt:
        BluetoothGatt? = null

    private var characteristic:
        BluetoothGattCharacteristic? = null

    private val scanCallback =
        object : ScanCallback() {

            override fun onScanResult(
                callbackType: Int,
                result: ScanResult
            ) {

                /*
                 * Chỉ tự động chọn thiết bị
                 * có service UUID đã cấu hình.
                 */

                val device =
                    result.device

                if (
                    result.scanRecord
                        ?.serviceUuids
                        ?.any {
                            it.uuid ==
                                    SERVICE_UUID
                        } == true
                ) {

                    stopScan()

                    connect(
                        device
                    )
                }
            }
        }

    private val gattCallback =
        object : BluetoothGattCallback() {

            override fun onConnectionStateChange(
                gatt: BluetoothGatt,
                status: Int,
                newState: Int
            ) {

                if (
                    newState ==
                    android.bluetooth.BluetoothProfile.STATE_CONNECTED
                ) {

                    if (
                        hasConnectPermission()
                    ) {
                        gatt.discoverServices()
                    }

                } else {

                    characteristic = null

                    try {
                        gatt.close()
                    } catch (_: Throwable) {
                    }

                    this@BleManager.gatt =
                        null
                }
            }

            override fun onServicesDiscovered(
                gatt: BluetoothGatt,
                status: Int
            ) {

                if (
                    status !=
                    BluetoothGatt.GATT_SUCCESS
                ) {
                    return
                }

                characteristic =
                    gatt.getService(
                        SERVICE_UUID
                    )?.getCharacteristic(
                        CHARACTERISTIC_UUID
                    )
            }
        }

    fun start() {

        if (
            adapter == null ||
            !adapter!!.isEnabled
        ) {
            return
        }

        if (!hasScanPermission()) {
            return
        }

        if (scanning) {
            return
        }

        val scanner =
            adapter!!
                .bluetoothLeScanner
                ?: return

        scanning = true

        scanner.startScan(
            scanCallback
        )
    }

    private fun connect(
        device: android.bluetooth.BluetoothDevice
    ) {

        if (!hasConnectPermission()) {
            return
        }

        gatt?.close()

        gatt =
            device.connectGatt(
                context,
                false,
                gattCallback,
                BluetoothDevice.TRANSPORT_LE
            )
    }

    fun sendTelemetry(
        target: TrackedTarget
    ) {

        val g =
            gatt ?: return

        val c =
            characteristic ?: return

        if (!hasConnectPermission()) {
            return
        }

        /*
         * Telemetry text:
         *
         * X,Y,DX,DY,confidence
         */

        val payload =
            String.format(
                java.util.Locale.US,
                "%.5f,%.5f,%.5f,%.5f,%.5f",
                target.x,
                target.y,
                target.dx,
                target.dy,
                target.confidence
            ).toByteArray()

        c.value = payload

        try {

            if (
                android.os.Build.VERSION.SDK_INT >=
                android.os.Build.VERSION_CODES.TIRAMISU
            ) {

                g.writeCharacteristic(
                    c,
                    payload,
                    BluetoothGattCharacteristic
                        .WRITE_TYPE_DEFAULT
                )

            } else {

                @Suppress("DEPRECATION")
                g.writeCharacteristic(c)
            }

        } catch (
            _: Throwable
        ) {
        }
    }

    private fun stopScan() {

        if (!scanning) {
            return
        }

        if (!hasScanPermission()) {
            return
        }

        try {

            adapter
                ?.bluetoothLeScanner
                ?.stopScan(
                    scanCallback
                )

        } catch (_: Throwable) {
        }

        scanning = false
    }

    fun stop() {

        stopScan()

        try {

            if (hasConnectPermission()) {
                gatt?.disconnect()
            }

            gatt?.close()

        } catch (_: Throwable) {
        }

        gatt = null
        characteristic = null
    }

    private fun hasScanPermission():
        Boolean {

        return ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.BLUETOOTH_SCAN
        ) == PackageManager.PERMISSION_GRANTED
    }

    private fun hasConnectPermission():
        Boolean {

        return ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.BLUETOOTH_CONNECT
        ) == PackageManager.PERMISSION_GRANTED
    }
}
