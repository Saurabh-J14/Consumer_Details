package com.example.feeder.ui

import ConsumerUpdateBody
import ConsumerUpdateRepository
import android.Manifest
import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.os.Handler
import android.os.Looper
import android.provider.MediaStore
import android.text.TextUtils
import android.util.Log
import android.view.View
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.activity.viewModels
import androidx.annotation.RequiresPermission
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import com.example.feeder.R
import com.example.feeder.data.remote.RetrofitClient
import com.example.feeder.databinding.ActivityConsumerDetailsBinding
import com.example.feeder.service.BluetoothLeService
import com.example.feeder.ui.base.ConsumerUpdateViewModelFactory
import com.example.feeder.ui.viewModel.ConsumerUpdateViewModel
import com.example.feeder.utils.FusedLocationTracker
import com.example.feeder.utils.PrefManager
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

class ConsumerDetailsActivity : AppCompatActivity() {

    companion object { private const val TAG = "BLE Connectivity" }
    private lateinit var binding: ActivityConsumerDetailsBinding
    private lateinit var prefManager: PrefManager
    private val BT_PERMISSION_REQ = 1010
    private val REQ_ENABLE_BT = 1011
    private val CAMERA_REQ = 1001
    private val PERMISSION_REQ = 2001
    private var latitude = 0.0
    private var longitude = 0.0
    private var capturedBitmap: Bitmap? = null
    private var openCameraAfterLocation = false
    private lateinit var fusedLocationClient: FusedLocationTracker

    private var bluetoothAdapter: BluetoothAdapter? = null
    private val deviceAddresses = mutableListOf<String>()
    private val deviceLabels = mutableListOf<String>()
    private lateinit var deviceAdapter: ArrayAdapter<String>
    private var isScanning = false
    private val serviceGroups = mutableListOf<ServiceGroup>()
    private lateinit var characteristicAdapter: BleCharTreeAdapter
    private val scanHandler = Handler(Looper.getMainLooper())
    private val scanTimeoutMs = 60_000L
    private val stopScanRunnable = Runnable {
        if (isScanning) stopBleScan()
    }


