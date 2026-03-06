package com.example.feeder.service

import android.Manifest
import android.annotation.SuppressLint
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothGattCallback
import android.bluetooth.BluetoothGattCharacteristic
import android.bluetooth.BluetoothGattDescriptor
import android.bluetooth.BluetoothManager
import android.bluetooth.BluetoothProfile
import android.bluetooth.le.BluetoothLeScanner
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanResult
import android.bluetooth.le.ScanSettings
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.BitmapFactory
import android.os.Build
import android.os.IBinder
import android.util.Base64
import androidx.annotation.RequiresPermission
import androidx.core.app.NotificationCompat
import com.example.feeder.R
import com.example.feeder.ui.ConsumerDetailsActivity
import java.io.ByteArrayOutputStream
import java.io.File
import java.util.UUID

class BluetoothLeService : Service() {

    companion object {
        const val ACTION_START_SCAN = "com.example.feeder.action.START_SCAN"
        const val ACTION_STOP_SCAN = "com.example.feeder.action.STOP_SCAN"
        const val ACTION_CONNECT = "com.example.feeder.action.CONNECT"
        const val ACTION_DISCONNECT = "com.example.feeder.action.DISCONNECT"
        const val ACTION_DEVICE_FOUND = "com.example.feeder.action.DEVICE_FOUND"
        const val ACTION_STATUS = "com.example.feeder.action.STATUS"
        const val ACTION_DATA = "com.example.feeder.action.DATA"
        const val ACTION_NOTIFY_LIST = "com.example.feeder.action.NOTIFY_LIST"
        const val ACTION_SUBSCRIBE_CHAR = "com.example.feeder.action.SUBSCRIBE_CHAR"
        const val ACTION_READ_CHAR = "com.example.feeder.action.READ_CHAR"

        const val EXTRA_DEVICE_ADDRESS = "extra_device_address"
        const val EXTRA_DEVICE_NAME = "extra_device_name"
        const val EXTRA_STATUS = "extra_status"
        const val EXTRA_DATA_TYPE = "extra_data_type"
        const val EXTRA_DATA_TEXT = "extra_data_text"
        const val EXTRA_DATA_BYTES = "extra_data_bytes"
        const val EXTRA_NOTIFY_LIST = "extra_notify_list"
        const val EXTRA_SERVICE_UUID = "extra_service_uuid"
        const val EXTRA_CHAR_UUID = "extra_char_uuid"

        const val DATA_TYPE_TEXT = "text"
        const val DATA_TYPE_JSON = "json"
        const val DATA_TYPE_BINARY = "binary"
        const val DATA_TYPE_FILE = "file"

        const val EXTRA_FILE_NAME = "extra_file_name"
        const val EXTRA_FILE_MIME = "extra_file_mime"
        const val EXTRA_FILE_PATH = "extra_file_path"
        const val EXTRA_FILE_SIZE = "extra_file_size"

        private val UUID_GAP_SERVICE: UUID = UUID.fromString("00001800-0000-1000-8000-00805f9b34fb")
        private val UUID_DEVICE_NAME: UUID = UUID.fromString("00002a00-0000-1000-8000-00805f9b34fb")
    }

    private val channelId = "ble_connection_alerts"
    private val notificationId = 9901

    private var bluetoothAdapter: BluetoothAdapter? = null
    private var scanner: BluetoothLeScanner? = null
    private var isScanning = false
    private val discoveredAddresses = mutableSetOf<String>()

    private var bluetoothGatt: BluetoothGatt? = null
    private var connectedDeviceName: String? = null

    private val lineBuffer = StringBuilder()
    private var isReceivingFile = false
    private var fileName: String? = null
    private var fileMime: String? = null
    private var fileExpectedSize: Long = 0
    private var fileBuffer: ByteArrayOutputStream? = null

    override fun onCreate() {
        super.onCreate()
        val manager = getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
        bluetoothAdapter = manager.adapter
        scanner = bluetoothAdapter?.bluetoothLeScanner
        createNotificationChannel()
    }

