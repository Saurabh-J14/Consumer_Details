package com.example.feeder.ui

import android.content.Intent
import android.os.Bundle
import android.view.KeyEvent
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.isVisible
import com.example.feeder.data.body.LoginBody
import com.example.feeder.data.remote.RetrofitClient
import com.example.feeder.data.repository.AuthRepository
import com.example.feeder.databinding.ActivityLoginBinding
import com.example.feeder.ui.base.LoginActivityViewModelFactory
import com.example.feeder.ui.viewModel.LoginActivityViewModel
import com.example.feeder.utils.CheckInternetConnection
import com.example.feeder.utils.PrefManager
import com.google.android.material.snackbar.Snackbar

class LoginActivity : AppCompatActivity() {

    private val binding: ActivityLoginBinding by lazy {
        ActivityLoginBinding.inflate(layoutInflater)
    }
    private lateinit var prefManager: PrefManager

    private val viewModel: LoginActivityViewModel by viewModels {
        LoginActivityViewModelFactory(
            AuthRepository(RetrofitClient.getServices()),
            application
        )
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(binding.root)

        prefManager = PrefManager(this)

        if (prefManager.isUserLoggedIn()) {
            startActivity(Intent(this, DashboardActivity::class.java))
            finish()
        }

        setupClickListeners()
        setupObservers()
    }

    private fun setupClickListeners() {
        binding.btnLogin.setOnClickListener { submitForm() }

        binding.edtPassword.setOnKeyListener { _, keyCode, event ->
            if (keyCode == KeyEvent.KEYCODE_ENTER && event.action == KeyEvent.ACTION_UP) {
                submitForm()
                true
            } else false
        }
    }

    private fun setupObservers() {
        viewModel.isLoading.observe(this) { isLoading ->
            binding.progressLayout.isVisible = isLoading
        }

        viewModel.errorMessage.observe(this) { error ->
            binding.progressLayout.isVisible = false
            val message = when (error) {
                "500" -> "$error - Internal Server Error"
                else -> error ?: "Wrong User ID or Password"
            }

            Snackbar.make(binding.root, message, Snackbar.LENGTH_LONG).show()


        }

        viewModel.user.observe(this) { response ->
            binding.progressLayout.isVisible = false

            if (response?.resData == null) {
                Snackbar.make(binding.root, "Login Failed", Snackbar.LENGTH_LONG).show()
                return@observe
            }

            prefManager.setUserLoggedIn(true)
            prefManager.setEmployeeId(response.resData.userId.toString())
            prefManager.setEmployeeName(response.resData.userName)
            prefManager.setEmployeeMobile(response.resData.mobileNo.toString())
            prefManager.setAccessToken(response.resData.token)

            startActivity(Intent(this, DashboardActivity::class.java))
            finish()
        }
    }


    private fun submitForm() {
        if (!validateForm()) return

        if (!CheckInternetConnection.isNetworkAvailable(this)) {
            Snackbar.make(binding.root, "No Internet Connection", Snackbar.LENGTH_LONG).show()
            return
        }

        val userId = binding.edtUsername.text.toString().trim().toIntOrNull()
        val password = binding.edtPassword.text.toString().trim()

        if (userId == null) {
            binding.usernameLayout.error = "Invalid User ID"
            return
        }

        val body = LoginBody(userID = userId, password = password)

        binding.progressLayout.isVisible = true

        viewModel.loginUser("", body)
    }

    private fun validateForm(): Boolean {
        var valid = true

        if (binding.edtUsername.text.isNullOrEmpty()) {
            binding.usernameLayout.error = "User ID required"
            valid = false
        } else binding.usernameLayout.error = null

        if (binding.edtPassword.text.isNullOrEmpty()) {
            binding.passwordLayout.error = "Password required"
            valid = false
        } else binding.passwordLayout.error = null

        return valid
    }

}