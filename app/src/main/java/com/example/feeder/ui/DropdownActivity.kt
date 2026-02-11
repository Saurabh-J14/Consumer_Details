package com.example.feeder.ui
import SubstationRepository
import android.app.AlertDialog
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.text.Spannable
import android.text.SpannableString
import android.text.style.ForegroundColorSpan
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.feeder.R
import com.example.feeder.data.body.AddDtNameBody
import com.example.feeder.data.model.ConsumerResponse
import com.example.feeder.data.remote.RetrofitClient
import com.example.feeder.data.repository.AddDtNameRepository
import com.example.feeder.data.repository.ConsumerRepository
import com.example.feeder.data.repository.DtDeleteRepository
import com.example.feeder.data.repository.DtnameRepository
import com.example.feeder.data.repository.FeederRepository
import com.example.feeder.data.repository.ListRepository
import com.example.feeder.databinding.ActivityDropdownBinding
import com.example.feeder.databinding.DialogAddDtBinding
import com.example.feeder.databinding.DialogUserProfileBinding
import com.example.feeder.ui.adapter.ConsumerListAdapter
import com.example.feeder.ui.base.*
import com.example.feeder.ui.view.fragment.ProfileFragment
import com.example.feeder.ui.viewModel.*
import com.example.feeder.utils.PrefManager

class DropdownActivity : AppCompatActivity() {

    private lateinit var binding: ActivityDropdownBinding
    private lateinit var prefManager: PrefManager
    private var addDtDialog: AlertDialog? = null


    private val substationViewModel: SubstationActivityViewModel by viewModels {
        SubstationViewModelFactory(SubstationRepository(RetrofitClient.getServices()))
    }

    private val feederViewModel: FeederViewModel by viewModels {
        FeederViewModelFactory(FeederRepository(RetrofitClient.getServices()))
    }

    private val dtNameViewModel: DtNameActivityViewModel by viewModels {
        DtNameViewModelFactory(DtnameRepository(RetrofitClient.getServices()))
    }

    private val addDtViewModel: AddDtNameActivityViewModel by viewModels {
        AddDtNameViewModelFactory(AddDtNameRepository(RetrofitClient.getServices()))
    }
    private val listViewModel: ListActivityViewModel by viewModels {
        ListViewModeFactory(ListRepository(RetrofitClient.getServices()))
    }

    private val consumerViewModel: ConsumerActivityViewModel by viewModels {
        ConsumerViewModelFactory(ConsumerRepository(RetrofitClient.getServices()))
    }
    private val dtDeleteViewModel: DtDeleteViewModel by viewModels {
        DtDeleteViewModelFactory(
            DtDeleteRepository(RetrofitClient.getServices())
        )
    }


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        binding = ActivityDropdownBinding.inflate(layoutInflater)
        setContentView(binding.root)
        binding.btnDeleteDt.visibility = View.GONE

        prefManager = PrefManager(this)