    @SuppressLint("MissingPermission")
    @RequiresPermission(Manifest.permission.BLUETOOTH_SCAN)
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START_SCAN -> {
                ensureForeground("Scanning for BLE devices", "Scanning...")
                startScan()
            }
            ACTION_STOP_SCAN -> stopScan()
            ACTION_CONNECT -> {
                val address = intent.getStringExtra(EXTRA_DEVICE_ADDRESS)
                if (address.isNullOrBlank()) {
                    sendStatus("Invalid device address")
                } else {
                    ensureForeground("Connecting", address)
                    connect(address)
                }
            }
            ACTION_DISCONNECT -> disconnect()
            ACTION_SUBSCRIBE_CHAR -> {
                val serviceUuid = intent.getStringExtra(EXTRA_SERVICE_UUID)
                val charUuid = intent.getStringExtra(EXTRA_CHAR_UUID)
                if (serviceUuid.isNullOrBlank() || charUuid.isNullOrBlank()) {
                    sendStatus("Characteristic UUID missing")
                } else {
                    subscribeCharacteristic(serviceUuid, charUuid)
                }
            }
            ACTION_READ_CHAR -> {
                val serviceUuid = intent.getStringExtra(EXTRA_SERVICE_UUID)
                val charUuid = intent.getStringExtra(EXTRA_CHAR_UUID)
                if (serviceUuid.isNullOrBlank() || charUuid.isNullOrBlank()) {
                    sendStatus("Characteristic UUID missing")
                } else {
                    readCharacteristic(serviceUuid, charUuid)
                }
            }
        }
        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null

    @SuppressLint("MissingPermission")
    @RequiresPermission(Manifest.permission.BLUETOOTH_SCAN)
    override fun onDestroy() {
        stopScan()
        disconnect()
        super.onDestroy()
    }

    private fun ensureForeground(title: String, text: String) {
        val notification = buildNotification(title, text)
        startForeground(notificationId, notification)
    }

    private fun buildNotification(title: String, text: String): Notification {
        val intent = Intent(this, ConsumerDetailsActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(this, channelId)
            .setSmallIcon(R.drawable.ic_ble_notify)
            .setLargeIcon(BitmapFactory.decodeResource(resources, R.drawable.logo))
            .setContentTitle(title)
            .setContentText(text)
            .setContentIntent(pendingIntent)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setVibrate(longArrayOf(0, 250, 150, 250))
            .setOngoing(true)
            .build()
    }

    private fun updateNotification(title: String, text: String) {
        val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        manager.notify(notificationId, buildNotification(title, text))
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "BLE Alerts",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                enableVibration(true)
                vibrationPattern = longArrayOf(0, 250, 150, 250)
            }
            val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            manager.createNotificationChannel(channel)
        }
    }

    private fun hasScanPermission(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            checkSelfPermission(Manifest.permission.BLUETOOTH_SCAN) == PackageManager.PERMISSION_GRANTED
        } else {
            checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
        }
    }

    private fun hasConnectPermission(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            checkSelfPermission(Manifest.permission.BLUETOOTH_CONNECT) == PackageManager.PERMISSION_GRANTED
        } else {
            true
        }
    }

    @RequiresPermission(Manifest.permission.BLUETOOTH_SCAN)
    private fun startScan() {
        if (!hasScanPermission()) {
            sendStatus("Bluetooth scan permission missing")
            return
        }
        if (bluetoothAdapter == null || bluetoothAdapter?.isEnabled != true) {
            sendStatus("Bluetooth is off")
            return
        }
        if (isScanning) return
        discoveredAddresses.clear()
        val settingsBuilder = ScanSettings.Builder()
            .setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O &&
            bluetoothAdapter?.isLeExtendedAdvertisingSupported == true
        ) {
            settingsBuilder.setLegacy(false)
            settingsBuilder.setPhy(ScanSettings.PHY_LE_ALL_SUPPORTED)
        }

        scanner?.startScan(null, settingsBuilder.build(), scanCallback)
        isScanning = true
        sendStatus("Scanning for BLE devices...")
    }

    @RequiresPermission(Manifest.permission.BLUETOOTH_SCAN)
    private fun stopScan() {
        if (!isScanning) return
        scanner?.stopScan(scanCallback)
        isScanning = false
        sendStatus("Scan stopped")
    }


    private val scanCallback = object : ScanCallback() {
        @RequiresPermission(Manifest.permission.BLUETOOTH_CONNECT)
        override fun onScanResult(callbackType: Int, result: ScanResult?) {
            val device = result?.device ?: return
            val address = device.address ?: return
            if (discoveredAddresses.add(address)) {
                val recordName = result.scanRecord?.deviceName
                sendDeviceFound(device, recordName)
            }
        }

        override fun onScanFailed(errorCode: Int) {
            sendStatus("Scan failed: $errorCode")
        }
    }


    @RequiresPermission(allOf = [Manifest.permission.BLUETOOTH_SCAN, Manifest.permission.BLUETOOTH_CONNECT])
    private fun connect(address: String) {
        if (!hasConnectPermission()) {
            sendStatus("Bluetooth connect permission missing")
            return
        }
        stopScan()
        disconnect()
        val device = bluetoothAdapter?.getRemoteDevice(address)
        if (device == null) {
            sendStatus("Device not found")
            return
        }
        connectedDeviceName = device.name ?: address
        sendStatus("Connecting to ${connectedDeviceName}")
        updateNotification("Connecting", connectedDeviceName ?: address)
        bluetoothGatt = device.connectGatt(this, false, gattCallback, BluetoothDevice.TRANSPORT_LE)
    }

    @RequiresPermission(Manifest.permission.BLUETOOTH_CONNECT)
    private fun disconnect() {
        bluetoothGatt?.disconnect()
        bluetoothGatt?.close()
        bluetoothGatt = null
        connectedDeviceName = null
        updateNotification("Disconnected", "No device")
        sendStatus("Disconnected")
    }

    private val gattCallback = object : BluetoothGattCallback() {
        @RequiresPermission(Manifest.permission.BLUETOOTH_CONNECT)
        override fun onConnectionStateChange(gatt: BluetoothGatt, status: Int, newState: Int) {
            if (newState == BluetoothProfile.STATE_CONNECTED) {
                bluetoothGatt = gatt
                val name = connectedDeviceName ?: "Unknown"
                sendStatus("Connected to $name")
                updateNotification("Connected", name)
                gatt.discoverServices()
            } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                sendStatus("Device disconnected")
                updateNotification("Disconnected", "No device")
            }
        }

        @RequiresPermission(Manifest.permission.BLUETOOTH_CONNECT)
        override fun onServicesDiscovered(gatt: BluetoothGatt, status: Int) {
            if (status != BluetoothGatt.GATT_SUCCESS) {
                sendStatus("Service discovery failed")
                return
            }
            val gapService = gatt.getService(UUID_GAP_SERVICE)
            val nameChar = gapService?.getCharacteristic(UUID_DEVICE_NAME)
            if (nameChar != null) {
                gatt.readCharacteristic(nameChar)
            }
            val characteristicList = collectCharacteristics(gatt)
            if (characteristicList.isEmpty()) {
                sendStatus("No characteristics found")
                return
            }
            sendNotifyList(characteristicList)
            sendStatus("Select characteristic to subscribe")
        }

        override fun onCharacteristicRead(
            gatt: BluetoothGatt,
            characteristic: BluetoothGattCharacteristic,
            status: Int
        ) {
            if (status != BluetoothGatt.GATT_SUCCESS) return
            if (characteristic.uuid == UUID_DEVICE_NAME) {
                val name = characteristic.value?.toString(Charsets.UTF_8)?.trim()
                if (!name.isNullOrBlank()) {
                    connectedDeviceName = name
                    sendStatus("Connected to $name")
                    updateNotification("Connected", name)
                }
                return
            }
            val data = characteristic.value ?: return
            sendStatus("Read ${characteristic.uuid}")
            handleIncomingData(data)
        }

        override fun onCharacteristicChanged(
            gatt: BluetoothGatt,
            characteristic: BluetoothGattCharacteristic
        ) {
            val data = characteristic.value ?: return
            handleIncomingData(data)
        }

        override fun onDescriptorWrite(
            gatt: BluetoothGatt,
            descriptor: BluetoothGattDescriptor,
            status: Int
        ) {
            val ok = status == BluetoothGatt.GATT_SUCCESS
            val msg = if (ok) "Notify enabled" else "Notify enable failed: $status"
            sendStatus(msg)
        }
    }

    @RequiresPermission(Manifest.permission.BLUETOOTH_CONNECT)
    private fun enableNotifications(gatt: BluetoothGatt, ch: BluetoothGattCharacteristic) {
        val props = ch.properties
        val enableValue = if ((props and BluetoothGattCharacteristic.PROPERTY_INDICATE) != 0) {
            BluetoothGattDescriptor.ENABLE_INDICATION_VALUE
        } else {
            BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE
        }

        gatt.setCharacteristicNotification(ch, true)
        val cccd = ch.getDescriptor(UUID.fromString("00002902-0000-1000-8000-00805f9b34fb"))
        if (cccd != null) {
            cccd.value = enableValue
            gatt.writeDescriptor(cccd)
        } else {
            sendStatus("Notify CCCD not found")
        }
    }

    private fun collectCharacteristics(gatt: BluetoothGatt): List<String> {
        val list = mutableListOf<String>()
        for (service in gatt.services) {
            for (ch in service.characteristics) {
                list.add("${service.uuid}|${ch.uuid}|${ch.properties}")
            }
        }
        return list
    }

    private fun handleIncomingData(bytes: ByteArray) {
        val text = bytes.toString(Charsets.UTF_8)

        if (text.contains("FILE_BEGIN:") || text.contains("FILE_CHUNK:") || text.contains("FILE_END") || isReceivingFile) {
            handleFileProtocol(text)
            return
        }

        if (text.startsWith("JSON:")) {
            val payload = text.removePrefix("JSON:")
            sendData(DATA_TYPE_JSON, payload, null)
            return
        }

        if (text.startsWith("TXT:")) {
            val payload = text.removePrefix("TXT:")
            sendData(DATA_TYPE_TEXT, payload, null)
            return
        }

        if (looksLikeJson(text)) {
            sendData(DATA_TYPE_JSON, text, null)
            return
        }

        if (isMostlyPrintable(text)) {
            sendData(DATA_TYPE_TEXT, text, null)
            return
        }

        sendData(DATA_TYPE_BINARY, bytesToHex(bytes), bytes)
    }

    private fun handleFileProtocol(text: String) {
        lineBuffer.append(text)
        var index = lineBuffer.indexOf("\n")
        while (index >= 0) {
            val line = lineBuffer.substring(0, index).trim()
            lineBuffer.delete(0, index + 1)
            processFileLine(line)
            index = lineBuffer.indexOf("\n")
        }
    }

    private fun processFileLine(line: String) {
        if (line.startsWith("FILE_BEGIN:")) {
            val parts = line.removePrefix("FILE_BEGIN:").split(":")
            if (parts.size < 3) {
                sendStatus("Invalid FILE_BEGIN")
                return
            }
            fileName = parts[0].trim()
            fileMime = parts[1].trim()
            fileExpectedSize = parts[2].trim().toLongOrNull() ?: 0
            fileBuffer = ByteArrayOutputStream()
            isReceivingFile = true
            sendStatus("Receiving file ${fileName ?: ""}")
            return
        }

        if (line.startsWith("FILE_CHUNK:")) {
            if (!isReceivingFile || fileBuffer == null) return
            val chunk = line.removePrefix("FILE_CHUNK:").trim()
            if (chunk.isNotEmpty()) {
                try {
                    val decoded = Base64.decode(chunk, Base64.DEFAULT)
                    fileBuffer?.write(decoded)
                } catch (_: IllegalArgumentException) {
                    sendStatus("Invalid file chunk")
                }
            }
            return
        }

        if (line.startsWith("FILE_END")) {
            if (!isReceivingFile || fileBuffer == null) return
            val bytes = fileBuffer!!.toByteArray()
            val safeName = sanitizeFileName(fileName ?: "ble_file_${System.currentTimeMillis()}")
            val dir = File(cacheDir, "ble_files").apply { mkdirs() }
            val file = File(dir, safeName)
            file.writeBytes(bytes)

            sendFileData(
                name = safeName,
                mime = fileMime ?: "application/octet-stream",
                path = file.absolutePath,
                size = bytes.size.toLong()
            )

            isReceivingFile = false
            fileBuffer = null
            fileName = null
            fileMime = null
            fileExpectedSize = 0
            return
        }
    }

    private fun sanitizeFileName(name: String): String {
        return File(name).name.replace(Regex("[\\\\/:*?\"<>|]"), "_")
    }

    private fun looksLikeJson(text: String): Boolean {
        val trimmed = text.trim()
        return (trimmed.startsWith("{") && trimmed.endsWith("}")) ||
            (trimmed.startsWith("[") && trimmed.endsWith("]"))
    }

    private fun isMostlyPrintable(text: String): Boolean {
        if (text.isBlank()) return false
        val printable = text.count { it.code in 32..126 || it == '\n' || it == '\r' || it == '\t' }
        return printable.toFloat() / text.length.toFloat() > 0.8f
    }

    private fun bytesToHex(bytes: ByteArray): String {
        val sb = StringBuilder(bytes.size * 2)
        for (b in bytes) {
            sb.append(String.format("%02X", b))
        }
        return sb.toString()
    }


    @RequiresPermission(Manifest.permission.BLUETOOTH_CONNECT)
    private fun sendDeviceFound(device: BluetoothDevice, scanName: String?) {
        val intent = Intent(ACTION_DEVICE_FOUND)
            .setPackage(packageName)
            .putExtra(EXTRA_DEVICE_ADDRESS, device.address)
            .putExtra(EXTRA_DEVICE_NAME, scanName ?: device.name ?: "Unknown")
        sendBroadcast(intent)
    }

    private fun sendStatus(status: String) {
        val intent = Intent(ACTION_STATUS)
            .setPackage(packageName)
            .putExtra(EXTRA_STATUS, status)
        sendBroadcast(intent)
    }

    private fun sendData(type: String, text: String?, bytes: ByteArray?) {
        val intent = Intent(ACTION_DATA)
            .setPackage(packageName)
            .putExtra(EXTRA_DATA_TYPE, type)
        if (text != null) intent.putExtra(EXTRA_DATA_TEXT, text)
        if (bytes != null) intent.putExtra(EXTRA_DATA_BYTES, bytes)
        sendBroadcast(intent)
    }

    private fun sendNotifyList(list: List<String>) {
        val intent = Intent(ACTION_NOTIFY_LIST)
            .setPackage(packageName)
            .putStringArrayListExtra(EXTRA_NOTIFY_LIST, ArrayList(list))
        sendBroadcast(intent)
    }

    @RequiresPermission(Manifest.permission.BLUETOOTH_CONNECT)
    private fun subscribeCharacteristic(serviceUuid: String, charUuid: String) {
        val gatt = bluetoothGatt ?: return
        val service = gatt.getService(UUID.fromString(serviceUuid)) ?: run {
            sendStatus("Service not found")
            return
        }
        val characteristic = service.getCharacteristic(UUID.fromString(charUuid)) ?: run {
            sendStatus("Characteristic not found")
            return
        }
        enableNotifications(gatt, characteristic)
        sendStatus("Subscribed to $charUuid")
    }

    @RequiresPermission(Manifest.permission.BLUETOOTH_CONNECT)
    private fun readCharacteristic(serviceUuid: String, charUuid: String) {
        val gatt = bluetoothGatt ?: return
        val service = gatt.getService(UUID.fromString(serviceUuid)) ?: run {
            sendStatus("Service not found")
            return
        }
        val characteristic = service.getCharacteristic(UUID.fromString(charUuid)) ?: run {
            sendStatus("Characteristic not found")
            return
        }
        val ok = gatt.readCharacteristic(characteristic)
        if (!ok) {
            sendStatus("Read request failed")
        }
    }

    private fun sendFileData(name: String, mime: String, path: String, size: Long) {
        val intent = Intent(ACTION_DATA)
            .setPackage(packageName)
            .putExtra(EXTRA_DATA_TYPE, DATA_TYPE_FILE)
            .putExtra(EXTRA_FILE_NAME, name)
            .putExtra(EXTRA_FILE_MIME, mime)
            .putExtra(EXTRA_FILE_PATH, path)
            .putExtra(EXTRA_FILE_SIZE, size)
        sendBroadcast(intent)
    }
}
