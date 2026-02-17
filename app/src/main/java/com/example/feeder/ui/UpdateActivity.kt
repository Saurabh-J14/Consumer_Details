package com.example.feeder.ui

import android.os.Bundle
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.feeder.data.model.ConsumerUpdateResponse
import com.example.feeder.data.model.InnerData
import com.example.feeder.data.model.UpdateResponse
import com.example.feeder.databinding.ActivityUpdateBinding
import com.google.gson.Gson

class UpdateActivity : AppCompatActivity() {

    private lateinit var binding: ActivityUpdateBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        enableEdgeToEdge()
        binding = ActivityUpdateBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupToolbar()
        showUpdatedData()
    }

    private fun setupToolbar() {
        setSupportActionBar(binding.toolbar)
        supportActionBar?.apply {
            title = "Updated Consumer"
            setDisplayHomeAsUpEnabled(true)
        }
        binding.toolbar.setNavigationOnClickListener { finish() }
    }

    private fun showUpdatedData() {

        val json = intent.getStringExtra("consumer_data") ?: return

        val data = Gson().fromJson(
            json,
            InnerData::class.java
        )


        binding.etconsumerno.text = data.resData.consumerNumber
        binding.etMeterNo.text = data.resData.meterNumber
        binding.txtMobileno.text = data.resData.mobileNo
        binding.txtFeedername.text = data.resData.feederId
        binding.etsanctionedload.setText(data.resData.sanctionedLoad)
        binding.etdtcName.setText(data.resData.dtcName)
        binding.txtdtcCode.setText(data.resData.dtcCode)

    }

    private fun showToast(msg: String) {
        Toast.makeText(this, msg, Toast.LENGTH_SHORT).show()
    }
}
