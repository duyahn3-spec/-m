import android.annotation.SuppressLint
import android.bluetooth.*
import android.content.Context
import java.util.*

@SuppressLint("MissingPermission")
class BleManager(private val context: Context, private val macAddress: String) {
    private var bluetoothGatt: BluetoothGatt? = null
    private var targetCharacteristic: BluetoothGattCharacteristic? = null

    private val SERVICE_UUID = UUID.fromString("7c9e0001-1111-2222-3333-444444444444")
    private val CHAR_UUID = UUID.fromString("7c9e0002-1111-2222-3333-444444444444")

    fun connect() {
        val bluetoothManager = context.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
        val bluetoothAdapter = bluetoothManager.adapter
        val device = bluetoothAdapter?.getRemoteDevice(macAddress)
        bluetoothGatt = device?.connectGatt(context, false, gattCallback)
    }

    private val gattCallback = object : BluetoothGattCallback() {
        override fun onConnectionStateChange(gatt: BluetoothGatt, status: Int, newState: Int) {
            if (newState == BluetoothProfile.STATE_CONNECTED) {
                gatt.discoverServices()
            } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                // Tự động kết nối lại
                gatt.connect()
            }
        }

        override fun onServicesDiscovered(gatt: BluetoothGatt, status: Int) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
                val service = gatt.getService(SERVICE_UUID)
                targetCharacteristic = service?.getCharacteristic(CHAR_UUID)
            }
        }
    }

    fun sendData(dx: Short, dy: Short) {
        val char = targetCharacteristic ?: return
        val gatt = bluetoothGatt ?: return

        // Đóng gói 4 bytes (Little-Endian)
        val byteArray = ByteArray(4)
        byteArray[0] = (dx.toInt() and 0xFF).toByte()
        byteArray[1] = ((dx.toInt() shr 8) and 0xFF).toByte()
        byteArray[2] = (dy.toInt() and 0xFF).toByte()
        byteArray[3] = ((dy.toInt() shr 8) and 0xFF).toByte()

        char.value = byteArray
        char.writeType = BluetoothGattCharacteristic.WRITE_TYPE_NO_RESPONSE
        gatt.writeCharacteristic(char)
    }

    fun disconnect() {
        bluetoothGatt?.disconnect()
        bluetoothGatt?.close()
        bluetoothGatt = null
    }
}
