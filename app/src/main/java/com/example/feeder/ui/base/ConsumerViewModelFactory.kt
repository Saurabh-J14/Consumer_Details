package com.example.feeder.ui.base

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.feeder.data.repository.ConsumerRepository
import com.example.feeder.ui.viewModel.ConsumerActivityViewModel

class ConsumerViewModelFactory(
    private val repository: ConsumerRepository
) : ViewModelProvider.Factory {

    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(ConsumerActivityViewModel::class.java)) {
            return ConsumerActivityViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
