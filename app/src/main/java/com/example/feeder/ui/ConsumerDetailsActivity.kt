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
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.example.feeder.R
import com.example.feeder.data.remote.RetrofitClient
import com.example.feeder.databinding.ActivityConsumerDetailsBinding
import com.example.feeder.service.BluetoothLeService
import com.example.feeder.ui.base.ConsumerUpdateViewModelFactory
import com.example.feeder.ui.viewModel.ConsumerUpdateViewModel
import com.example.feeder.utils.FusedLocationTracker
import com.example.feeder.utils.PrefManager
import java.io.File
import java.io.FileOutputStream
import java.nio.charset.Charset
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
    private var bleScanDialog: androidx.appcompat.app.AlertDialog? = null
    private var bleServicesDialog: androidx.appcompat.app.AlertDialog? = null
    private var bleDataDialog: androidx.appcompat.app.AlertDialog? = null
    private var bleDataLabel: android.widget.TextView? = null
    private var bleScanStatusLabel: android.widget.TextView? = null
    private var bleServicesStatusLabel: android.widget.TextView? = null
    private var blePhaseStatusLabel: android.widget.TextView? = null
    private var blePhasePingLabel: android.widget.TextView? = null
    private var blePhaseDtuLabel: android.widget.TextView? = null
    private var blePhasePhasor: com.example.feeder.custom.PhasorView? = null
    private var blePhaseConfirmBtn: android.widget.Button? = null
    private var autoReadChar: CharItem? = null
    private var lastBleText: String? = null
    private var lastParsedBle: ParsedBleData? = null
    private var showBleDialogAfterEnable = false


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

        refreshPairedDevices()
    }

    private fun startBleScan() {
        deviceAddresses.clear()
        deviceLabels.clear()
        serviceGroups.clear()
        autoReadChar = null
        deviceAdapter.notifyDataSetChanged()
        characteristicAdapter.notifyDataSetChanged()
        isScanning = true
        bleScanStatusLabel?.text = "Status: Scanning..."
        startBleService(BluetoothLeService.ACTION_START_SCAN)
        scanHandler.removeCallbacks(stopScanRunnable)
        scanHandler.postDelayed(stopScanRunnable, scanTimeoutMs)
    }

    private fun stopBleScan() {
        isScanning = false
        bleScanStatusLabel?.text = "Status: Scan stopped"
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
        bleScanStatusLabel?.text = "Status: Connecting..."
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
                            val isRead = (props and android.bluetooth.BluetoothGattCharacteristic.PROPERTY_READ) != 0
                            if (!isRead) continue
                            val listForService = map.getOrPut(service) { mutableListOf() }
                            listForService.add(CharItem(service, ch, props))
                        }
                        for ((serviceUuid, chars) in map) {
                            serviceGroups.add(ServiceGroup(serviceUuid, chars))
                        }
                        val totalChars = serviceGroups.sumOf { it.chars.size }
                        if (totalChars == 0) {
                            Log.d(TAG, "Readable characteristics: none")
                            characteristicAdapter.notifyDataSetChanged()
                            Toast.makeText(
                                this@ConsumerDetailsActivity,
                                "No readable characteristics found",
                                Toast.LENGTH_SHORT
                            ).show()
                        } else {
                            characteristicAdapter.notifyDataSetChanged()
                            Log.d(TAG, "Readable characteristics: $totalChars in ${serviceGroups.size} services")
                            Toast.makeText(
                                this@ConsumerDetailsActivity,
                                "Found $totalChars readable characteristics",
                                Toast.LENGTH_SHORT
                            ).show()
                            showBleServicesDialog()
                        }
                    } else {
                        Log.d(TAG, "Characteristics: none")
                        characteristicAdapter.notifyDataSetChanged()
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
                    if (isCharStatus) return
                    if (status.startsWith("Connected")) {
                        Log.d(TAG, "BLE connected: $status")
                        bleScanDialog?.dismiss()
                        blePhaseStatusLabel?.text = "Status: Connected"
                        bleServicesStatusLabel?.text = "Status: Connected"
                    }
                    if (status.startsWith("Connected")) {
                        isScanning = false
                    }
                    if (status.startsWith("Disconnected") || status.startsWith("Device disconnected")) {
                        bleDataDialog?.dismiss()
                        bleServicesDialog?.dismiss()
                        blePhaseStatusLabel?.text = "Status: Disconnected"
                        bleServicesStatusLabel?.text = "Status: Disconnected"
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
                    val hexPreview = bytes?.joinToString(" ") { "%02X".format(it) } ?: "-"
                    val textDisplay = text ?: bytes?.toString(Charsets.UTF_8) ?: "-"
                    val display = "Text:\n$textDisplay\n\nHex:\n$hexPreview"
                    val parsed = updateDialogFromBleText(textDisplay)
                    bleDataLabel?.visibility = View.VISIBLE
                    blePhaseStatusLabel?.text = "Status: Receiving data"
                    if (parsed.phase != null) {
                        applyPhaseFromBleData(parsed.phase)
                    }
                    saveBleTextToFile(textDisplay)
                    val enableConfirm = parsed.phase != null && parsed.dtu != null
                    blePhaseConfirmBtn?.isEnabled = enableConfirm
                    if (bleServicesDialog?.isShowing == true) {
                        bleServicesDialog?.dismiss()
                    }
                    if (bleDataDialog?.isShowing != true) {
                        showBleDataDialog()
                    }
                    when (type) {
                        BluetoothLeService.DATA_TYPE_JSON -> {}
                        BluetoothLeService.DATA_TYPE_TEXT -> {}
                        else -> {}
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

    @SuppressLint("MissingPermission")
    private fun setupConnectDeviceButton() {

        binding.btnPhases.setOnClickListener {
            bluetoothAdapter = BluetoothAdapter.getDefaultAdapter()
            if (bluetoothAdapter == null) {
                Toast.makeText(this, "Bluetooth not supported", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            if (!ensureBlePermissions()) return@setOnClickListener
            if (bluetoothAdapter?.isEnabled != true) {
                showBleDialogAfterEnable = true
                startActivityForResult(Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE), REQ_ENABLE_BT)
                return@setOnClickListener
            }
            showBleScanDialog()
            startBleScan()
        }
    }

    private fun showBleScanDialog() {
        if (bleScanDialog?.isShowing == true) return
        val dialogView = layoutInflater.inflate(R.layout.dialog_ble_devices, null)
        val list = dialogView.findViewById<android.widget.ListView>(R.id.listBleDialog)
        val status = dialogView.findViewById<android.widget.TextView>(R.id.tvBleDialogStatus)
        list.adapter = deviceAdapter
        list.setOnItemClickListener { _, _, position, _ ->
            val address = deviceAddresses.getOrNull(position) ?: return@setOnItemClickListener
            connectToBleDevice(address)
        }

        status.text = "Status: Scanning..."

        val dialog = androidx.appcompat.app.AlertDialog.Builder(this)
            .setView(dialogView)
            .setCancelable(true)
            .create()
        dialog.show()
        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)
        bleScanDialog = dialog
        bleScanStatusLabel = status
    }

    @SuppressLint("SetTextI18n")
    private fun showBleServicesDialog() {
        if (bleServicesDialog?.isShowing == true) return
        val dialogView = layoutInflater.inflate(R.layout.dialog_ble_services, null)
        val list = dialogView.findViewById<android.widget.ExpandableListView>(R.id.listBleServices)
        val status = dialogView.findViewById<android.widget.TextView>(R.id.tvBleServicesStatus)
        list.setAdapter(characteristicAdapter)
        list.setOnChildClickListener { _, _, groupPosition, childPosition, _ ->
            val item = serviceGroups
                .getOrNull(groupPosition)
                ?.chars
                ?.getOrNull(childPosition)
                ?: return@setOnChildClickListener true
            autoReadChar = item
            status.text = "Status: Reading..."
            readCharacteristic(item.serviceUuid, item.charUuid)
            bleServicesDialog?.dismiss()
            showBleDataDialog()
            true
        }

        status.text = "Status: Select characteristic"

        val dialog = androidx.appcompat.app.AlertDialog.Builder(this)
            .setView(dialogView)
            .setCancelable(true)
            .create()
        dialog.show()
        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)
        bleServicesDialog = dialog
        bleServicesStatusLabel = status
    }

    @SuppressLint("SetTextI18n")
    private fun showBleDataDialog() {
        if (bleDataDialog?.isShowing == true) return
        val dialogView = layoutInflater.inflate(R.layout.dialog_phase, null)
        val phasor = dialogView.findViewById<com.example.feeder.custom.PhasorView>(R.id.dialogPhasorView)
        val tvLabel = dialogView.findViewById<android.widget.TextView>(R.id.tvDialogLabel)
        val tvStatus = dialogView.findViewById<android.widget.TextView>(R.id.tvDialogStatus)
        val tvPing = dialogView.findViewById<android.widget.TextView>(R.id.tvDialogPing)
        val tvDtu = dialogView.findViewById<android.widget.TextView>(R.id.tvDialogDtu)
        val btnConfirm = dialogView.findViewById<android.widget.Button>(R.id.btnConfirm)
        val btnExit = dialogView.findViewById<android.widget.Button>(R.id.btnExit)
        val btnRetry = dialogView.findViewById<android.widget.Button>(R.id.btnretry)

        btnConfirm.visibility = View.VISIBLE
        btnConfirm.isEnabled = false
        tvLabel.textSize = 20f
        tvLabel.text = "Waiting for data..."
        tvLabel.visibility = View.INVISIBLE
        tvStatus.text = "Status: Connected"

        btnRetry.setOnClickListener {
            val target = autoReadChar
            if (target == null) {
            } else {
                tvStatus.text = "Status: Reading..."
                btnConfirm.isEnabled = false
                tvLabel.visibility = View.INVISIBLE
                phasor.needleAngle = 90f
                readCharacteristic(target.serviceUuid, target.charUuid)
            }
        }
        btnConfirm.setOnClickListener {
            bleDataDialog?.dismiss()
            val parsed = lastParsedBle
            val phaseValue = parsed?.phase ?: lastBleText
            if (!phaseValue.isNullOrBlank()) {
                binding.etphases.setText(phaseValue)
            }
            val dtuValue = parsed?.dtu
            if (!dtuValue.isNullOrBlank()) {
                binding.etDt.setText(dtuValue)
            }
            binding.phaselayout.visibility = View.VISIBLE
            binding.txtOpenCamera.visibility = View.VISIBLE
            binding.updatePhoto.visibility = View.VISIBLE
        }
        btnExit.setOnClickListener { bleDataDialog?.dismiss() }

        val dialog = androidx.appcompat.app.AlertDialog.Builder(this)
            .setView(dialogView)
            .setCancelable(true)
            .create()
        dialog.show()

        bleDataDialog = dialog
        bleDataLabel = tvLabel
        blePhaseStatusLabel = tvStatus
        blePhasePingLabel = tvPing
        blePhaseDtuLabel = tvDtu
        blePhasePhasor = phasor
        blePhaseConfirmBtn = btnConfirm
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
                if (showBleDialogAfterEnable) {
                    showBleScanDialog()
                }
                startBleScan()
                showBleDialogAfterEnable = false
            } else {
                Toast.makeText(this, "Bluetooth required to scan", Toast.LENGTH_SHORT).show()
                showBleDialogAfterEnable = false
            }
            return
        }

        if (requestCode == CAMERA_REQ && resultCode == RESULT_OK) {
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

    private fun applyPhaseFromBleData(raw: String) {
        val token = raw.trim().uppercase(Locale.getDefault()).split(Regex("\\s+")).firstOrNull() ?: return
        val cleaned = token.replace(Regex("[^A-Z]"), "")
        if (cleaned.isBlank()) return

        val normalized = when (cleaned) {
            "YR" -> "RY"
            "BR" -> "RB"
            "BY" -> "YB"
            else -> cleaned
        }

        val mappedPhase = when (normalized) {
            "RYB", "R" -> "A"
            "RY", "Y" -> "B"
            "B", "RB", "YB" -> "C"
            else -> return
        }

        val phasor = blePhasePhasor ?: return
        val label = bleDataLabel ?: return
        val confirm = blePhaseConfirmBtn ?: return

        confirm.isEnabled = false

        phasor.startFiveSecondRotation(mappedPhase)
        phasor.setOnRotationCompleteListener {
            confirm.isEnabled = true
        }
    }

    private data class ParsedBleData(
        val phase: String?,
        val dtu: String?,
        val ping: String?
    )

    private fun updateDialogFromBleText(raw: String): ParsedBleData {
        lastBleText = raw
        val text = raw.trim()
        if (text.isBlank()) {
            lastParsedBle = ParsedBleData(null, null, null)
            return lastParsedBle!!
        }

        val phase = when {
            text.contains("RYBPH", ignoreCase = true) -> "RYB"
            text.contains("RPH", ignoreCase = true) -> "R"
            text.contains("YPH", ignoreCase = true) -> "Y"
            text.contains("BPH", ignoreCase = true) -> "B"
            else -> null
        }

        val pingMatch = Regex("AVG\\s*=\\s*(\\d+)", RegexOption.IGNORE_CASE).find(text)
        val ping = pingMatch?.groupValues?.getOrNull(1)

        val dtuMatch = Regex("DTUID\\s*([A-Z0-9]+)", RegexOption.IGNORE_CASE).find(text)
        val dtu = dtuMatch?.groupValues?.getOrNull(1)

        if (phase != null || ping != null || dtu != null) {
            bleDataLabel?.text = phase ?: "RAW"
            if (dtu != null) {
                blePhaseDtuLabel?.text = "DT: $dtu"
                blePhaseDtuLabel?.visibility = View.VISIBLE
            } else {
                blePhaseDtuLabel?.visibility = View.GONE
            }
            if (ping != null) {
                blePhasePingLabel?.text = "Ping: $ping"
                blePhasePingLabel?.visibility = View.VISIBLE
            } else {
                blePhasePingLabel?.visibility = View.GONE
            }
        } else {
            bleDataLabel?.text = text
            blePhaseDtuLabel?.visibility = View.GONE
            blePhasePingLabel?.visibility = View.GONE
        }
        lastParsedBle = ParsedBleData(phase, dtu, ping)
        return lastParsedBle!!
    }

    private fun saveBleTextToFile(text: String) {
        if (text.isBlank()) return
        try {
            val date = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
            val time = SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date())
            val line = "$time | $text\n"
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                val resolver = contentResolver
                val values = android.content.ContentValues().apply {
                    put(MediaStore.MediaColumns.DISPLAY_NAME, "BLE_Data$date.txt")
                    put(MediaStore.MediaColumns.MIME_TYPE, "text/plain")
                    put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DOCUMENTS + "/ConsumerDetails")
                }
                val collection = MediaStore.Files.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)
                val uri = resolver.insert(collection, values) ?: return
                resolver.openOutputStream(uri, "wa")?.use { out ->
                    out.write(line.toByteArray(Charset.forName("UTF-8")))
                    out.flush()
                }
            } else {
                val dir = File(
                    Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS),
                    "ConsumerDetails"
                )
                if (!dir.exists()) dir.mkdirs()
                val file = File(dir, "ble_data_$date.txt")
                FileOutputStream(file, true).use { it.write(line.toByteArray(Charset.forName("UTF-8"))) }
            }
        } catch (_: Exception) {
        }
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