        setupToolbar()
        handleInsets()
        setupObservers()
        setupSwipeToRefresh()
        clickListeners()
        callSubstationApi()
    }

    private fun callSubstationApi() {
        val token = prefManager.getAccessToken()
        if (token.isNullOrEmpty()) {
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
            return
        }
        showShimmer(true)
        substationViewModel.fetchSubstations(token)
    }

    private fun setupObservers() {

        substationViewModel.substationList.observe(this) {
            showShimmer(false)
            if (it.isNotEmpty()) setupSubstationDropdown(it)
        }

        feederViewModel.feederList.observe(this) {
            setupFeederDropdown(it)
        }

        dtNameViewModel.dtNameList.observe(this) {
            setupDtNameDropdown(it)
        }

        addDtViewModel.addDtResult.observe(this) { result ->

            result.onSuccess {
                Toast.makeText(this, it.resMessage, Toast.LENGTH_SHORT).show()

                addDtDialog?.dismiss()
                addDtDialog = null

                binding.dtname.setText("")

                dtNameViewModel.fetchDtNames(
                    "Bearer ${prefManager.getAccessToken()}",
                    binding.feederIdSpin.text.toString()
                )
            }

            result.onFailure {
                Toast.makeText(
                    this,
                    it.message ?: "Failed to add DT",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }


        listViewModel.consumerList.observe(this) { list ->

            if (list.isEmpty()) {
                Toast.makeText(this, "No data found", Toast.LENGTH_SHORT).show()
            } else {
                showConsumerListDialog(list)
            }
        }

        consumerViewModel.consumerData.observe(this) { data ->
            openConsumerDetailsActivity(data)
        }


        consumerViewModel.isLoading.observe(this) { loading ->
            if (loading) {

            }
        }

        consumerViewModel.errorMessage.observe(this) { msg ->
            Toast.makeText(this, msg, Toast.LENGTH_SHORT).show()
        }

        dtDeleteViewModel.deleteResult.observe(this) { result ->

            result.onSuccess { msg ->
                Toast.makeText(this, msg, Toast.LENGTH_SHORT).show()

                binding.dtname.setText("")

                binding.btnDeleteDt.visibility = View.GONE

                dtNameViewModel.fetchDtNames(
                    prefManager.getAccessToken() ?: "",
                    binding.feederIdSpin.text.toString()
                )
            }

            result.onFailure {
                Toast.makeText(
                    this,
                    it.message ?: "Delete failed",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }

    }

    private fun setupSubstationDropdown(list: List<String>) {
        val adapter = ArrayAdapter(this, android.R.layout.simple_dropdown_item_1line, list)
        binding.substation.setAdapter(adapter)
        binding.substation.setOnClickListener { binding.substation.showDropDown() }

        binding.substation.setOnItemClickListener { _, _, pos, _ ->
            val token = prefManager.getAccessToken() ?: return@setOnItemClickListener
            feederViewModel.fetchFeederIds(token, list[pos])
        }
    }

    private fun setupFeederDropdown(list: List<String>) {
        val adapter = ArrayAdapter(this, android.R.layout.simple_dropdown_item_1line, list)
        binding.feederIdSpin.setAdapter(adapter)
        binding.feederIdSpin.setOnClickListener { binding.feederIdSpin.showDropDown() }

        binding.feederIdSpin.setOnItemClickListener { _, _, pos, _ ->
            val token = prefManager.getAccessToken() ?: return@setOnItemClickListener
            dtNameViewModel.fetchDtNames(token, list[pos])
        }
    }

    private fun setupDtNameDropdown(list: List<String>) {
        val adapter = ArrayAdapter(this, android.R.layout.simple_dropdown_item_1line, list)
        binding.dtname.setAdapter(adapter)
        binding.dtname.setOnClickListener {
            binding.dtname.showDropDown()
        }
        binding.dtname.setOnItemClickListener { _, _, _, _ ->
            binding.btnDeleteDt.visibility = View.VISIBLE
        }
    }

    private fun clickListeners() {

        binding.btnDtAction.setOnClickListener {

            val substation = binding.substation.text.toString().trim()
            val feederId = binding.feederIdSpin.text.toString().trim()

            if (substation.isEmpty()) {
                Toast.makeText(this, "Please select Substation ", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (feederId.isEmpty()) {
                Toast.makeText(this, "Please select Feeder ID", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            showAddDtDialog()
        }

        binding.btnlist.setOnClickListener {

            val feederId = binding.feederIdSpin.text.toString().trim()

            if (feederId.isEmpty()) {
                Toast.makeText(this, "Please select feeder", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val token = prefManager.getAccessToken()
            if (token.isNullOrEmpty()) {
                Toast.makeText(this, "Session expired, login again", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            listViewModel.fetchConsumerList(token, feederId)
        }

        binding.btnDeleteDt.setOnClickListener {

            val dtName = binding.dtname.text.toString().trim()

            if (dtName.isEmpty()) {
                Toast.makeText(this, "Please select DT Name", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            AlertDialog.Builder(this)
                .setTitle("Delete DT")
                .setMessage("Are you sure you want to delete DT \"$dtName\"?")
                .setPositiveButton("Yes") { _, _ ->

                    val token = prefManager.getAccessToken()
                    if (token.isNullOrEmpty()) {
                        Toast.makeText(this, "Session expired", Toast.LENGTH_SHORT).show()
                        return@setPositiveButton
                    }

                    dtDeleteViewModel.deleteDt(token, dtName)
                }
                .setNegativeButton("No", null)
                .show()
        }
    }

    private fun openConsumerDetailsActivity(data: ConsumerResponse.ResData.Data) {

        val intent = Intent(this, ConsumerDetailsActivity::class.java)

        intent.putExtra("consumerNumber", data.consumerNumber ?: "-")
        intent.putExtra("dtcName", data.dtC_Name ?: "-")
        intent.putExtra("meterNumber", data.meterNumber ?: "-")
        intent.putExtra("mobileNo", data.mobileNo?.toString() ?: "-")   // ✅ FIX
        intent.putExtra("feederName", data.feeder_Name ?: "-")
        intent.putExtra("substationName", data.substation_Name ?: "-")
        intent.putExtra("divisionName", data.division_Name ?: "-")
        intent.putExtra("circleName", data.circle_Name ?: "-")
        intent.putExtra("phase", data.phase ?: "-")
        intent.putExtra("voltage", data.voltage?.toString() ?: "-")     // ✅ FIX
        intent.putExtra("sanctionedLoad", data.sanctionedLoad?.toString() ?: "-") // ✅ FIX
        intent.putExtra("feederId", data.feederId ?: "-")
        intent.putExtra("regionName", data.region_Name ?: "-")
        intent.putExtra("zoneName", data.zone_Name ?: "-")
        intent.putExtra("createdOn", data.createdOn ?: "-")
        intent.putExtra("consumerStatus", data.consumerStatus ?: "-")

        startActivity(intent)
    }

    private fun showConsumerListDialog(list: List<String>) {

        if (list.isEmpty()) {
            Toast.makeText(this, "No consumer found", Toast.LENGTH_SHORT).show()
            return
        }

        val dialogView = layoutInflater.inflate(R.layout.dailog_list, null)

        val recyclerView = dialogView.findViewById<RecyclerView>(R.id.rvlist)

        recyclerView.layoutManager = LinearLayoutManager(this)
        recyclerView.setHasFixedSize(true)

        recyclerView.adapter = ConsumerListAdapter(list) { consumerNo ->

            val token = prefManager.getAccessToken()
            val dtcName = binding.dtname.text.toString()

            if (token.isNullOrEmpty()) {
                Toast.makeText(this, "Session expired", Toast.LENGTH_SHORT).show()
                return@ConsumerListAdapter
            }
            consumerViewModel.fetchConsumer(token, consumerNo, dtcName)
        }

        val dialog = AlertDialog.Builder(this)
            .setView(dialogView)
            .setCancelable(true)
            .create()

        dialog.show()

        dialog.window?.setLayout(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.MATCH_PARENT
        )
    }

  /*  private fun showAddDtDialog() {

        val dialogBinding = DialogAddDtBinding.inflate(layoutInflater)

        val dialog = AlertDialog.Builder(this)
            .setView(dialogBinding.root)
            .setCancelable(false)
            .create()

        addDtDialog = dialog

        dialogBinding.btnDtAction.setOnClickListener {

            val dtName = dialogBinding.dtname.text.toString().trim()

            if (dtName.isEmpty()) {
                Toast.makeText(this, "Enter DT Name", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val body = AddDtNameBody(
                DTC_Name = dtName,
                feederID = binding.feederIdSpin.text.toString(),
                feeder_Name = binding.feederIdSpin.text.toString(),
                substation_Name = binding.substation.text.toString()
            )

            addDtViewModel.addDt(
                prefManager.getAccessToken() ?: "",
                body
            )
        }

        dialog.show()

        addDtDialog = dialog
    }*/

    private fun showAddDtDialog() {

        val dialogBinding = DialogAddDtBinding.inflate(layoutInflater)

        val dialog = AlertDialog.Builder(this)
            .setView(dialogBinding.root)
            .setCancelable(true)
            .create()

        addDtDialog = dialog

        dialogBinding.btnDtAction.setOnClickListener {

            val dtName = dialogBinding.dtname.text.toString().trim()

            if (dtName.isEmpty()) {
                Toast.makeText(this, "Enter DT Name", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val body = AddDtNameBody(
                DTC_Name = dtName,
                feederID = binding.feederIdSpin.text.toString(),
                feeder_Name = binding.feederIdSpin.text.toString(),
                substation_Name = binding.substation.text.toString()
            )

            val token = "Bearer ${prefManager.getAccessToken()}"

            addDtViewModel.addDt(token, body)
        }

        dialog.show()
    }


    private fun setupSwipeToRefresh() {
        binding.swipeRefresh.setOnRefreshListener {
            binding.substation.setText("")
            binding.feederIdSpin.setText("")
            binding.dtname.setText("")
            binding.btnDeleteDt.visibility = View.GONE

            binding.substation.setAdapter(null)
            binding.feederIdSpin.setAdapter(null)
            binding.dtname.setAdapter(null)

            feederViewModel.clearFeederList()
            dtNameViewModel.clearDtNameList()

            callSubstationApi()
            binding.swipeRefresh.isRefreshing = false
        }
    }

    private fun showShimmer(show: Boolean) {
        binding.shimmerViewContainer.visibility = if (show) View.VISIBLE else View.GONE
        if (show) binding.shimmerViewContainer.startShimmer()
        else binding.shimmerViewContainer.stopShimmer()
    }

    private fun setupToolbar() {
        setSupportActionBar(binding.toolbar)

        val titleText = "UtilityNet App"

        val spannable = SpannableString(titleText)

        spannable.setSpan(
            ForegroundColorSpan(Color.RED),
            0,
            11,
            Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
        )

        spannable.setSpan(
            ForegroundColorSpan(Color.WHITE),
            11,
            titleText.length,
            Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
        )

        supportActionBar?.title = spannable
    }

//    private fun showUserProfileDialog() {
//
//        val dialogBinding = DialogUserProfileBinding.inflate(layoutInflater)
//
//        dialogBinding.userid.text = prefManager.getEmployeeName()
//        dialogBinding.name.text = prefManager.getEmployeeId()
//        dialogBinding.mob.text = prefManager.getMobileNumber()
//
//        val dialog = AlertDialog.Builder(this)
//            .setView(dialogBinding.root)
//            .setCancelable(true)
//            .create()
//
//        dialog.show()
//
//        dialogBinding.logout.setOnClickListener {
//
//            prefManager.clear()
//
//            dialog.dismiss()
//
//            val intent = Intent(this, LoginActivity::class.java)
//            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
//            startActivity(intent)
//            finish()
//        }
//    }

    private fun handleInsets() {
        ViewCompat.setOnApplyWindowInsetsListener(binding.main) { _, insets ->
            binding.headerView.setPadding(0, insets.getInsets(WindowInsetsCompat.Type.systemBars()).top, 0, 0)
            insets
        }
    }
}
