package com.example.feeder.ui.base

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.feeder.data.repository.AuthRepository
import com.example.feeder.ui.viewModel.LoginActivityViewModel
import android.app.Application

class LoginActivityViewModelFactory(
    private val authRepository: AuthRepository,
    private val application: Application
) : ViewModelProvider.Factory {

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(LoginActivityViewModel::class.java)) {
            return LoginActivityViewModel(authRepository, application) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}