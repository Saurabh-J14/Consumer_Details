package com.example.feeder.ui.base

import SubstationRepository
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.feeder.ui.viewModel.SubstationActivityViewModel

class SubstationViewModelFactory(
    private val repository: SubstationRepository
) : ViewModelProvider.Factory {

    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(SubstationActivityViewModel::class.java)) {
            return SubstationActivityViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
