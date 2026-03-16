package com.example.feeder.ui.view.fragment

import PendingConsumerViewModelFactory
import android.app.AlertDialog
import android.Manifest
import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.graphics.Color
import android.graphics.Typeface
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.text.Spannable
import android.text.SpannableString
import android.text.Spanned
import android.text.style.ForegroundColorSpan
import android.text.style.RelativeSizeSpan
import android.text.style.StyleSpan
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.feeder.R
import com.example.feeder.data.model.CountResponse
import com.example.feeder.data.model.TotalConsumerResponse
import com.example.feeder.data.remote.RetrofitClient
import com.example.feeder.data.repository.CountRepository
import com.example.feeder.data.repository.PendingConsumerRepository
import com.example.feeder.data.repository.RequestStatus
import com.example.feeder.data.repository.TotalConsumerRepository
import com.example.feeder.databinding.DailogConsumerDetailsBinding
import com.example.feeder.databinding.DailogcountconsumerBinding
import com.example.feeder.databinding.FragmentDashBoardBinding
import com.example.feeder.service.BluetoothLeService
import com.example.feeder.ui.ConsumerDetailsActivity
import com.example.feeder.ui.DropdownActivity
import com.example.feeder.ui.adapter.ConsumerCountAdapter
import com.example.feeder.ui.adapter.PendingConsumerAdapter
import com.example.feeder.ui.adapter.TotalConsumerAdapter
import com.example.feeder.ui.base.CountViewModelFactory
import com.example.feeder.ui.base.TotalConsumerViewModelFactory
import com.example.feeder.ui.viewModel.CountConsumerViewModel
import com.example.feeder.ui.viewModel.PendingConsumerViewModel
import com.example.feeder.ui.viewModel.TotalConsumerViewModel
import com.example.feeder.utils.PrefManager
import kotlinx.coroutines.launch

class DashBoardFragment : Fragment() {

    private var _binding: FragmentDashBoardBinding? = null
    private val binding get() = _binding!!
    private val btPermissionReq = 1010
    private val reqEnableBt = 1011
    private var bluetoothAdapter: BluetoothAdapter? = null
    private val deviceAddresses = mutableListOf<String>()
    private val deviceLabels = mutableListOf<String>()
    private lateinit var deviceAdapter: ArrayAdapter<String>
    private var isScanning = false
    private val scanHandler = Handler(Looper.getMainLooper())
    private val scanTimeoutMs = 60_000L
    private val stopScanRunnable = Runnable {
        if (isScanning) stopBleScan()
    }
    private var bleScanDialog: AlertDialog? = null
    private var bleScanStatusLabel: android.widget.TextView? = null
    private var showBleDialogAfterEnable = false
    private var hasBleConnectedOnce = false

    private lateinit var prefManager: PrefManager

    private val viewModel: CountConsumerViewModel by viewModels {
        CountViewModelFactory(CountRepository(RetrofitClient.getServices())
        )
    }

    private val totalConsumerViewModel: TotalConsumerViewModel by viewModels {
        TotalConsumerViewModelFactory(
            TotalConsumerRepository(RetrofitClient.getServices())
        )
    }

