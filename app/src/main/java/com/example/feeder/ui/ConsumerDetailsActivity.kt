package com.example.feeder.ui

import ConsumerUpdateBody
import ConsumerUpdateRepository
import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.*
import android.location.Geocoder
import android.os.Bundle
import android.provider.MediaStore
import android.view.View
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.example.feeder.data.remote.RetrofitClient
import com.example.feeder.databinding.ActivityConsumerDetailsBinding
import com.example.feeder.ui.base.ConsumerUpdateViewModelFactory
import com.example.feeder.ui.viewModel.ConsumerUpdateViewModel
import com.example.feeder.utils.FusedLocationTracker
import com.example.feeder.utils.PrefManager
import com.google.android.gms.location.LocationServices
import com.google.gson.Gson
import java.text.SimpleDateFormat
import java.util.*

class ConsumerDetailsActivity : AppCompatActivity() {

    private lateinit var binding: ActivityConsumerDetailsBinding
    private lateinit var prefManager: PrefManager

    private val CAMERA_REQ = 1001
    private val PERMISSION_REQ = 2001

    private var latitude = 0.0
    private var longitude = 0.0
    private var capturedBitmap: Bitmap? = null
    private var openCameraAfterLocation = false
    private lateinit var fusedLocationClient: FusedLocationTracker

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

        setupPhaseButton()
        setupToolbar()
        setupUpdateButton()

        loadConsumerData()

        binding.swipeRefresh.setOnRefreshListener {
            loadConsumerData()
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

            binding.layoutSpeedometer.visibility = View.VISIBLE

            binding.phasorView.animate()
                .rotationBy(360f)
                .setDuration(1000)
                .start()

            when (phaseInput) {

                "RYB" -> binding.phasorView.setPhases(listOf("C", ))

                "RY" -> binding.phasorView.setPhases(listOf("B"))

                "B" -> binding.phasorView.setPhases(listOf("A"))

                else -> {
                    binding.etphases.error = "Invalid Phase "
                    binding.etphases.requestFocus()
                    return@setOnClickListener
                }
            }

            binding.tvSpeedLabel.text = "Phase: $phaseInput"
        }
    }

    private fun loadConsumerData() {
        binding.swipeRefresh.isRefreshing = true

        showConsumerData()

        val phase = binding.etphases.text.toString()
            .trim()
            .uppercase(Locale.getDefault())

        when (phase) {
            "RYB" -> binding.phasorView.setPhases(listOf("A","B","C"))
            "RY" -> binding.phasorView.setPhases(listOf("A","B"))
            "B" -> binding.phasorView.setPhases(listOf("C"))
        }

//        binding.phasorView.setPhaseAngle(angle)


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

                "RYB", "RY", "B" -> {
                }

                else -> {
                    binding.etphases.error = "Invalid Phase"
                    binding.etphases.requestFocus()
                    return@setOnClickListener
                }
            }

            val body = ConsumerUpdateBody(
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
                showToast("✅ Updated Successfully")
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
//        binding.etsanctionedload.setText(intent.extras?.get("sanctionedLoad")?.toString() ?: "-")
//        binding.etregionName.setText(intent.getStringExtra("regionName") ?: "-")
//        binding.textzone.setText(intent.getStringExtra("zoneName") ?: "-")
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

            binding.consumerlocation.text =
                address ?: "Lat: $latitude, Lng: $longitude"

            if (openCameraAfterLocation) {
                openCameraAfterLocation = false
                openCameraSafely()
            }
        }
    }


    private fun drawTextOnBitmap(original: Bitmap): Bitmap {
        val mutableBitmap = original.copy(Bitmap.Config.ARGB_8888, true)
        val canvas = Canvas(mutableBitmap)

        val density = resources.displayMetrics.density
        val textSizePx = 12f * density
        val padding = 12f * density

        val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.WHITE
            textSize = textSizePx
            typeface = Typeface.DEFAULT_BOLD
        }

        val bgPaint = Paint().apply {
            color = Color.parseColor("#80000000")
            style = Paint.Style.FILL
        }

        val dateTime = SimpleDateFormat("dd-MM-yyyy  hh:mm a  EEEE", Locale.getDefault())
            .format(Date())
        val locationText = "Lat: $latitude , Lng: $longitude"

        val yDate = mutableBitmap.height - (textSizePx * 2)
        val yLoc = mutableBitmap.height - textSizePx

        canvas.drawRect(0f, yDate - padding, mutableBitmap.width.toFloat(), mutableBitmap.height.toFloat(), bgPaint)
        canvas.drawText(dateTime, padding, yDate, textPaint)
        canvas.drawText(locationText, padding, yLoc, textPaint)

        return mutableBitmap
    }

    override fun onRequestPermissionsResult(
        requestCode: Int, permissions: Array<out String>, grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == PERMISSION_REQ && grantResults.isNotEmpty() &&
            grantResults.all { it == PackageManager.PERMISSION_GRANTED }
        ) {
//            openCameraAfterLocation = true
            fetchLocation()
        } else {
            Toast.makeText(this, "Location Permission denied", Toast.LENGTH_SHORT).show()
        }
    }

    private fun setupSpeedometer(phase: String) {

        val upperPhase = phase.uppercase(Locale.getDefault())

        val speed = when (upperPhase) {
            "RYB" -> 3f
            "RY" -> 2f
            "B" -> 1f
            else -> 0f
        }

//        binding.speedometer.apply {
//            maxSpeed = 3f
//            withTremble = false
//            unit = ""
//            speedTextColor = Color.BLACK
//            unitTextColor = Color.BLACK
//            speedTextSize = 22f
//            speedTo(speed)
//        }

        binding.tvSpeedLabel.text = "Phase: $upperPhase"
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
