package com.example.feeder.ui

import ConsumerUpdateBody
import ConsumerUpdateRepository
import android.Manifest
import android.annotation.SuppressLint
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
import android.view.View
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
import com.google.android.material.snackbar.Snackbar
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
    private val CAMERA_REQ = 1001
    private val PERMISSION_REQ = 2001
    private var latitude = 0.0
    private var longitude = 0.0
    private var capturedBitmap: Bitmap? = null
    private var openCameraAfterLocation = false
    private lateinit var fusedLocationClient: FusedLocationTracker

    private val serviceGroups = mutableListOf<ServiceGroup>()
    private var bleServicesDialog: androidx.appcompat.app.AlertDialog? = null
    private var bleDataDialog: androidx.appcompat.app.AlertDialog? = null
    private var bleDataLabel: android.widget.TextView? = null
    private var bleServicesStatusLabel: android.widget.TextView? = null
    private var blePhaseStatusLabel: android.widget.TextView? = null
    private var blePhasePingLabel: android.widget.TextView? = null
    private var blePhaseDtuLabel: android.widget.TextView? = null
    private var blePhaseDeviceIdLabel: android.widget.TextView? = null
    private var blePhasePhasor: com.example.feeder.custom.PhasorView? = null
    private var blePhaseConfirmBtn: android.widget.Button? = null
    private var autoReadChar: CharItem? = null
    private var bleReadRequested = false
    private var lastBleText: String? = null
    private var lastParsedBle: ParsedBleData? = null
    private var isBleConnected = false
    private val refreshHandler = Handler(Looper.getMainLooper())
    private val refreshIntervalMs = 20_000L
    private val refreshRunnable: Runnable = object : Runnable {
        override fun run() {
            if (isBleConnected) {
                startBleService(BluetoothLeService.ACTION_REFRESH_SERVICES)
                refreshHandler.postDelayed(this, refreshIntervalMs)
            }
        }
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
        setupConnectDeviceButton()

        setupPhaseButton()
        setupToolbar()
        setupUpdateButton()
        loadConsumerData()
        setupBleUi()
        binding.btnConsumerUpdate.visibility = View.GONE


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
        serviceGroups.clear()
        autoReadChar = null
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

    private fun requestBleStatus() {
        startBleService(BluetoothLeService.ACTION_REQUEST_STATUS)
        startBleService(BluetoothLeService.ACTION_REQUEST_NOTIFY_LIST)
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
                BluetoothLeService.ACTION_NOTIFY_LIST -> {
                    val list = intent.getStringArrayListExtra(BluetoothLeService.EXTRA_NOTIFY_LIST)
                    serviceGroups.clear()
                    if (!list.isNullOrEmpty()) {
                        val map = LinkedHashMap<String, MutableList<CharItem>>()
                        val hasTargetChar = list.any { entry ->
                            val parts = entry.split("|")
                            val ch = parts.getOrNull(1) ?: ""
                            ch.startsWith("0000abf1", ignoreCase = true)
                        }
                        for (entry in list) {
                            val parts = entry.split("|")
                            val service = parts.getOrNull(0) ?: continue
                            val ch = parts.getOrNull(1) ?: continue
                            val props = parts.getOrNull(2)?.toIntOrNull() ?: 0
                            if (hasTargetChar && !ch.startsWith("0000abf1", ignoreCase = true)) continue
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
                            blePhaseStatusLabel?.text = "No readable characteristics found"
                        } else {
                            autoReadChar = serviceGroups.flatMap { it.chars }.firstOrNull()
                            if (!bleReadRequested) {
                            } else if (autoReadChar == null) {
                                blePhaseStatusLabel?.text = "No readable characteristics found"
                            } else {
                                showBleDataDialog()
                                blePhaseStatusLabel?.text = "Status: Reading..."
                                readCharacteristic(autoReadChar!!.serviceUuid, autoReadChar!!.charUuid)
                            }
                        }
                    } else {
                        blePhaseStatusLabel?.text = "No characteristics found"
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
                        blePhaseStatusLabel?.text = "Status: Connected"
                        bleServicesStatusLabel?.text = "Status: Connected"
                        if (!isBleConnected) {
                            isBleConnected = true
                            refreshHandler.removeCallbacks(refreshRunnable)
                            refreshHandler.postDelayed(refreshRunnable, refreshIntervalMs)
                        }
                    }
                    if (status.startsWith("Disconnected") || status.startsWith("Device disconnected")) {
                        bleReadRequested = false
                        bleDataDialog?.dismiss()
                        bleServicesDialog?.dismiss()
                        blePhaseStatusLabel?.text = "Status: Disconnected"
                        bleServicesStatusLabel?.text = "Status: Disconnected"
                        isBleConnected = false
                        refreshHandler.removeCallbacks(refreshRunnable)
                    }
                }
                BluetoothLeService.ACTION_DATA -> {
                    val type = intent.getStringExtra(BluetoothLeService.EXTRA_DATA_TYPE)
                    val text = intent.getStringExtra(BluetoothLeService.EXTRA_DATA_TEXT)
                    val bytes = intent.getByteArrayExtra(BluetoothLeService.EXTRA_DATA_BYTES)
                    val textDisplay = text ?: bytes?.toString(Charsets.UTF_8) ?: "-"
                    val parsed = updateDialogFromBleText(textDisplay)
                    if (!parsed.valid) {
                        blePhaseStatusLabel?.text = "Invalid data received from the field unit"
                        return
                    }
                    if (!bleReadRequested) {
                        return
                    }
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
            addAction(BluetoothLeService.ACTION_NOTIFY_LIST)
            addAction(BluetoothLeService.ACTION_STATUS)
            addAction(BluetoothLeService.ACTION_DATA)
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            registerReceiver(bleReceiver, filter, Context.RECEIVER_NOT_EXPORTED)
        } else {
            registerReceiver(bleReceiver, filter)
        }
        requestBleStatus()
    }

    override fun onResume() {
        super.onResume()
    }

    override fun onStop() {
        super.onStop()
        refreshHandler.removeCallbacks(refreshRunnable)
        try {
            unregisterReceiver(bleReceiver)
        } catch (_: Exception) {}
    }

    @SuppressLint("MissingPermission")
    private fun setupConnectDeviceButton() {

        binding.btnPhases.setOnClickListener {
            if (!ensureBlePermissions()) return@setOnClickListener
            if (!isBleConnected) {
                Snackbar.make(
                    binding.root,
                    "Device is not connected. Connect device and try again",
                    Snackbar.LENGTH_SHORT
                ).show()
                return@setOnClickListener
            }
            bleReadRequested = true
            showBleDataDialog()
            bleDataLabel?.text = "Waiting for data..."
            bleDataLabel?.visibility = View.VISIBLE
            blePhaseStatusLabel?.text = "Status: Reading..."
            blePhaseConfirmBtn?.isEnabled = false
            requestBleStatus()
        }
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
        val tvDeviceId = dialogView.findViewById<android.widget.TextView>(R.id.tvDialogDeviceId)
        val btnConfirm = dialogView.findViewById<android.widget.Button>(R.id.btnConfirm)
        val btnExit = dialogView.findViewById<android.widget.Button>(R.id.btnExit)
        val btnRetry = dialogView.findViewById<android.widget.Button>(R.id.btnretry)

        btnConfirm.visibility = View.VISIBLE
        btnConfirm.isEnabled = false
        tvLabel.textSize = 14f
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
                phasor.needleAngle = 0f
                readCharacteristic(target.serviceUuid, target.charUuid)
            }
        }
        btnConfirm.setOnClickListener {
            bleReadRequested = false
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
            val deviceIdValue = parsed?.deviceId
            if (!deviceIdValue.isNullOrBlank()) {
                binding.etDeviceId.setText(deviceIdValue)
                binding.deviceIdLayout.visibility = View.VISIBLE
            }
            binding.phaselayout.visibility = View.VISIBLE
            binding.dTransformerLayout.visibility = View.VISIBLE
            binding.txtOpenCamera.visibility = View.VISIBLE
            binding.updatePhoto.visibility = View.VISIBLE
            binding.btnConsumerUpdate.visibility = View.VISIBLE
        }
        btnExit.setOnClickListener {
            bleReadRequested = false
            bleDataDialog?.dismiss()
        }

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
        blePhaseDeviceIdLabel = tvDeviceId
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
                "RYB" -> "R"
                "R" -> "R"
                "RY"  -> "Y"
                "B"   -> "B"
                "RB" -> "B"
                "YB"  -> "B"
                "Y"   -> "Y"
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
            "RYB" -> "R"
            "R" -> "R"
            "RY"  -> "Y"
            "B"   -> "B"
            "RB" ->  "B"
            "YB"  -> "B"
            "Y"   -> "Y"
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

        binding.btnConsumerUpdate.setOnClickListener {

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
                PhaseDesignation = phase,
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
            "RYB", "R" -> "R"
            "RY", "Y" -> "Y"
            "B", "RB", "YB" -> "B"
            else -> return
        }

        val phasor = blePhasePhasor ?: return
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
        val deviceId: String?,
        val ping: String?,
        val valid: Boolean
    )

    private fun updateDialogFromBleText(raw: String): ParsedBleData {
        lastBleText = raw
        val text = raw.trim()
        if (text.isBlank()) {
            lastParsedBle = ParsedBleData(null, null, null, null, false)
            return lastParsedBle!!
        }

        val phase = when {
            text.contains("RYBPH", ignoreCase = true) -> "RYB"
            text.contains("RPH", ignoreCase = true) -> "R"
            text.contains("YPH", ignoreCase = true) -> "Y"
            text.contains("BPH", ignoreCase = true) -> "B"
            else -> null
        }

        val pingToken = Regex("AVG\\s*=\\s*([^\\s]+)", RegexOption.IGNORE_CASE).find(text)
            ?.groupValues
            ?.getOrNull(1)
        val pingNumber = pingToken?.toIntOrNull()
        val pingValid = pingToken == null || (pingNumber != null && pingNumber in 0..4095)
        if (!pingValid) {
            lastParsedBle = ParsedBleData(null, null, null, null, false)
            return lastParsedBle!!
        }
        val ping = pingNumber?.toString()

        val dtuMatch = Regex("DTUID\\s*[A-Z0-9]+", RegexOption.IGNORE_CASE).find(text)
        val dtu = dtuMatch?.value?.replace(" ", "")
        val deviceId = Regex("\\bFU[A-Z0-9]+\\b", RegexOption.IGNORE_CASE)
            .find(text)
            ?.value
            ?.uppercase(Locale.getDefault())

        if (phase != null || ping != null || dtu != null || deviceId != null) {
            bleDataLabel?.text = if (phase != null) "Phase : $phase" else "RAW"
            if (dtu != null) {
                blePhaseDtuLabel?.text = "DTU: $dtu"
                blePhaseDtuLabel?.visibility = View.VISIBLE
            } else {
                blePhaseDtuLabel?.visibility = View.GONE
            }
            if (deviceId != null) {
                blePhaseDeviceIdLabel?.text = "Device ID: $deviceId"
                blePhaseDeviceIdLabel?.visibility = View.VISIBLE
            } else {
                blePhaseDeviceIdLabel?.visibility = View.GONE
            }
            if (ping != null) {
                blePhasePingLabel?.text = "RSSI: $ping"
                blePhasePingLabel?.visibility = View.VISIBLE
            } else {
                blePhasePingLabel?.visibility = View.GONE
            }
        } else {
            bleDataLabel?.text = text
            blePhaseDtuLabel?.visibility = View.GONE
            blePhaseDeviceIdLabel?.visibility = View.GONE
            blePhasePingLabel?.visibility = View.GONE
        }
        lastParsedBle = ParsedBleData(phase, dtu, deviceId, ping, true)
        return lastParsedBle!!
    }

    private fun saveBleTextToFile(text: String) {
        if (text.isBlank()) return
        try {
            val date = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
            val time = SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date())
            val line = "$time | $text\n"
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                val fileName = "BLE_Data_$date.txt"
                val relPath = Environment.DIRECTORY_DOCUMENTS + "/ConsumerDetails"
                val relPathWithSlash = "$relPath/"
                val resolver = contentResolver
                val collection = MediaStore.Files.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)
                val prefs = getSharedPreferences("ble_logs", Context.MODE_PRIVATE)
                val cachedDate = prefs.getString("date", null)
                val cachedUri = prefs.getString("uri", null)

                val uriFromPrefs = if (cachedDate == date && !cachedUri.isNullOrBlank()) {
                    android.net.Uri.parse(cachedUri)
                } else {
                    null
                }

                val resolvedUri = uriFromPrefs ?: run {
                    val selection = "${MediaStore.MediaColumns.DISPLAY_NAME}=? AND ${MediaStore.MediaColumns.RELATIVE_PATH}=?"
                    val selectionArgs = arrayOf(fileName, relPathWithSlash)
                    resolver.query(
                        collection,
                        arrayOf(MediaStore.MediaColumns._ID),
                        selection,
                        selectionArgs,
                        "${MediaStore.MediaColumns.DATE_ADDED} DESC"
                    )?.use { cursor ->
                        if (cursor.moveToFirst()) {
                            val id = cursor.getLong(0)
                            android.content.ContentUris.withAppendedId(collection, id)
                        } else {
                            null
                        }
                    }
                }

                val uri = resolvedUri ?: run {
                    val values = android.content.ContentValues().apply {
                        put(MediaStore.MediaColumns.DISPLAY_NAME, fileName)
                        put(MediaStore.MediaColumns.MIME_TYPE, "text/plain")
                        put(MediaStore.MediaColumns.RELATIVE_PATH, relPath)
                    }
                    resolver.insert(collection, values)
                } ?: return

                resolver.openOutputStream(uri, "wa")?.use { out ->
                    out.write(line.toByteArray(Charset.forName("UTF-8")))
                    out.flush()
                } ?: return

                prefs.edit()
                    .putString("date", date)
                    .putString("uri", uri.toString())
                    .apply()
            } else {
                val dir = File(
                    Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS),
                    "ConsumerDetails"
                )
                if (!dir.exists()) dir.mkdirs()
                val file = File(dir, "BLE_Data_$date.txt")
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
                bleReadRequested = true
                showBleDataDialog()
                bleDataLabel?.text = "Waiting for data..."
                bleDataLabel?.visibility = View.VISIBLE
                blePhaseStatusLabel?.text = "Status: Reading..."
                blePhaseConfirmBtn?.isEnabled = false
                requestBleStatus()
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