    private val pendingViewModel: PendingConsumerViewModel by viewModels {
        PendingConsumerViewModelFactory(
            PendingConsumerRepository(RetrofitClient.getServices())
        )
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentDashBoardBinding.inflate(inflater, container, false)
        prefManager = PrefManager(requireContext())

        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setHasOptionsMenu(true)
        initUI()
        setupClickListeners()
        loadCounts()
        loadTotalConsumers()
        loadPendingCount()
        setupToolbarTitle()
        setupRecyclerView()
        observeData()
        showAllCardBorders()
        showShimmer()
        setupBleUi()

        val token = prefManager.getAccessToken() ?: return
        binding.rvConsumerList.visibility = View.VISIBLE
        totalConsumerViewModel.getTotalConsumer("Bearer $token")

        highlightCard("UPDATE")

        binding.swipeLayout.setOnRefreshListener {
            loadCounts()
            loadTotalConsumers()
            loadPendingCount()
        }

        binding.consumerUpdate.setOnClickListener {
            val token = prefManager.getAccessToken() ?: return@setOnClickListener
            binding.rvConsumerList.visibility = View.VISIBLE
            totalConsumerViewModel.getTotalConsumer("Bearer $token")

            highlightCard("UPDATE")
        }

        binding.totalCounts.setOnClickListener {
            val token = prefManager.getAccessToken() ?: return@setOnClickListener
            binding.rvConsumerList.visibility = View.VISIBLE
            viewModel.getConsumerCount("Bearer $token")
            highlightCard("COUNT")
        }

        binding.totalPending.setOnClickListener {
            val token = prefManager.getAccessToken() ?: return@setOnClickListener
            binding.rvConsumerList.visibility = View.VISIBLE
            pendingViewModel.getPendingConsumers("Bearer $token")
            highlightCard("PENDING")
        }


    }
    private fun showShimmer() {
        binding.shimmerViewContainer.visibility = View.VISIBLE
        binding.shimmerViewContainer.startShimmer()
        binding.dashBoardLayout.visibility = View.GONE
        binding.rvConsumerList.visibility = View.GONE
    }

    private fun hideShimmer() {
        binding.shimmerViewContainer.stopShimmer()
        binding.shimmerViewContainer.visibility = View.GONE
        binding.dashBoardLayout.visibility = View.VISIBLE
    }

    private fun setupRecyclerView() {
        binding.rvConsumerList.layoutManager =
            LinearLayoutManager(requireContext())
    }

    @SuppressLint("MissingPermission")
    private fun setupBleUi() {
        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter()
        deviceAdapter = ArrayAdapter(requireContext(), R.layout.item_ble_device, R.id.tvBleDevice, deviceLabels)
        refreshPairedDevices()
    }

    private fun startBleService(action: String, address: String? = null) {
        val intent = Intent(requireContext(), BluetoothLeService::class.java).apply {
            this.action = action
            if (address != null) {
                putExtra(BluetoothLeService.EXTRA_DEVICE_ADDRESS, address)
            }
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            requireContext().startForegroundService(intent)
        } else {
            requireContext().startService(intent)
        }
    }

    private fun requestBleStatus() {
        startBleService(BluetoothLeService.ACTION_REQUEST_STATUS)
    }

    private fun updateBleToolbarStatus(status: String) {
        val displayStatus = when {
            status.startsWith("Connected") -> {
                hasBleConnectedOnce = true
                status
            }
            status.startsWith("Disconnected") || status.startsWith("Device disconnected") -> {
                if (hasBleConnectedOnce) "Disconnected" else "Scan"
            }
            else -> status
        }
        val subtitle = SpannableString(displayStatus).apply {
            setSpan(RelativeSizeSpan(0.75f), 0, length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
            setSpan(StyleSpan(Typeface.BOLD), 0, length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
        }
        binding.toolbar.subtitle = subtitle
        binding.toolbar.setSubtitleTextColor(ContextCompat.getColor(requireContext(), R.color.white))
    }

    private fun startBleScan() {
        deviceAddresses.clear()
        deviceLabels.clear()
        deviceAdapter.notifyDataSetChanged()
        refreshPairedDevices()
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
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S &&
            ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED
        ) {
            return
        }
        val bonded = bluetoothAdapter?.bondedDevices ?: emptySet()
        for (device in bonded) {
            val address = device.address
            val name = device.name?.trim()
            if (name.isNullOrBlank() || name.equals("Unknown", ignoreCase = true)) continue
            if (!deviceAddresses.contains(address)) {
                deviceAddresses.add(address)
                deviceLabels.add("Paired: $name\n$address")
            }
        }
        deviceAdapter.notifyDataSetChanged()
    }

    private fun connectToBleDevice(address: String) {
        bleScanStatusLabel?.text = "Status: Connecting..."
        startBleService(BluetoothLeService.ACTION_CONNECT, address)
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

        val dialog = AlertDialog.Builder(requireContext())
            .setView(dialogView)
            .setCancelable(true)
            .create()
        dialog.show()
        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)
        bleScanDialog = dialog
        bleScanStatusLabel = status
    }

