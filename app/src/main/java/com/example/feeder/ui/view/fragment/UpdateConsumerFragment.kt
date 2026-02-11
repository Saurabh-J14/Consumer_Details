package com.example.feeder.ui.view.fragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.feeder.data.model.TotalConsumerResponse
import com.example.feeder.data.remote.RetrofitClient
import com.example.feeder.data.repository.RequestStatus
import com.example.feeder.data.repository.TotalConsumerRepository
import com.example.feeder.databinding.DailogcountconsumerBinding
import com.example.feeder.databinding.FragmentUpdateConsumerBinding
import com.example.feeder.ui.adapter.TotalConsumerAdapter
import com.example.feeder.ui.base.TotalConsumerViewModelFactory
import com.example.feeder.ui.viewModel.TotalConsumerViewModel
import com.example.feeder.utils.PrefManager
import kotlinx.coroutines.launch

class UpdateConsumerFragment : Fragment() {

    private var _binding: FragmentUpdateConsumerBinding? = null
    private val binding get() = _binding!!

    private lateinit var prefManager: PrefManager

    private val viewModel: TotalConsumerViewModel by viewModels {
        TotalConsumerViewModelFactory(
            TotalConsumerRepository(RetrofitClient.getServices())
        )
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentUpdateConsumerBinding.inflate(inflater, container, false)
        prefManager = PrefManager(requireContext())
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupRecyclerView()
        loadTotalConsumer()
        observeData()
    }

    private fun setupRecyclerView() {
        binding.rvConsumerList.apply {
            layoutManager = LinearLayoutManager(requireContext())
            setHasFixedSize(true)
        }
    }

    private fun loadTotalConsumer() {
        val token = prefManager.getAccessToken()

        if (token.isNullOrEmpty()) {
            Toast.makeText(requireContext(), "Session expired", Toast.LENGTH_SHORT).show()
            return
        }

        viewModel.getTotalConsumer("Bearer $token")
    }

    private fun observeData() {
        viewLifecycleOwner.lifecycleScope.launch {

            viewModel.consumerResult.collect { status ->

                when (status) {

                    is RequestStatus.Waiting -> {
                        binding.totalcount.text = "Loading..."
                    }

                    is RequestStatus.Success -> {
                        val resData = status.data?.resData

                        if (resData != null) {

                            binding.totalcount.text =
                                resData.totalCount.toString()

                            binding.rvConsumerList.adapter =
                                TotalConsumerAdapter(resData.data) { selectedItem ->
                                    showConsumerDialog(selectedItem)
                                }

                        } else {
                            binding.totalcount.text = "0"
                            binding.rvConsumerList.adapter =
                                TotalConsumerAdapter(emptyList()) { }
                        }
                    }

                    is RequestStatus.Error -> {

                        binding.totalcount.text = "0"

                        Toast.makeText(
                            requireContext(),
                            status.message ?: "Something went wrong",
                            Toast.LENGTH_LONG
                        ).show()

                        binding.rvConsumerList.adapter =
                            TotalConsumerAdapter(emptyList()) { }
                    }
                }
            }
        }
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


    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
