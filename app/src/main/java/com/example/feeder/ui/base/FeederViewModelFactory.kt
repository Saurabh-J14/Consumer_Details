package com.example.feeder.ui.base

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.feeder.data.repository.FeederRepository
import com.example.feeder.ui.viewModel.FeederViewModel

class FeederViewModelFactory(
    private val repository: FeederRepository
) : ViewModelProvider.Factory {

    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(FeederViewModel::class.java)) {
            return FeederViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel")
    }
}
