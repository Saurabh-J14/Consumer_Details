package com.example.feeder.ui.view.fragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.feeder.data.model.CountResponse
import com.example.feeder.data.remote.RetrofitClient
import com.example.feeder.data.repository.CountRepository
import com.example.feeder.data.repository.RequestStatus
import com.example.feeder.databinding.DailogConsumerDetailsBinding
import com.example.feeder.databinding.FragmentTotalCountConsumerBinding
import com.example.feeder.ui.adapter.ConsumerCountAdapter
import com.example.feeder.ui.base.CountViewModelFactory
import com.example.feeder.ui.viewModel.CountConsumerViewModel
import com.example.feeder.utils.PrefManager
import kotlinx.coroutines.launch

class TotalCountConsumerFragment : Fragment() {

    private var _binding: FragmentTotalCountConsumerBinding? = null
    private val binding get() = _binding!!

    private val viewModel: CountConsumerViewModel by viewModels {
        CountViewModelFactory(
            CountRepository(RetrofitClient.getServices())
        )
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentTotalCountConsumerBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.rvConsumerList.layoutManager = LinearLayoutManager(requireContext())

        loadConsumerCount()
        observeData()
    }

    private fun loadConsumerCount() {
        val token = PrefManager(requireContext()).getAccessToken() ?: return
        viewModel.getConsumerCount("Bearer $token")
    }

    private fun observeData() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.countResult.collect { status ->

                when (status) {

                    is RequestStatus.Success -> {
                        val resData = status.data?.resData
                        if (resData != null) {
                            binding.txtstime.text = resData.totalCount.toString()

                            binding.rvConsumerList.adapter =
                                ConsumerCountAdapter(resData.data) { selectedItem ->
                                    showConsumerDialog(selectedItem)
                                }
                        } else {
                            binding.txtstime.text = "0"
                        }
                    }

                    is RequestStatus.Error -> {
                        binding.txtstime.text = "0"
                    }

                    is RequestStatus.Waiting -> {
                        binding.txtstime.text = "Loading..."
                    }
                }
            }
        }
    }

    private fun showConsumerDialog(data: CountResponse.ResData.Data) {

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
        dialogBinding.userid.setText(data.userID ?.toString()?: "")
        dialogBinding.txtCreated.setText(data.createdOn ?: "")
        dialogBinding.txtSubstation.setText(data.substation_Name)

        dialogBinding.toolbar.setNavigationOnClickListener {
            dialog.dismiss()
        }
        dialog.show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