    private val viewModel: ConsumerUpdateViewModel by viewModels {
        ConsumerUpdateViewModelFactory(ConsumerUpdateRepository(RetrofitClient.getServices()))
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityConsumerDetailsBinding.inflate(layoutInflater)
        setContentView(binding.root)
        fusedLocationClient = FusedLocationTracker(this)
        prefManager = PrefManager(this)

        binding.txtOpenCamera.setOnClickListener {
            binding.imgPhoto.visibility = View.GONE
            checkCameraPermission()
        }
        binding.phasorView.setRotation(45f)
        setupConnectDeviceButton()

        setupPhaseButton()
        setupToolbar()
        setupUpdateButton()
        loadConsumerData()
        setupBleUi()


        binding.swipeRefresh.setOnRefreshListener {
            loadConsumerData()
        }
        binding.etphases.addTextChangedListener(object : android.text.TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}

            override fun afterTextChanged(s: android.text.Editable?) {
                val input = s.toString().trim().uppercase()
                binding.etphases.setTextColor(when (input) {
                    "RYB" -> Color.parseColor("#D32F2F")
                    "R" -> Color.parseColor("#D32F2F")
                    "RY"  -> Color.parseColor("#FFEB3B")
                    "B"   -> Color.parseColor("#1976D2")
                    "RB"   -> Color.parseColor("#1976D2")
                    "YB"   -> Color.parseColor("#1976D2")
                    "y"   -> Color.parseColor("#FFEB3B")
                    else  -> Color.BLACK
                })
            }
        })
    }
    @SuppressLint("MissingPermission", "ClickableViewAccessibility")
    private fun setupBleUi() {

        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter()

        deviceAdapter = ArrayAdapter(this, R.layout.item_ble_device, R.id.tvBleDevice, deviceLabels)
        characteristicAdapter = BleCharTreeAdapter(this, serviceGroups)

        binding.listDevices.adapter = deviceAdapter
        binding.listDevices.setOnTouchListener { v, _ ->
            v.parent.requestDisallowInterceptTouchEvent(true)
            false
        }
        binding.listCharacteristics.setAdapter(characteristicAdapter)
        binding.listCharacteristics.setOnTouchListener { v, _ ->
            v.parent.requestDisallowInterceptTouchEvent(true)
            false
        }

        binding.btnBluetooth.setOnClickListener {

            bluetoothAdapter = BluetoothAdapter.getDefaultAdapter()

            if (bluetoothAdapter == null) {
                Toast.makeText(this, "Bluetooth not supported", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (!ensureBlePermissions()) return@setOnClickListener

            if (bluetoothAdapter?.isEnabled != true) {
                startActivityForResult(Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE), REQ_ENABLE_BT)
                return@setOnClickListener
            }

            if (isScanning) {
                stopBleScan()
            } else {
                startBleScan()
            }
        }

        binding.listDevices.setOnItemClickListener { _, _, position, _ ->
            val address = deviceAddresses.getOrNull(position) ?: return@setOnItemClickListener
            connectToBleDevice(address)
        }
        binding.listCharacteristics.setOnChildClickListener { _, _, groupPosition, childPosition, _ ->
            val item = serviceGroups
                .getOrNull(groupPosition)
                ?.chars
                ?.getOrNull(childPosition)
                ?: return@setOnChildClickListener true
            val service = item.serviceUuid
            val ch = item.charUuid
            val props = item.properties
            val canRead = (props and android.bluetooth.BluetoothGattCharacteristic.PROPERTY_READ) != 0
            val canNotify = (props and android.bluetooth.BluetoothGattCharacteristic.PROPERTY_NOTIFY) != 0
            val canIndicate = (props and android.bluetooth.BluetoothGattCharacteristic.PROPERTY_INDICATE) != 0
            if (canRead) {
                binding.tvBleCharStatus.text = "Status: Reading..."
                readCharacteristic(service, ch)
            }
            if (canNotify || canIndicate) {
                binding.tvBleCharStatus.text = "Status: Subscribing..."
                subscribeToCharacteristic(service, ch)
            }
            if (!canRead && !canNotify && !canIndicate) {
                binding.tvBleCharStatus.text = "Status: Not read/notify/indicate"
            }
            true
        }
        refreshPairedDevices()
    }

    private fun startBleScan() {
        deviceAddresses.clear()
        deviceLabels.clear()
        serviceGroups.clear()
        deviceAdapter.notifyDataSetChanged()
        characteristicAdapter.notifyDataSetChanged()
        binding.listDevices.visibility = View.VISIBLE
        binding.listCharacteristics.visibility = View.GONE
        binding.tvBleCharStatus.text = "Status: Idle"
        binding.tvBleData.text = "No data"
        binding.tvBleStatus.text = "Status: Scanning..."
        binding.btnBluetooth.text = "Stop Scan"
        isScanning = true
        startBleService(BluetoothLeService.ACTION_START_SCAN)
        scanHandler.removeCallbacks(stopScanRunnable)
        scanHandler.postDelayed(stopScanRunnable, scanTimeoutMs)
    }

    private fun stopBleScan() {
        binding.tvBleStatus.text = "Status: Scan stopped"
        binding.btnBluetooth.text = "Scan BLE Devices"
        isScanning = false
        startBleService(BluetoothLeService.ACTION_STOP_SCAN)
        scanHandler.removeCallbacks(stopScanRunnable)
    }


    @SuppressLint("MissingPermission")
    private fun refreshPairedDevices() {
        if (bluetoothAdapter == null) return
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            if (ContextCompat.checkSelfPermission(
                    this@ConsumerDetailsActivity,
                    Manifest.permission.BLUETOOTH_CONNECT
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                return
            }
        }
        val bonded = bluetoothAdapter?.bondedDevices ?: emptySet()
        for (device in bonded) {
            val address = device.address
            val name = device.name?.trim()
            if (name.isNullOrBlank() || name.equals("Unknown", ignoreCase = true)) continue
            if (!deviceAddresses.contains(address)) {
                deviceAddresses.add(address)
                deviceLabels.add("Paired: $name\n$address")
                deviceAdapter.notifyDataSetChanged()
            }
        }
    }

    private fun connectToBleDevice(address: String) {
        binding.tvBleStatus.text = "Status: Connecting..."
        startBleService(BluetoothLeService.ACTION_CONNECT, address)
    }

    private fun startBleService(action: String, address: String? = null) {
        val intent = Intent(this, BluetoothLeService::class.java).apply {
            this.action = action
            if (address != null) {
                putExtra(BluetoothLeService.EXTRA_DEVICE_ADDRESS, address)
            }
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(intent)
        } else {
            startService(intent)
        }
    }

    private fun ensureBlePermissions(): Boolean {
        val permissions = mutableListOf<String>()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_SCAN) != PackageManager.PERMISSION_GRANTED) {
                permissions.add(Manifest.permission.BLUETOOTH_SCAN)
            }
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
                permissions.add(Manifest.permission.BLUETOOTH_CONNECT)
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                    permissions.add(Manifest.permission.POST_NOTIFICATIONS)
                }
            }
        } else {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                permissions.add(Manifest.permission.ACCESS_FINE_LOCATION)
            }
        }

        if (permissions.isNotEmpty()) {
            ActivityCompat.requestPermissions(this, permissions.toTypedArray(), BT_PERMISSION_REQ)
            return false
        }

        return true
    }

    private fun subscribeToCharacteristic(serviceUuid: String, charUuid: String) {
        val intent = Intent(this, BluetoothLeService::class.java).apply {
            action = BluetoothLeService.ACTION_SUBSCRIBE_CHAR
            putExtra(BluetoothLeService.EXTRA_SERVICE_UUID, serviceUuid)
            putExtra(BluetoothLeService.EXTRA_CHAR_UUID, charUuid)
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(intent)
        } else {
            startService(intent)
        }
    }

    private fun readCharacteristic(serviceUuid: String, charUuid: String) {
        val intent = Intent(this, BluetoothLeService::class.java).apply {
            action = BluetoothLeService.ACTION_READ_CHAR
            putExtra(BluetoothLeService.EXTRA_SERVICE_UUID, serviceUuid)
            putExtra(BluetoothLeService.EXTRA_CHAR_UUID, charUuid)
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(intent)
        } else {
            startService(intent)
        }
    }



    private val bleReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            when (intent?.action) {
                BluetoothAdapter.ACTION_STATE_CHANGED -> {
                    val state = intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, BluetoothAdapter.ERROR)
                    if (state == BluetoothAdapter.STATE_ON) {
                        if (ensureBlePermissions() && !isScanning) {
                            startBleScan()
                        }
                    } else if (state == BluetoothAdapter.STATE_OFF) {
                        binding.tvBleStatus.text = "Status: Bluetooth OFF"
                        binding.tvBleCharStatus.text = "Status: Idle"
                    }
                }
                BluetoothLeService.ACTION_DEVICE_FOUND -> {
                    val name = intent.getStringExtra(BluetoothLeService.EXTRA_DEVICE_NAME)?.trim()
                    val address = intent.getStringExtra(BluetoothLeService.EXTRA_DEVICE_ADDRESS) ?: return
                    if (name.isNullOrBlank() || name.equals("Unknown", ignoreCase = true)) return
                    if (!deviceAddresses.contains(address)) {
                        deviceAddresses.add(address)
                        deviceLabels.add("$name\n$address")
                        deviceAdapter.notifyDataSetChanged()
                        Log.d(TAG, "BLE device found: name=$name, address=$address")
                    }
                }
                BluetoothLeService.ACTION_NOTIFY_LIST -> {
                    val list = intent.getStringArrayListExtra(BluetoothLeService.EXTRA_NOTIFY_LIST)
                    serviceGroups.clear()
                    if (!list.isNullOrEmpty()) {
                        val map = LinkedHashMap<String, MutableList<CharItem>>()
                        for (entry in list) {
                            val parts = entry.split("|")
                            val service = parts.getOrNull(0) ?: continue
                            val ch = parts.getOrNull(1) ?: continue
                            val props = parts.getOrNull(2)?.toIntOrNull() ?: 0
                            val listForService = map.getOrPut(service) { mutableListOf() }
                            listForService.add(CharItem(service, ch, props))
                        }
                        for ((serviceUuid, chars) in map) {
                            serviceGroups.add(ServiceGroup(serviceUuid, chars))
                        }
                        characteristicAdapter.notifyDataSetChanged()
                        binding.listCharacteristics.visibility = View.VISIBLE
                        binding.tvBleCharStatus.text = "Status: Select one"
                        val totalChars = serviceGroups.sumOf { it.chars.size }
                        Log.d(TAG, "Characteristics: $totalChars in ${serviceGroups.size} services")
                        Toast.makeText(
                            this@ConsumerDetailsActivity,
                            "Found $totalChars characteristics",
                            Toast.LENGTH_SHORT
                        ).show()
                    } else {
                        Log.d(TAG, "Characteristics: none")
                        characteristicAdapter.notifyDataSetChanged()
                        binding.listCharacteristics.visibility = View.GONE
                        binding.tvBleCharStatus.text = "Status: None"
                        Toast.makeText(
                            this@ConsumerDetailsActivity,
                            "No characteristics found",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }
                BluetoothLeService.ACTION_STATUS -> {
                    val status = intent.getStringExtra(BluetoothLeService.EXTRA_STATUS) ?: ""
                    val isCharStatus = status.startsWith("Subscribed") ||
                        status.startsWith("Subscribing") ||
                        status.startsWith("Characteristic") ||
                        status.startsWith("Service not found") ||
                        status.startsWith("Select characteristic") ||
                        status.startsWith("No characteristics") ||
                        status.startsWith("Read") ||
                        status.startsWith("Notify") ||
                        status.startsWith("Notify enabled") ||
                        status.startsWith("Notify enable failed") ||
                        status.startsWith("Notify CCCD") ||
                        status.startsWith("Read") ||
                        status.startsWith("Write") ||
                        status.startsWith("Notify")
                    if (isCharStatus) {
                        binding.tvBleCharStatus.text = "Status: $status"
                        return
                    }
                    if (status.startsWith("Connected")) {
                        Log.d(TAG, "BLE connected: $status")
                    }
                    binding.tvBleStatus.text = "Status: $status"
                    if (status.startsWith("Connected")) {
                        binding.listDevices.visibility = View.GONE
                        binding.btnBluetooth.text = "Scan BLE Devices"
                        isScanning = false
                    }
                }
                BluetoothLeService.ACTION_DATA -> {
                    val type = intent.getStringExtra(BluetoothLeService.EXTRA_DATA_TYPE)
                    val text = intent.getStringExtra(BluetoothLeService.EXTRA_DATA_TEXT)
                    val bytes = intent.getByteArrayExtra(BluetoothLeService.EXTRA_DATA_BYTES)
                    Log.d(
                        TAG,
                        "BLE data received: type=$type, textPreview=${text?.take(80)}, bytes=${bytes?.size ?: 0}"
                    )
                    if (!text.isNullOrBlank()) {
                        val fullText = text.take(2000)
                        Log.d(TAG, "BLE data text (up to 2000 chars): $fullText")
                    }
                    if (bytes != null && bytes.isNotEmpty()) {
                        val hex = bytes.take(256).joinToString("") { "%02X".format(it) }
                        Log.d(TAG, "BLE data bytes (first 256 bytes hex): $hex")
                    }
                    binding.tvBleStatus.text = "Status: Receiving data..."
                    val hexPreview = bytes?.joinToString(" ") { "%02X".format(it) } ?: "-"
                    val textDisplay = text ?: bytes?.toString(Charsets.UTF_8) ?: "-"
                    val display = "Text:\n$textDisplay\n\nHex:\n$hexPreview"
                    when (type) {
                        BluetoothLeService.DATA_TYPE_JSON -> {
                            binding.tvBleData.text = display
                        }
                        BluetoothLeService.DATA_TYPE_TEXT -> {
                            binding.tvBleData.text = display
                        }
                        else -> {
                            binding.tvBleData.text = display
                        }
                    }
                }
            }
        }
    }

    @SuppressLint("UnspecifiedRegisterReceiverFlag")
    override fun onStart() {
        super.onStart()
        val filter = IntentFilter().apply {
            addAction(BluetoothAdapter.ACTION_STATE_CHANGED)
            addAction(BluetoothLeService.ACTION_DEVICE_FOUND)
            addAction(BluetoothLeService.ACTION_NOTIFY_LIST)
            addAction(BluetoothLeService.ACTION_STATUS)
            addAction(BluetoothLeService.ACTION_DATA)
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            registerReceiver(bleReceiver, filter, Context.RECEIVER_NOT_EXPORTED)
        } else {
            registerReceiver(bleReceiver, filter)
        }
        refreshPairedDevices()
    }

    override fun onResume() {
        super.onResume()
    }

    override fun onStop() {
        super.onStop()
        try {
            unregisterReceiver(bleReceiver)
        } catch (_: Exception) {}
    }

    private fun setupConnectDeviceButton() {

        binding.btnPhases.setOnClickListener {

            val phaseInput = binding.etphases.text.toString()
                .trim()
                .uppercase(Locale.getDefault())

            if (phaseInput.isEmpty()) {
                binding.etphases.error = "Enter Phase"
                binding.etphases.requestFocus()
                return@setOnClickListener
            }

            val mappedPhase = when (phaseInput) {
                "RYB" -> "A"
                "R" -> "A"
                "RY"  -> "B"
                "B"   -> "C"
                "RB" -> "C"
                "YB"  -> "C"
                "Y"   -> "B"
                else -> {
                    binding.etphases.error = "Invalid Phase"
                    binding.etphases.requestFocus()
                    return@setOnClickListener
                }
            }

            showPhaseDialog(mappedPhase, phaseInput)
        }
    }

    private fun showPhaseDialog(mappedPhase: String, originalPhase: String) {
        val dialogView = layoutInflater.inflate(R.layout.dialog_phase, null)

        val dialogPhasorView = dialogView.findViewById<com.example.feeder.custom.PhasorView>(R.id.dialogPhasorView)
        val tvLabel = dialogView.findViewById<android.widget.TextView>(R.id.tvDialogLabel)
        val btnConfirm = dialogView.findViewById<android.widget.Button>(R.id.tvDialogTitle)
        val btnExit = dialogView.findViewById<android.widget.Button>(R.id.btnExit)
        val btnRetry = dialogView.findViewById<android.widget.Button>(R.id.btnretry)
        btnConfirm.isEnabled = false
        tvLabel.visibility = View.INVISIBLE

        val dialog = androidx.appcompat.app.AlertDialog.Builder(this)
            .setView(dialogView)
            .setCancelable(false)
            .create()

        dialog.show()

        dialogPhasorView.startFiveSecondRotation(mappedPhase)

        dialogPhasorView.setOnRotationCompleteListener {
            tvLabel.text = " $originalPhase"
            tvLabel.visibility = View.VISIBLE
            btnConfirm.isEnabled = true

            tvLabel.setTextColor(
                when (originalPhase.uppercase()) {

                    "RYB" -> Color.parseColor("#D32F2F")
                    "R" ->  Color.parseColor("#D32F2F")// Red
                    "RY"  -> Color.parseColor("#FFEB3B")   // Yellow
                    "Y"   -> Color.parseColor("#FFEB3B")   // Yellow
                    "B"   -> Color.parseColor("#1976D2")   // Blue
                    "RB"  -> Color.parseColor("#1976D2")   // Blue
                    "YB"  -> Color.parseColor("#1976D2")   // Blue

                    else  -> Color.BLACK
                }
            )


            tvLabel.setTypeface(tvLabel.typeface, Typeface.BOLD)
            tvLabel.textSize = 24f
            tvLabel.visibility = View.VISIBLE
        }

        btnConfirm.setOnClickListener {
            dialog.dismiss()
            binding.phaselayout.visibility = View.VISIBLE
            binding.txtOpenCamera.visibility = View.VISIBLE
            binding.updatePhoto.visibility = View.VISIBLE
        }

        btnExit.setOnClickListener {
            dialog.dismiss()
        }

        btnRetry.setOnClickListener {
            btnConfirm.isEnabled = false
            tvLabel.visibility = View.INVISIBLE
            dialogPhasorView.needleAngle = 90f
            dialogPhasorView.startFiveSecondRotation(mappedPhase)
        }
    }


    private fun setupPhaseButton() {
        binding.btnPhase.setOnClickListener {

            val phaseInput = binding.etphases.text.toString()
                .trim()
                .uppercase(Locale.getDefault())

            if (phaseInput.isEmpty()) {
                binding.etphases.error = "Enter Phase"
                binding.etphases.requestFocus()
                return@setOnClickListener
            }
            binding.etphases.setTextColor(when (phaseInput) {
                "RYB" -> Color.parseColor("#D32F2F")
                "R" -> Color.parseColor("#D32F2F")
                "RY"  -> Color.parseColor("#FFEB3B")
                "B"   -> Color.parseColor("#1976D2")
                "RB"   -> Color.parseColor("#1976D2")
                "YB"   -> Color.parseColor("#1976D2")
                "y"   -> Color.parseColor("#FFEB3B")
                else -> Color.BLACK
            })
            val mappedPhase = when (phaseInput) {
                "RYB" -> "A"
                "R" -> "A"
                "RY"  -> "B"
                "B"   -> "C"
                "RB" -> "C"
                "YB"  -> "C"
                "Y"   -> "B"
                else -> {
                    binding.etphases.error = "Invalid Phase"
                    binding.etphases.requestFocus()
                    return@setOnClickListener
                }
            }

            binding.layoutSpeedometer.visibility = View.VISIBLE

            binding.phasorView.startFiveSecondRotation(mappedPhase)

            binding.tvSpeedLabel.text = "Phase: $phaseInput"
        }
    }

    private fun loadConsumerData() {

        binding.swipeRefresh.isRefreshing = true

        showConsumerData()

        val phaseInput = binding.etphases.text.toString()
            .trim()
            .uppercase(Locale.getDefault())

        if (phaseInput.isEmpty()) {
            binding.swipeRefresh.isRefreshing = false
            binding.etphases.error = "Enter Phase"
            binding.etphases.requestFocus()
            return
        }

        binding.etphases.setTextColor(when (phaseInput) {
            "RYB" -> Color.parseColor("#D32F2F")
            "R" -> Color.parseColor("#D32F2F")
            "RY"  -> Color.parseColor("#FFEB3B")
            "B"   -> Color.parseColor("#1976D2")
            "RB"   -> Color.parseColor("#1976D2")
            "YB"   -> Color.parseColor("#1976D2")
            "y"   -> Color.parseColor("#FFEB3B")
            else  -> Color.BLACK
        })

        val mappedPhase = when (phaseInput) {
            "RYB" -> "A"
            "R" -> "A"
            "RY"  -> "B"
            "B"   -> "C"
            "RB" ->  "C"
            "YB"  -> "C"
            "Y"   -> "B"
            else -> {
                binding.swipeRefresh.isRefreshing = false
                binding.etphases.error = "Invalid Phase"
                binding.etphases.requestFocus()
                return
            }
        }

        binding.phasorView.startFiveSecondRotation(mappedPhase)

        fetchLocation()
    }


    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == REQ_ENABLE_BT) {
            if (resultCode == RESULT_OK) {
                startBleScan()
            } else {
                Toast.makeText(this, "Bluetooth required to scan", Toast.LENGTH_SHORT).show()
            }
            return
        }

        if (requestCode == CAMERA_REQ && resultCode == RESULT_OK) {
            // Your existing camera code (keep it if needed)
            val bitmap = data?.extras?.get("data") as? Bitmap
            if (bitmap != null) {
                val finalBitmap = drawTextOnBitmap(bitmap)
                capturedBitmap = finalBitmap
                binding.imgPhoto.visibility = View.VISIBLE
                binding.imgPhoto.setImageBitmap(finalBitmap)
            } else {
                Toast.makeText(this, "Image capture failed", Toast.LENGTH_SHORT).show()
            }
            return
        }

    }
    private fun setupToolbar() {
        setSupportActionBar(binding.toolbar)
        supportActionBar?.apply {
            title = "Consumer Details"
            setDisplayHomeAsUpEnabled(true)
        }
        binding.toolbar.setNavigationOnClickListener { finish() }
    }

    private fun setupUpdateButton() {

        binding.btnupdateConsumer.setOnClickListener {

            val phase = binding.etphases.text.toString().trim().uppercase(Locale.getDefault())

            when (phase) {
                "" -> {
                    binding.etphases.error = "Phase required"
                    binding.etphases.requestFocus()
                    return@setOnClickListener
                }

                "RYB","R", "RY", "B","RB","YB","Y" -> {
                }

                else -> {
                    binding.etphases.error = "Invalid Phase"
                    binding.etphases.requestFocus()
                    return@setOnClickListener
                }
            }

            val body = ConsumerUpdateBody(
                ConsumerNumber = binding.etconsumerno.text.toString(),
                MeterNumber = binding.etMeterNo.text.toString(),
                FeederId = binding.etfeedr.text.toString(),
                Feeder_Name = binding.txtFeedername.text.toString(),
                Substation_Name = binding.txtSubstation.text.toString(),
                PhaseDesignation = phase,   // validated value
                Voltage = binding.etvoltage.text.toString(),
                DTCName = binding.etdtcName.text.toString(),
                DTCCode = "356",
                Latitude = latitude.toString(),
                Longitude = longitude.toString(),
                Location = binding.consumerlocation.text.toString(),
                UserID = "1052",
                SanctionedLoad = binding.etsanctionedload.text.toString(),
                MobileNo = binding.txtMobileno.text.toString(),
                CreatedOn = "2026-01-13"
            )

            val token = "Bearer ${prefManager.getAccessToken()}"
            viewModel.updateConsumer(this, token, body, capturedBitmap)
        }

        viewModel.updateResponse.observe(this) { res ->
            if (res != null) {
                showToast("? Updated Successfully")
                finish()
            } else {
                showToast("Update failed! No response from server")
            }
        }

        viewModel.error.observe(this) { errMsg ->
            if (!errMsg.isNullOrEmpty()) {
                when {
                    errMsg.contains("400") -> showToast("Bad Request (400)")
                    errMsg.contains("401") -> showToast("Unauthorized (401)")
                    errMsg.contains("500") -> showToast("There is technical issue (500)")
                    else -> showToast("Update failed! $errMsg")
                }
            }
        }
    }

    private fun showToast(message: String?) {
        Toast.makeText(this, message ?: "Something went wrong", Toast.LENGTH_SHORT).show()
    }

    private fun showConsumerData() {
        binding.etconsumerno.setText(intent.getStringExtra("consumerNumber") ?: "-")
        binding.etdtcName.setText(intent.getStringExtra("dtcName") ?: "-")
        binding.etMeterNo.setText(intent.getStringExtra("meterNumber") ?: "-")
        binding.txtMobileno.setText(intent.extras?.get("mobileNo")?.toString() ?: "-")
        binding.txtFeedername.setText(intent.getStringExtra("feederName") ?: "-")
        binding.txtSubstation.setText(intent.getStringExtra("substationName") ?: "-")
        binding.txtDivision.setText(intent.getStringExtra("divisionName") ?: "-")
        binding.etcircleName.setText(intent.getStringExtra("circleName") ?: "-")
        binding.etphases.setText(intent.getStringExtra("phase") ?: "-")
        binding.etvoltage.setText(intent.extras?.get("voltage")?.toString() ?: "-")
        binding.etconsumerNumber.setText(intent.getStringExtra("consumerStatus") ?: "-")
        binding.txtCreated.setText(intent.getStringExtra("createdOn") ?: "-")
        binding.etfeedr.setText(intent.extras?.get("feederId")?.toString() ?: "-")
    }

    private fun checkCameraPermission() {
        val cameraGranted =
            ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) ==
                    PackageManager.PERMISSION_GRANTED

        val locationGranted =
            ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) ==
                    PackageManager.PERMISSION_GRANTED

        if (!cameraGranted || !locationGranted) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(
                    Manifest.permission.CAMERA,
                    Manifest.permission.ACCESS_FINE_LOCATION
                ),
                PERMISSION_REQ
            )
        } else {
            openCameraAfterLocation = true
            fetchLocation()
        }
    }

    private fun fetchLocation() {

        binding.consumerlocation.text = "Fetching location..."
        binding.swipeRefresh.isRefreshing = true
        binding.consumerlocation.maxLines = 1
        binding.consumerlocation.ellipsize = TextUtils.TruncateAt.END
        binding.consumerlocation.isSingleLine = true

        if (!fusedLocationClient.hasLocationPermissions()) {
            fusedLocationClient.requestLocationPermissions(this)
            binding.swipeRefresh.isRefreshing = false
            return
        }

        if (!fusedLocationClient.isLocationEnabled()) {
            Toast.makeText(this, "Please enable GPS", Toast.LENGTH_SHORT).show()
            binding.swipeRefresh.isRefreshing = false
            return
        }

        fusedLocationClient.getCurrentLocation { location ->

            binding.swipeRefresh.isRefreshing = false

            if (location == null) {
                binding.consumerlocation.text = "Location unavailable"
                return@getCurrentLocation
            }

            latitude = location.latitude
            longitude = location.longitude

            val address = fusedLocationClient.getAddressLine()

            binding.consumerlocation.text = address ?: "Lat: $latitude, Lng: $longitude"

            if (openCameraAfterLocation) {
                openCameraAfterLocation = false
                openCameraSafely()
            }
        }
    }

    private fun drawTextOnBitmap(original: Bitmap): Bitmap  {
        val mutableBitmap = original.copy(Bitmap.Config.ARGB_8888, true)
        val canvas = Canvas(mutableBitmap)

        val density = resources.displayMetrics.density
        val textSizePx = 4f * density
        val padding = 3f * density

        val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.WHITE
            textSize = textSizePx
            typeface = Typeface.DEFAULT_BOLD
        }

        val bgPaint = Paint().apply {
            color = Color.parseColor("#80000000")
            style = Paint.Style.FILL
        }

        val dateTime = SimpleDateFormat("dd-MM-yyyy  hh:mm a  EEEE", Locale.getDefault()).format(Date())
        val locationText = "Lat: $latitude , Lng: $longitude"

        val yDate = mutableBitmap.height - (textSizePx * 2)
        val yLoc = mutableBitmap.height - textSizePx

        canvas.drawRect(0f, yDate - padding, mutableBitmap.width.toFloat(), mutableBitmap.height.toFloat(), bgPaint)
        canvas.drawText(dateTime, padding, yDate, textPaint)
        canvas.drawText(locationText, padding, yLoc, textPaint)

        return mutableBitmap
    }

    @SuppressLint("MissingPermission")
    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == BT_PERMISSION_REQ) {
            if (grantResults.isNotEmpty() &&
                grantResults.all { it == PackageManager.PERMISSION_GRANTED }
            ) {
                if (bluetoothAdapter?.isEnabled == true) {
                    startBleScan()
                } else {
                    startActivityForResult(Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE), REQ_ENABLE_BT)
                }
            } else {
                Toast.makeText(this, "Bluetooth Permission Required!", Toast.LENGTH_SHORT).show()
            }
        }
    }


    private fun openCameraSafely() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) ==
            PackageManager.PERMISSION_GRANTED
        ) {
            val intent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
            startActivityForResult(intent, CAMERA_REQ)
        }
    }

}
