package com.example.feeder.ui.base

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.feeder.data.repository.DtDeleteRepository
import com.example.feeder.ui.viewModel.DtDeleteViewModel

class DtDeleteViewModelFactory(
    private val repository: DtDeleteRepository
) : ViewModelProvider.Factory {

    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(DtDeleteViewModel::class.java)) {
            return DtDeleteViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
