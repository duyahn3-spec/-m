package com.screenai

import android.Manifest
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothGattCallback
import android.bluetooth.BluetoothGattCharacteristic
import android.bluetooth.BluetoothGattDescriptor
import android.bluetooth.BluetoothManager
import android.content.Context
import android.content.pm.PackageManager
import androidx.core.content.ContextCompat
import java.util.UUID

class BleManager(
    private val context: Context
) {

    companion object {

        val SERVICE_UUID: UUID =
            UUID.fromString(
                "6e400001-b5a3-f393-e0a9-e50e24dcca9e"
            )

        val RX_UUID: UUID =
            UUID.fromString(
                "6e400002-b5a3-f393-e0a9-e50e24dcca9e"
            )

        val TX_UUID: UUID =
            UUID.fromString(
                "6e400003-b5a3-f393-e0a9-e50e24dcca9e"
            )

        val CCCD_UUID: UUID =
            UUID.fromString(
                "00002902-0000-1000-8000-00805f9b34fb"
            )
    }

    interface Listener {

        fun onConnected()

        fun onDisconnected()

        fun onMessage(message: String)

        fun onError(message: String)
    }

    var listener: Listener? = null

    private var gatt: BluetoothGatt? = null

    private var rxCharacteristic:
        BluetoothGattCharacteristic? = null

    fun connect(device: BluetoothDevice) {

        if (!hasConnectPermission()) {

            listener?.onError(
                "Thiếu BLUETOOTH_CONNECT permission"
            )

            return
        }

        gatt?.close()

        gatt = device.connectGatt(
            context,
            false,
            callback,
            BluetoothDevice.TRANSPORT_LE
        )
    }

    fun send(message: String) {

        val g = gatt
        val characteristic = rxCharacteristic

        if (g == null || characteristic == null) {

            listener?.onError(
                "BLE chưa connected"
            )

            return
        }

        if (!hasConnectPermission()) {
            return
        }

        characteristic.writeType =
            BluetoothGattCharacteristic
                .WRITE_TYPE_DEFAULT

        characteristic.value =
            message.toByteArray(Charsets.UTF_8)

        g.writeCharacteristic(characteristic)
    }

    private val callback =
        object : BluetoothGattCallback() {

            override fun onConnectionStateChange(
                gatt: BluetoothGatt,
                status: Int,
                newState: Int
            ) {

                if (newState ==
                    android.bluetooth.BluetoothProfile.STATE_CONNECTED
                ) {

                    this@BleManager.gatt = gatt

                    if (hasConnectPermission()) {
                        gatt.discoverServices()
                    }

                    listener?.onConnected()

                } else {

                    listener?.onDisconnected()
                }
            }

            override fun onServicesDiscovered(
                gatt: BluetoothGatt,
                status: Int
            ) {

                if (status !=
                    BluetoothGatt.GATT_SUCCESS
                ) {
                    listener?.onError(
                        "GATT service discovery failed"
                    )
                    return
                }

                val service =
                    gatt.getService(SERVICE_UUID)

                if (service == null) {

                    listener?.onError(
                        "Không tìm thấy service"
                    )

                    return
                }

                rxCharacteristic =
                    service.getCharacteristic(RX_UUID)

                val tx =
                    service.getCharacteristic(TX_UUID)

                if (tx != null &&
                    hasConnectPermission()
                ) {

                    gatt.setCharacteristicNotification(
                        tx,
                        true
                    )

                    val descriptor =
                        tx.getDescriptor(CCCD_UUID)

                    descriptor?.value =
                        BluetoothGattDescriptor
                            .ENABLE_NOTIFICATION_VALUE

                    if (descriptor != null) {
                        gatt.writeDescriptor(descriptor)
                    }
                }
            }

            override fun onCharacteristicChanged(
                gatt: BluetoothGatt,
                characteristic:
                BluetoothGattCharacteristic
            ) {

                if (characteristic.uuid == TX_UUID) {

                    val text =
                        characteristic.value
                            .toString(Charsets.UTF_8)

                    listener?.onMessage(text)
                }
            }

            @Suppress("DEPRECATION")
            override fun onCharacteristicChanged(
                gatt: BluetoothGatt,
                characteristic:
                BluetoothGattCharacteristic,
                value: ByteArray
            ) {

                if (characteristic.uuid == TX_UUID) {

                    listener?.onMessage(
                        value.toString(
                            Charsets.UTF_8
                        )
                    )
                }
            }
        }

    private fun hasConnectPermission(): Boolean {

        return if (
            android.os.Build.VERSION.SDK_INT >= 31
        ) {

            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.BLUETOOTH_CONNECT
            ) == PackageManager.PERMISSION_GRANTED

        } else {
            true
        }
    }

    fun close() {

        if (hasConnectPermission()) {
            gatt?.disconnect()
        }

        gatt?.close()

        gatt = null
        rxCharacteristic = null
    }
}
