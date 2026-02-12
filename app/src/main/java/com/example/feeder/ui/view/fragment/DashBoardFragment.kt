package com.example.feeder.ui.view.fragment

import PendingConsumerViewModelFactory
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.text.Spannable
import android.text.SpannableString
import android.text.style.ForegroundColorSpan
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import com.example.feeder.R
import com.example.feeder.data.remote.RetrofitClient
import com.example.feeder.data.repository.CountRepository
import com.example.feeder.data.repository.PendingConsumerRepository
import com.example.feeder.data.repository.RequestStatus
import com.example.feeder.data.repository.TotalConsumerRepository
import com.example.feeder.databinding.FragmentDashBoardBinding
import com.example.feeder.ui.DropdownActivity
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

        binding.swipeLayout.setOnRefreshListener {
            loadCounts()
            loadTotalConsumers()
        }
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

        binding.totalCounts.setOnClickListener {
            parentFragmentManager.beginTransaction()
                .replace(R.id.fragmentContainer, TotalCountConsumerFragment())
                .addToBackStack(null)
                .commit()
        }

        binding.consumerUpdate.setOnClickListener {
            parentFragmentManager.beginTransaction()
                .replace(R.id.fragmentContainer, UpdateConsumerFragment())
                .addToBackStack(null)
                .commit()
        }

        binding.totalPending.setOnClickListener {
            parentFragmentManager.beginTransaction()
                .replace(R.id.fragmentContainer, PendingConsumerFragment())
                .addToBackStack(null)
                .commit()
        }

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
