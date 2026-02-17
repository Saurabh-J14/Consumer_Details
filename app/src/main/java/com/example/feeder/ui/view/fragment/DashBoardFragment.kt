package com.example.feeder.ui.view.fragment

import PendingConsumerViewModelFactory
import android.app.AlertDialog
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.text.Spannable
import android.text.SpannableString
import android.text.style.ForegroundColorSpan
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
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
        val token = prefManager.getAccessToken() ?: return
        binding.rvConsumerList.visibility = View.VISIBLE
        totalConsumerViewModel.getTotalConsumer("Bearer $token")

        highlightCard("UPDATE")

        binding.swipeLayout.setOnRefreshListener {
            loadCounts()
            loadTotalConsumers()
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

    private fun setupRecyclerView() {
        binding.rvConsumerList.layoutManager =
            LinearLayoutManager(requireContext())
    }

    private fun observeData() {

        viewLifecycleOwner.lifecycleScope.launch {
            totalConsumerViewModel.consumerResult.collect { status ->

                when (status) {

                    is RequestStatus.Success -> {

                        val list = status.data.resData.data

                        binding.rvConsumerList.adapter =
                            TotalConsumerAdapter(list) { selectedItem ->
                                showConsumerDialog(selectedItem)
                            }

                        highlightCard("UPDATE")
                    }

                    is RequestStatus.Error -> {
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

                    is RequestStatus.Success -> {

                        val resData = status.data?.resData

                        if (resData != null) {

                            binding.rvConsumerList.adapter =
                                ConsumerCountAdapter(resData.data) { selectedItem ->
                                    showConsumerDialogFromCount(selectedItem)
                                }

                            highlightCard("COUNT")
                        }
                    }

                    is RequestStatus.Error -> {
                        binding.rvConsumerList.visibility = View.GONE
                    }

                    else -> {}
                }
            }
        }

        viewLifecycleOwner.lifecycleScope.launch {

            pendingViewModel.pendingResult.collect { status ->

                when (status) {

                    is RequestStatus.Waiting -> {
                        binding.pendingCount.text = "..."
                        binding.rvConsumerList.visibility = View.GONE
                    }

                    is RequestStatus.Success -> {

                        val resData = status.data.resData
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
        val red = ContextCompat.getColor(requireContext(), R.color.red)
        val white = ContextCompat.getColor(requireContext(), R.color.white)

        binding.consumerUpdate.setCardBackgroundColor(white)
        binding.complete.setBackgroundColor(white)

        binding.totalCounts.setCardBackgroundColor(white)
        binding.totalcom.setBackgroundColor(white)

        binding.totalPending.setCardBackgroundColor(white)
        binding.pending2.setBackgroundColor(white)

        when (type) {

            "UPDATE" -> {
                binding.consumerUpdate.setCardBackgroundColor(green)
                binding.complete.setBackgroundColor(green)
            }

            "COUNT" -> {
                binding.totalCounts.setCardBackgroundColor(green)
                binding.totalcom.setBackgroundColor(green)
            }

            "PENDING" -> {
                binding.totalPending.setCardBackgroundColor(green)
                binding.pending2.setBackgroundColor(green)
            }
        }
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

    private fun initUI() {
        binding.totalscountss.text = "0"
        binding.txtupdate.text = "0"
        binding.pendingCount.text = "0"

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

//        binding.totalCounts.setOnClickListener {
//            parentFragmentManager.beginTransaction()
//                .replace(R.id.fragmentContainer, TotalCountConsumerFragment())
//                .addToBackStack(null)
//                .commit()
//        }

//        binding.consumerUpdate.setOnClickListener {
//            parentFragmentManager.beginTransaction()
//                .replace(R.id.fragmentContainer, UpdateConsumerFragment())
//                .addToBackStack(null)
//                .commit()
//        }

//        binding.totalPending.setOnClickListener {
//            parentFragmentManager.beginTransaction()
//                .replace(R.id.fragmentContainer, PendingConsumerFragment())
//                .addToBackStack(null)
//                .commit()
//        }

        binding.profileImages.setOnClickListener {
            parentFragmentManager.beginTransaction()
                .replace(R.id.fragmentContainer, ProfileFragment())
                .addToBackStack(null)
                .commit()
        }

    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

}
