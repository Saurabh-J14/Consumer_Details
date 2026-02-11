package com.example.feeder.ui.view.fragment

import PendingConsumerViewModelFactory
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.appcompat.widget.SearchView
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.feeder.data.model.PendingConsumerResponse
import com.example.feeder.data.remote.RetrofitClient
import com.example.feeder.data.repository.PendingConsumerRepository
import com.example.feeder.data.repository.RequestStatus
import com.example.feeder.databinding.DailogcountconsumerBinding
import com.example.feeder.databinding.FragmentConsumerPandingBinding
import com.example.feeder.ui.ConsumerDetailsActivity
import com.example.feeder.ui.LoginActivity
import com.example.feeder.ui.adapter.PendingConsumerAdapter
import com.example.feeder.ui.viewModel.PendingConsumerViewModel
import com.example.feeder.utils.PrefManager
import kotlinx.coroutines.launch
import androidx.core.widget.doOnTextChanged
import com.google.gson.Gson

class PendingConsumerFragment : Fragment() {

    private var _binding: FragmentConsumerPandingBinding? = null
    private val binding get() = _binding!!

    private lateinit var prefManager: PrefManager
    private lateinit var adapter: PendingConsumerAdapter
    private var totalCount: Int = 0


    private var fullList: List<PendingConsumerResponse.ResData.Data> = emptyList()

    private val viewModel: PendingConsumerViewModel by viewModels {
        PendingConsumerViewModelFactory(
            PendingConsumerRepository(RetrofitClient.getServices())
        )
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentConsumerPandingBinding.inflate(inflater, container, false)
        prefManager = PrefManager(requireContext())
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupRecyclerView()
        setupSearch()
        loadPendingConsumers()
        observeData()
    }

    private fun setupRecyclerView() {
//        adapter = PendingConsumerAdapter { selectedItem ->
//            showConsumerDialog(selectedItem)
//        }
        adapter = PendingConsumerAdapter { selectedItem ->
            openConsumerDetails(selectedItem)
        }


        binding.rvConsumerList.apply {
            layoutManager = LinearLayoutManager(requireContext())
            setHasFixedSize(true)
            adapter = this@PendingConsumerFragment.adapter
        }
    }

    private fun loadPendingConsumers() {
        val token = prefManager.getAccessToken()

        if (token.isNullOrEmpty()) {
            logoutUser()
            return
        }

        viewModel.getPendingConsumers("Bearer $token")
    }

    private fun observeData() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.pendingResult.collect { status ->
                when (status) {

                    is RequestStatus.Waiting -> {
                        binding.txtstime.text = "Loading..."
                    }

                    is RequestStatus.Success -> {
                        val resData = status.data?.resData
                        binding.txtstime.text = resData?.totalCount?.toString() ?: "0"
                        totalCount = resData?.totalCount ?: 0

                        fullList = resData?.data ?: emptyList()
                        adapter.submitList(fullList)
                    }

                    is RequestStatus.Error -> {
                        binding.txtstime.text = "0"
                        Toast.makeText(
                            requireContext(),
                            status.message ?: "Something went wrong",
                            Toast.LENGTH_SHORT
                        ).show()
                        adapter.submitList(emptyList())
                    }
                }
            }
        }
    }

    private fun setupSearch() {

        binding.searchCard.setOnClickListener {
            binding.etSearchConsumer.requestFocus()

            val imm = requireContext().getSystemService(
                android.content.Context.INPUT_METHOD_SERVICE
            ) as android.view.inputmethod.InputMethodManager

            imm.showSoftInput(binding.etSearchConsumer, android.view.inputmethod.InputMethodManager.SHOW_IMPLICIT)
        }

        binding.etSearchConsumer.doOnTextChanged { text, _, _, _ ->
            filterList(text?.toString())
        }
    }
    private fun filterList(query: String?) {
        if (query.isNullOrEmpty()) {
            adapter.submitList(fullList)
            return
        }

        val filteredList = fullList.filter {
            it.consumerNumber.contains(query)
        }

        adapter.submitList(filteredList)
    }


    private fun openConsumerDetails(item: PendingConsumerResponse.ResData.Data) {

        val intent = Intent(requireContext(), ConsumerDetailsActivity::class.java).apply {

            putExtra("consumerNumber", item.consumerNumber)
            putExtra("meterNumber", item.meterNumber)
            putExtra("phase", item.phaseDesignation)
            putExtra("voltage", item.voltage)
            putExtra("dtcName", item.dtC_Name)
            putExtra("dtcCode", item.dtC_Code)
            putExtra("sanctionedLoad", item.sanctionedLoad)
            putExtra("regionName", item.region_Name)
            putExtra("zoneName", item.zone_Name)
            putExtra("circleName", item.circle_Name)
            putExtra("createdOn", item.createdOn)
            putExtra("substationName", item.substation_Name)
            putExtra("feederName", item.feeder_Name)
            putExtra("mobileNo", item.mobileNo)
            putExtra("feederId", item.feederID)
            putExtra("divisionName", item.division_Name)
            putExtra("consumerStatus", item.consumerStatus)
            putExtra("totalCount", totalCount)
            putExtra("consumerList", Gson().toJson(fullList))
        }

        startActivity(intent)
    }

//    private fun showConsumerDialog(item: PendingConsumerResponse.ResData.Data) {
//
//        val dialogBinding =
//            DailogcountconsumerBinding.inflate(LayoutInflater.from(requireContext()))
//
//        val dialog = android.app.AlertDialog.Builder(requireContext())
//            .setView(dialogBinding.root)
//            .setCancelable(true)
//            .create()
//
//        dialogBinding.toolbar.setNavigationOnClickListener {
//            dialog.dismiss()
//        }
//
//        dialogBinding.etconsumerno.text = item.consumerNumber
//        dialogBinding.etMeterNo.text = item.meterNumber
//        dialogBinding.etphase.setText(item.phaseDesignation ?: "")
//        dialogBinding.etvoltage.setText(item.voltage?.toString() ?: "")
//        dialogBinding.etdtcName.setText(item.dtC_Name ?: "")
//        dialogBinding.etdtcCode.setText(item.dtC_Code ?: "")
//        dialogBinding.etsanctionedload.setText(item.sanctionedLoad?.toString() ?: "")
//        dialogBinding.etregionName.setText(item.region_Name ?: "")
//        dialogBinding.textzone.text = item.zone_Name ?: ""
//        dialogBinding.circleCenter.text = item.circle_Name ?: ""
//        dialogBinding.txtCreated.text = item.createdOn ?: ""
//        dialogBinding.txtSubstation.text = item.substation_Name ?: ""
//        dialogBinding.txtFeedername.text = item.feeder_Name ?: ""
//        dialogBinding.txtMobileno.text = item.mobileNo?.toString() ?: ""
//        dialogBinding.etfeedr.setText(item.feederID ?: "")
//        dialogBinding.etdevision.text = item.division_Name ?: ""
//        dialogBinding.consumerstatus.text = item.consumerStatus ?: ""
//
//        dialog.show()
//    }

    private fun logoutUser() {
        prefManager.clear()
        startActivity(Intent(requireContext(), LoginActivity::class.java))
        requireActivity().finish()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
