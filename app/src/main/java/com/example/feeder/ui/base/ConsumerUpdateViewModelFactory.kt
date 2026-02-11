package com.example.feeder.ui.base

import ConsumerUpdateRepository
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.feeder.ui.viewModel.ConsumerUpdateViewModel

class ConsumerUpdateViewModelFactory (
    private val repository: ConsumerUpdateRepository
) : ViewModelProvider.Factory {

    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return ConsumerUpdateViewModel(repository) as T
    }
}