package com.example.feeder.ui.base

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.feeder.data.repository.DtnameRepository
import com.example.feeder.ui.viewModel.DtNameActivityViewModel

class DtNameViewModelFactory(
    private val repository: DtnameRepository
) : ViewModelProvider.Factory {

    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(DtNameActivityViewModel::class.java)) {
            return DtNameActivityViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel")
    }
}