    private fun ensureBlePermissions(): Boolean {
        val permissions = mutableListOf<String>()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.BLUETOOTH_SCAN) != PackageManager.PERMISSION_GRANTED) {
                permissions.add(Manifest.permission.BLUETOOTH_SCAN)
            }
            if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
                permissions.add(Manifest.permission.BLUETOOTH_CONNECT)
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
                ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
            ) {
                permissions.add(Manifest.permission.POST_NOTIFICATIONS)
            }
        } else if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            permissions.add(Manifest.permission.ACCESS_FINE_LOCATION)
        }

        if (permissions.isNotEmpty()) {
            requestPermissions(permissions.toTypedArray(), btPermissionReq)
            return false
        }
        return true
    }

    private val bleReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            when (intent?.action) {
                BluetoothAdapter.ACTION_STATE_CHANGED -> {
                    val state = intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, BluetoothAdapter.ERROR)
                    if (state == BluetoothAdapter.STATE_ON && ensureBlePermissions() && !isScanning) {
                        startBleScan()
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
                    }
                }
                BluetoothLeService.ACTION_STATUS -> {
                    val status = intent.getStringExtra(BluetoothLeService.EXTRA_STATUS) ?: return
                    bleScanStatusLabel?.text = "Status: $status"
                    updateBleToolbarStatus(status)
                    if (status.startsWith("Connected")) {
                        isScanning = false
                        scanHandler.removeCallbacks(stopScanRunnable)
                        bleScanDialog?.dismiss()
                    }
                }
            }
        }
    }

    private fun observeData() {

        viewLifecycleOwner.lifecycleScope.launch {
            totalConsumerViewModel.consumerResult.collect { status ->

                when (status) {
                    is RequestStatus.Waiting -> {
                        showShimmer()
                    }

                    is RequestStatus.Success -> {
                        hideShimmer()

                        val list = status.data.resData.data
                        binding.rvConsumerList.visibility = View.VISIBLE

                        binding.rvConsumerList.adapter =
                            TotalConsumerAdapter(list) { selectedItem ->
                                showConsumerDialog(selectedItem)
                            }

                        highlightCard("UPDATE")
                    }

                    is RequestStatus.Error -> {
                        hideShimmer()
                        Toast.makeText(
                            requireContext(),
                            "Something went wrong",
                            Toast.LENGTH_SHORT
                        ).show()
                    }

                    else -> {}
                }
            }
        }

        viewLifecycleOwner.lifecycleScope.launch {

            viewModel.countResult.collect { status ->

                when (status) {
                    is RequestStatus.Waiting -> {
                        showShimmer()
                    }

                    is RequestStatus.Success -> {
                        hideShimmer()
                        val resData = status.data?.resData

                        if (resData != null) {
                            binding.rvConsumerList.visibility = View.VISIBLE

                            binding.rvConsumerList.adapter =
                                ConsumerCountAdapter(resData.data) { selectedItem ->
                                    showConsumerDialogFromCount(selectedItem)
                                }

                            highlightCard("COUNT")
                        }
                    }

                    is RequestStatus.Error -> {
                        hideShimmer()
                        binding.rvConsumerList.visibility = View.GONE
                    }
                }


            }
        }

        viewLifecycleOwner.lifecycleScope.launch {

            pendingViewModel.pendingResult.collect { status ->

                when (status) {

                    is RequestStatus.Waiting -> {
                        showShimmer()

                        binding.pendingCount.text = "..."
                        binding.rvConsumerList.visibility = View.GONE
                    }

                    is RequestStatus.Success -> {
                        hideShimmer()

                        val resData = status.data.resData
                        binding.rvConsumerList.visibility = View.VISIBLE
                        binding.pendingCount.text = resData.totalCount.toString()

                        binding.rvConsumerList.visibility = View.VISIBLE

                        binding.rvConsumerList.adapter =
                            PendingConsumerAdapter { selectedItem ->

                                val intent = Intent(
                                    requireContext(),
                                    ConsumerDetailsActivity::class.java
                                ).apply {

                                    putExtra("consumerNumber", selectedItem.consumerNumber ?: "")
                                    putExtra("meterNumber", selectedItem.meterNumber ?: "")
                                    putExtra("phase", selectedItem.phaseDesignation ?: "")
                                    putExtra("voltage", selectedItem.voltage ?: "")
                                    putExtra("dtcName", selectedItem.dtC_Name ?: "")
                                    putExtra("dtcCode", selectedItem.dtC_Code ?: "")
                                    putExtra("sanctionedLoad", selectedItem.sanctionedLoad ?: "")
                                    putExtra("regionName", selectedItem.region_Name ?: "")
                                    putExtra("zoneName", selectedItem.zone_Name ?: "")
                                    putExtra("circleName", selectedItem.circle_Name ?: "")
                                    putExtra("createdOn", selectedItem.createdOn ?: "")
                                    putExtra("substationName", selectedItem.substation_Name ?: "")
                                    putExtra("feederName", selectedItem.feeder_Name ?: "")
                                    putExtra("mobileNo", selectedItem.mobileNo ?: "")
                                    putExtra("feederId", selectedItem.feederID ?: "")
                                    putExtra("divisionName", selectedItem.division_Name ?: "")
                                    putExtra("consumerStatus", selectedItem.consumerStatus ?: "")
                                }

                                startActivity(intent)
                            }.apply {
                                submitList(resData.data)
                            }

                        highlightCard("PENDING")
                    }

                    is RequestStatus.Error -> {
                        hideShimmer()
                        binding.pendingCount.text = "0"
                        binding.rvConsumerList.visibility = View.GONE
                        Toast.makeText(
                            requireContext(),
                            "Pending Load Failed",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }
            }
        }
    }

    private fun highlightCard(type: String) {

        val green = ContextCompat.getColor(requireContext(), R.color.greens)
        val white = ContextCompat.getColor(requireContext(), R.color.white)

        showAllCardBorders()

        binding.consumerUpdate.setCardBackgroundColor(white)
        binding.totalCounts.setCardBackgroundColor(white)
        binding.totalPending.setCardBackgroundColor(white)

        binding.complete.setBackgroundColor(white)
        binding.totalcom.setBackgroundColor(white)
        binding.pending2.setBackgroundColor(white)

        when (type) {

            "UPDATE" -> {
                binding.consumerUpdate.strokeWidth = 6
                binding.consumerUpdate.setCardBackgroundColor(green)
                binding.complete.setBackgroundColor(green)
            }

            "COUNT" -> {
                binding.totalCounts.strokeWidth = 6
                binding.totalCounts.setCardBackgroundColor(green)
                binding.totalcom.setBackgroundColor(green)
            }

            "PENDING" -> {
                binding.totalPending.strokeWidth = 6
                binding.totalPending.setCardBackgroundColor(green)
                binding.pending2.setBackgroundColor(green)
            }
        }
    }

    private fun showAllCardBorders() {

        val red = ContextCompat.getColor(requireContext(), R.color.card_bg)

        binding.consumerUpdate.strokeColor = red
        binding.consumerUpdate.strokeWidth = 4

        binding.totalCounts.strokeColor = red
        binding.totalCounts.strokeWidth = 4

        binding.totalPending.strokeColor = red
        binding.totalPending.strokeWidth = 4
    }

    private fun showConsumerDialogFromCount(data: CountResponse.ResData.Data) {

        val dialogBinding = DailogConsumerDetailsBinding.inflate(layoutInflater)

        val dialog = AlertDialog.Builder(requireContext())
            .setView(dialogBinding.root)
            .setCancelable(true)
            .create()

        dialogBinding.etconsumerno.setText(data.consumerNumber ?: "")
        dialogBinding.etMeterNo.setText(data.meterNumber ?: "")
        dialogBinding.etvoltage.setText(data.voltage?.toString() ?: "")
        dialogBinding.txtFeedername.setText(data.feeder_Name ?: "")
        dialogBinding.etdtcCode.setText(data.dtcCode ?: "")
        dialogBinding.etdtcName.setText(data.dtcName ?: "")
        dialogBinding.etsanctionedload.setText(data.sanctionedLoad?.toString() ?: "")
        dialogBinding.txtMobileno.setText(data.mobileNo?.toString() ?: "")
        dialogBinding.etfeedr.setText(data.feederId ?: "")
        dialogBinding.etphase.setText(data.phaseDesignation?.toString() ?: "")
        dialogBinding.location.setText(data.location ?: "")
        dialogBinding.etlatitude.setText(data.latitude ?: "")
        dialogBinding.txtLongitude.setText(data.longitude ?: "")
        dialogBinding.image.setText(data.imageUrl ?: "")
        dialogBinding.userid.setText(data.userID?.toString() ?: "")
        dialogBinding.txtCreated.setText(data.createdOn ?: "")
        dialogBinding.txtSubstation.setText(data.substation_Name)

        dialogBinding.toolbar.setNavigationOnClickListener {
            dialog.dismiss()
        }

        dialog.show()
    }

    private fun showConsumerDialog(item: TotalConsumerResponse.ResData.Data) {

        val dialogBinding =
            DailogcountconsumerBinding.inflate(LayoutInflater.from(requireContext()))

        val dialog = android.app.AlertDialog.Builder(requireContext())
            .setView(dialogBinding.root)
            .setCancelable(true)
            .create()

        dialogBinding.toolbar.setNavigationOnClickListener {
            dialog.dismiss()
        }

        dialogBinding.etconsumerno.text = item.consumerNumber ?: ""
        dialogBinding.etMeterNo.text = item.meterNumber ?: ""
        dialogBinding.etvoltage.setText(item.voltage?.toString() ?: "")
        dialogBinding.etdtcName.setText(item.dtC_Name ?: "")
        dialogBinding.etdtcCode.setText(item.dtC_Code ?: "")
        dialogBinding.etsanctionedload.setText(item.sanctionedLoad?.toString() ?: "")
        dialogBinding.etregionName.setText(item.region_Name ?: "")
        dialogBinding.textzone.text = item.zone_Name ?: ""
        dialogBinding.circleCenter.text = item.circle_Name ?: ""
        dialogBinding.txtCreated.text = item.createdOn ?: ""
        dialogBinding.txtSubstation.text = item.substation_Name ?: ""
        dialogBinding.txtFeedername.text = item.feeder_Name ?: ""
        dialogBinding.txtMobileno.text = item.mobileNo?.toString() ?: ""
        dialogBinding.etdevision.setText(item.division_Name ?: "")
        dialogBinding.consumerstatus.text = item.consumerStatus ?: ""
        dialogBinding.etphase.setText(item.phaseDesignation?.toString() ?: "")
        dialogBinding.etfeedr.setText(item.feederID?.toString() ?: "")

        dialog.show()
    }

    private fun setupToolbarTitle() {
        val titleText = "PhiTech"
        val spannable = SpannableString(titleText)

        spannable.setSpan(
            ForegroundColorSpan(Color.WHITE),
            7,
            titleText.length,
            Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
        )

        binding.toolbar.title = spannable
    }

    private fun loadCounts() {

        val token = prefManager.getAccessToken() ?: return

        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.getConsumerCount("Bearer $token")

            viewModel.countResult.collect { status ->

                binding.swipeLayout.isRefreshing = false

                when (status) {

                    is RequestStatus.Success -> {
                        val data = status.data.resData

                        binding.totalscountss.text = data.totalCount.toString()
                    }

                    is RequestStatus.Error -> {
                        binding.totalscountss.text = "0"
                    }

                    is RequestStatus.Waiting -> {
                        binding.swipeLayout.isRefreshing = true
                    }
                }
            }
        }
    }

    private fun loadTotalConsumers() {

        val token = prefManager.getAccessToken() ?: return

        viewLifecycleOwner.lifecycleScope.launch {

            totalConsumerViewModel.getTotalConsumer("Bearer $token")

            totalConsumerViewModel.consumerResult.collect { status ->

                binding.swipeLayout.isRefreshing = false

                when (status) {

                    is RequestStatus.Success -> {
                        val data = status.data.resData
                        binding.txtupdate.text = data.totalCount.toString()
                    }

                    is RequestStatus.Error -> {
                        binding.txtupdate.text = "0"
                    }

                    is RequestStatus.Waiting -> {
                        binding.swipeLayout.isRefreshing = true
                    }
                }
            }
        }
    }

    private fun loadPendingCount() {

        val token = prefManager.getAccessToken() ?: return

        viewLifecycleOwner.lifecycleScope.launch {

            pendingViewModel.getPendingConsumers("Bearer $token")

            pendingViewModel.pendingResult.collect { status ->

                when (status) {

                    is RequestStatus.Success -> {
                        binding.pendingCount.text =
                            status.data.resData.totalCount.toString()
                    }

                    is RequestStatus.Error -> {
                        binding.pendingCount.text = "0"
                    }

                    is RequestStatus.Waiting -> {
                        binding.pendingCount.text = "..."
                    }
                }
            }
        }
    }
    override fun onResume() {
        super.onResume()
        loadCounts()
        loadPendingCount()
    }

    @SuppressLint("UnspecifiedRegisterReceiverFlag")
    override fun onStart() {
        super.onStart()
        val filter = IntentFilter().apply {
            addAction(BluetoothAdapter.ACTION_STATE_CHANGED)
            addAction(BluetoothLeService.ACTION_DEVICE_FOUND)
            addAction(BluetoothLeService.ACTION_STATUS)
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            requireContext().registerReceiver(bleReceiver, filter, Context.RECEIVER_NOT_EXPORTED)
        } else {
            requireContext().registerReceiver(bleReceiver, filter)
        }
        refreshPairedDevices()
        requestBleStatus()
    }

    override fun onStop() {
        super.onStop()
        scanHandler.removeCallbacks(stopScanRunnable)
        try {
            requireContext().unregisterReceiver(bleReceiver)
        } catch (_: Exception) {
        }
    }

    private fun initUI() {
        binding.totalscountss.text = "0"
        binding.txtupdate.text = "0"
        binding.pendingCount.text = "0"
        updateBleToolbarStatus("Scan")

        binding.swipeLayout.setColorSchemeResources(
            R.color.blue_dark,
            R.color.black
        )


    }

    private fun setupClickListeners() {
        binding.btnAdd.setOnClickListener {
            startActivity(
                Intent(requireContext(), DropdownActivity::class.java)
            )
        }

        binding.profileImages.setOnClickListener {
            parentFragmentManager.beginTransaction()
                .replace(R.id.fragmentContainer, ProfileFragment())
                .addToBackStack(null)
                .commit()
        }

        binding.ivBluetooth.setOnClickListener {
            bluetoothAdapter = BluetoothAdapter.getDefaultAdapter()
            if (bluetoothAdapter == null) {
                Toast.makeText(requireContext(), "Bluetooth not supported", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            if (!ensureBlePermissions()) return@setOnClickListener
            if (bluetoothAdapter?.isEnabled != true) {
                showBleDialogAfterEnable = true
                startActivityForResult(Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE), reqEnableBt)
                return@setOnClickListener
            }
            showBleScanDialog()
            startBleScan()
        }

    }

    @Deprecated("Deprecated in Java")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == reqEnableBt) {
            if (resultCode == android.app.Activity.RESULT_OK) {
                if (showBleDialogAfterEnable) {
                    showBleScanDialog()
                }
                startBleScan()
                showBleDialogAfterEnable = false
            } else {
                Toast.makeText(requireContext(), "Bluetooth required to scan", Toast.LENGTH_SHORT).show()
                showBleDialogAfterEnable = false
            }
        }
    }

    @Deprecated("Deprecated in Java")
    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == btPermissionReq) {
            if (grantResults.isNotEmpty() && grantResults.all { it == PackageManager.PERMISSION_GRANTED }) {
                if (bluetoothAdapter?.isEnabled == true) {
                    showBleScanDialog()
                    startBleScan()
                } else {
                    showBleDialogAfterEnable = true
                    startActivityForResult(Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE), reqEnableBt)
                }
            } else {
                Toast.makeText(requireContext(), "Bluetooth Permission Required!", Toast.LENGTH_SHORT).show()
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

}
